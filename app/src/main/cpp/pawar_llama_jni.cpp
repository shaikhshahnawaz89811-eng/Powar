// Real llama.cpp inference bridge for Pawar.
//
// This file intentionally does NOT fake a result. If llama.cpp fails to load
// the model or generation fails, the error is propagated back to Kotlin via
// TokenSink.onError(...) instead of returning fabricated text.
//
// NOTE: written against the llama.cpp public C API as of the ggml-org/llama.cpp
// "simple-chat" style example (llama_model_load_from_file / llama_init_from_model /
// llama_sampler_chain). If the vendored llama.cpp commit differs, some of these
// symbols may have been renamed upstream — this has not been compiled in this
// environment (no NDK/toolchain available here), so build it locally first and
// fix any signature drift before shipping.

#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <string>
#include <vector>
#include <mutex>

#include "llama.h"

#define LOG_TAG "PawarLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct PawarLlamaSession {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    std::mutex generationMutex;
    std::atomic<bool> cancelRequested{false};
};

std::string jstringToUtf8(JNIEnv* env, jstring value) {
    if (value == nullptr) return "";
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(value, chars);
    return result;
}

void reportError(JNIEnv* env, jobject callback, const std::string& message) {
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onError = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");
    if (onError != nullptr) {
        jstring jmsg = env->NewStringUTF(message.c_str());
        env->CallVoidMethod(callback, onError, jmsg);
        env->DeleteLocalRef(jmsg);
    }
    env->DeleteLocalRef(callbackClass);
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_pawar_app_LlamaBridge_nativeLoadModel(
    JNIEnv* env, jclass /*clazz*/, jstring modelPath, jint nCtx, jint nThreads) {

    const std::string path = jstringToUtf8(env, modelPath);
    LOGI("Loading model from %s (ctx=%d threads=%d)", path.c_str(), nCtx, nThreads);

    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    // CPU-only on-device inference. n_gpu_layers stays 0 unless a GPU backend
    // (e.g. Vulkan) is explicitly built into the vendored llama.cpp.
    modelParams.n_gpu_layers = 0;

    llama_model* model = llama_model_load_from_file(path.c_str(), modelParams);
    if (model == nullptr) {
        LOGE("llama_model_load_from_file failed for %s", path.c_str());
        return 0;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(nCtx > 0 ? nCtx : 2048);
    ctxParams.n_threads = nThreads > 0 ? nThreads : 4;
    ctxParams.n_threads_batch = ctxParams.n_threads;

    llama_context* ctx = llama_init_from_model(model, ctxParams);
    if (ctx == nullptr) {
        LOGE("llama_init_from_model failed");
        llama_model_free(model);
        return 0;
    }

    llama_sampler_chain_params samplerParams = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(samplerParams);
    // A conservative, deterministic-leaning chain: sensible defaults for code
    // generation (lower temperature than free-form chat).
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.2f));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    auto* session = new PawarLlamaSession();
    session->model = model;
    session->ctx = ctx;
    session->sampler = sampler;
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_pawar_app_LlamaBridge_nativeGenerate(
    JNIEnv* env, jclass /*clazz*/, jlong handle, jstring prompt, jint maxTokens, jobject callback) {

    auto* session = reinterpret_cast<PawarLlamaSession*>(handle);
    if (session == nullptr || session->ctx == nullptr || session->model == nullptr) {
        reportError(env, callback, "Model session is not loaded.");
        return JNI_FALSE;
    }

    std::lock_guard<std::mutex> lock(session->generationMutex);
    session->cancelRequested = false;

    const std::string promptText = jstringToUtf8(env, prompt);
    const llama_vocab* vocab = llama_model_get_vocab(session->model);

    // Tokenize the prompt (two-pass: measure, then fill).
    const int negTokenCount = -llama_tokenize(vocab, promptText.c_str(), (int32_t) promptText.size(),
                                               nullptr, 0, true, true);
    std::vector<llama_token> tokens(negTokenCount);
    if (llama_tokenize(vocab, promptText.c_str(), (int32_t) promptText.size(),
                        tokens.data(), (int32_t) tokens.size(), true, true) < 0) {
        reportError(env, callback, "Prompt tokenization failed.");
        return JNI_FALSE;
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(session->ctx, batch) != 0) {
        reportError(env, callback, "Prompt evaluation (prefill) failed.");
        return JNI_FALSE;
    }

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onDone = env->GetMethodID(callbackClass, "onDone", "()V");

    const int limit = maxTokens > 0 ? maxTokens : 512;
    char pieceBuffer[256];

    for (int i = 0; i < limit; i++) {
        if (session->cancelRequested.load()) {
            LOGI("Generation cancelled after %d tokens", i);
            break;
        }

        llama_token nextToken = llama_sampler_sample(session->sampler, session->ctx, -1);
        if (llama_vocab_is_eog(vocab, nextToken)) {
            LOGI("Generation reached end-of-generation token after %d tokens", i);
            break;
        }

        int pieceLength = llama_token_to_piece(vocab, nextToken, pieceBuffer, sizeof(pieceBuffer), 0, true);
        if (pieceLength > 0 && onToken != nullptr) {
            jstring jpiece = env->NewStringUTF(std::string(pieceBuffer, pieceLength).c_str());
            env->CallVoidMethod(callback, onToken, jpiece);
            env->DeleteLocalRef(jpiece);
        }

        llama_batch nextBatch = llama_batch_get_one(&nextToken, 1);
        if (llama_decode(session->ctx, nextBatch) != 0) {
            reportError(env, callback, "Decoding failed mid-generation.");
            env->DeleteLocalRef(callbackClass);
            return JNI_FALSE;
        }
    }

    if (onDone != nullptr) {
        env->CallVoidMethod(callback, onDone);
    }
    env->DeleteLocalRef(callbackClass);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pawar_app_LlamaBridge_nativeCancel(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    auto* session = reinterpret_cast<PawarLlamaSession*>(handle);
    if (session != nullptr) {
        session->cancelRequested = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_pawar_app_LlamaBridge_nativeFreeModel(JNIEnv* /*env*/, jclass /*clazz*/, jlong handle) {
    auto* session = reinterpret_cast<PawarLlamaSession*>(handle);
    if (session == nullptr) return;

    if (session->ctx != nullptr) llama_free(session->ctx);
    if (session->model != nullptr) llama_model_free(session->model);
    if (session->sampler != nullptr) llama_sampler_free(session->sampler);
    delete session;
}
