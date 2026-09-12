package com.pawar.app

/**
 * Thin JNI surface over pawar_llama_jni.cpp / llama.cpp.
 *
 * This does not exist yet as a working binary in a fresh checkout: the native
 * library is only produced once llama.cpp is vendored under
 * app/src/main/cpp/llama.cpp and the project is built with the NDK. Until
 * then System.loadLibrary below will throw UnsatisfiedLinkError, which
 * LlamaCodeGenerationCapability surfaces as a normal Result.failure — never
 * as a fabricated success.
 */
object LlamaBridge {

    @Volatile
    private var libraryLoaded = false
    private var libraryLoadError: Throwable? = null

    /** Callback invoked from native code on the calling (IO) thread. */
    interface TokenSink {
        fun onToken(text: String)
        fun onDone()
        fun onError(message: String)
    }

    @Synchronized
    fun ensureLibraryLoaded(): Result<Unit> {
        if (libraryLoaded) return Result.success(Unit)
        libraryLoadError?.let { return Result.failure(it) }
        return runCatching {
            System.loadLibrary("pawar_llama")
            libraryLoaded = true
        }.onFailure { libraryLoadError = it }
    }

    external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Long
    external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, callback: TokenSink): Boolean
    external fun nativeCancel(handle: Long)
    external fun nativeFreeModel(handle: Long)
}
