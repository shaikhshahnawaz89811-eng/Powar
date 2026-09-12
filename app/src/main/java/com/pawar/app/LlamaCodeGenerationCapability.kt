package com.pawar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Real, local code-generation capability backed by llama.cpp running the
 * module the user loaded in Settings. This class never returns a fabricated
 * success: if the module is not loaded, the native library has not been
 * built, or generation fails, it returns Result.failure with a genuine
 * reason instead of made-up code.
 *
 * Known limitation (documented, not hidden): the native llama.cpp session
 * created here is independent of ModelManager's own mmap. If the user taps
 * Unload in Settings, this class only notices and releases its native
 * session on the *next* generate() call, not immediately. Call shutdown()
 * from MainActivity.onDestroy() to avoid leaking the native session when the
 * app closes.
 */
class LlamaCodeGenerationCapability(
    private val manager: ModelManager
) : CodeGenerationCapability {

    override val id: String = "llama-cpp-code-generation"
    override val description: String =
        "Generates code locally via llama.cpp using the module loaded in Settings."

    private val sessionMutex = Mutex()
    private var sessionHandle: Long = 0L
    private var sessionModelPath: String? = null

    override suspend fun generate(request: String): Result<String> = withContext(Dispatchers.IO) {
        if (!manager.loaded) {
            releaseSessionLocked()
            return@withContext Result.failure(
                IllegalStateException("Local module is not loaded. Load it from Settings first.")
            )
        }

        LlamaBridge.ensureLibraryLoaded().fold(
            onSuccess = {},
            onFailure = { error ->
                return@withContext Result.failure(
                    IllegalStateException(
                        "Native inference library is not built yet. " +
                            "Vendor llama.cpp under app/src/main/cpp/llama.cpp and rebuild. Cause: ${error.message}",
                        error
                    )
                )
            }
        )

        val handleResult = sessionMutex.withLock { ensureSessionLocked() }
        val handle = handleResult.getOrElse { return@withContext Result.failure(it) }

        val prompt = buildPrompt(request)
        val output = StringBuilder()
        var failure: String? = null

        val sink = object : LlamaBridge.TokenSink {
            override fun onToken(text: String) {
                output.append(text)
            }
            override fun onDone() { /* handled by nativeGenerate's return value below */ }
            override fun onError(message: String) {
                failure = message
            }
        }

        val completed = runCatching {
            LlamaBridge.nativeGenerate(handle, prompt, MAX_GENERATED_TOKENS, sink)
        }.getOrElse { throwable ->
            return@withContext Result.failure(throwable)
        }

        if (!completed || failure != null) {
            return@withContext Result.failure(IllegalStateException(failure ?: "Generation failed."))
        }
        if (output.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Model produced no output."))
        }

        Result.success(output.toString().trim())
    }

    /** Call from MainActivity.onDestroy() so the native session is not leaked. */
    suspend fun shutdown() {
        sessionMutex.withLock { releaseSessionLocked() }
    }

    private fun ensureSessionLocked(): Result<Long> {
        val currentPath = manager.file.absolutePath
        if (sessionHandle != 0L && sessionModelPath == currentPath) {
            return Result.success(sessionHandle)
        }
        // Path changed (module replaced) or no session yet: drop the stale one first.
        releaseSessionLocked()

        val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(2, 6)
        val handle = LlamaBridge.nativeLoadModel(currentPath, CONTEXT_TOKENS, threads)
        if (handle == 0L) {
            return Result.failure(IllegalStateException("llama.cpp could not load the module file."))
        }
        sessionHandle = handle
        sessionModelPath = currentPath
        return Result.success(handle)
    }

    private fun releaseSessionLocked() {
        if (sessionHandle != 0L) {
            runCatching { LlamaBridge.nativeFreeModel(sessionHandle) }
        }
        sessionHandle = 0L
        sessionModelPath = null
    }

    private fun buildPrompt(request: String): String = buildString {
        append("<|im_start|>system\n")
        append("You are a careful coding assistant. Answer with working code and brief necessary explanation only.\n")
        append("<|im_end|>\n<|im_start|>user\n")
        append(request.trim())
        append("\n<|im_end|>\n<|im_start|>assistant\n")
    }

    private companion object {
        const val CONTEXT_TOKENS = 4096
        const val MAX_GENERATED_TOKENS = 768
    }
}
