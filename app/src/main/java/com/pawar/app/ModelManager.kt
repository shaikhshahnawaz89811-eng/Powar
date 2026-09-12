package com.pawar.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Manages the local GGUF module file.
 * This step intentionally implements only safe import, validation, load and unload.
 * Inference is not claimed here; a llama.cpp/native runtime will be wired in later.
 */
class ModelManager private constructor(private val context: Context) {
    // Keeps load/unload/import/delete atomic so a lifecycle change or another action
    // cannot race an in-progress model state transition.
    private val operationLock = Mutex()
    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }
    private val modelFile = File(modelDir, EXPECTED_FILE_NAME)

    @Volatile
    private var mapped: ByteBuffer? = null
    private var channel: FileChannel? = null
    private var raf: RandomAccessFile? = null

    val exists: Boolean get() = modelFile.isFile
    val file: File get() = modelFile
    val loaded: Boolean get() = mapped != null

    suspend fun import(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        operationLock.withLock {
            runCatching {
            if (loaded) error("Unload the module before replacing it.")

            val resolver = context.contentResolver
            val temp = File(modelDir, "${EXPECTED_FILE_NAME}.part")
            try {
                resolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output ->
                        input.copyTo(output, DEFAULT_BUFFER)
                    }
                } ?: error("Could not open the selected file.")

                if (temp.length() <= 0L) error("The module file is empty.")
                if (temp.length() > MAX_MODEL_BYTES) {
                    error("Module is too large. Maximum accepted size is 2 GiB.")
                }
                validateGguf(temp)

                // Keep the previous good module until the replacement is completely
                // validated. A failed move therefore cannot destroy the old module.
                moveReplacement(temp, modelFile)
            } finally {
                if (temp.exists()) temp.delete()
            }
            }
        }
    }

    suspend fun load(): Result<Unit> = withContext(Dispatchers.IO) {
        operationLock.withLock {
            runCatching {
            if (loaded) return@runCatching
            if (!exists) error("Import the GGUF module first.")

            validateGguf(modelFile)
            val localRaf = RandomAccessFile(modelFile, "r")
            try {
                val localChannel = localRaf.channel
                val size = localChannel.size()
                if (size <= 0L) error("The module file is empty.")
                val mappedBuffer = localChannel.map(FileChannel.MapMode.READ_ONLY, 0L, size)
                raf = localRaf
                channel = localChannel
                mapped = mappedBuffer
            } catch (t: Throwable) {
                runCatching { localRaf.close() }
                throw t
            }
            }
        }
    }

    suspend fun unload(): Result<Unit> = withContext(Dispatchers.IO) {
        operationLock.withLock {
            runCatching {
            // FileChannel.close() does not unmap an existing mapped buffer. The
            // reference is dropped here so the JVM/Android runtime can reclaim it.
            mapped = null
            runCatching { channel?.close() }
            runCatching { raf?.close() }
            channel = null
            raf = null
            }
        }
    }

    suspend fun delete(): Result<Unit> = withContext(Dispatchers.IO) {
        operationLock.withLock {
            runCatching {
            if (loaded) error("Unload the module before deleting it.")
            if (modelFile.exists() && !modelFile.delete()) {
                error("Could not delete the module.")
            }
            }
        }
    }

    suspend fun sha256(): String = withContext(Dispatchers.IO) {
        if (!exists) return@withContext ""
        val digest = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().use { input ->
            val buffer = ByteArray(HASH_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun validateStoredModel(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!exists) error("Module not imported.")
            if (modelFile.length() <= 0L) error("The module file is empty.")
            if (modelFile.length() > MAX_MODEL_BYTES) {
                error("Stored module is too large.")
            }
            validateGguf(modelFile)
        }
    }

    private fun validateGguf(file: File) {
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < GGUF_HEADER_BYTES) {
                error("Invalid GGUF: file is too small.")
            }

            val magic = ByteArray(4)
            input.readFully(magic)
            if (String(magic, Charsets.US_ASCII) != "GGUF") {
                error("Invalid GGUF: missing GGUF header.")
            }

            // GGUF uses little-endian uint32 for its version. Current llama.cpp
            // defines GGUF_VERSION as 3, so reject malformed/unsupported versions
            // before the file reaches the runtime.
            val version = Integer.reverseBytes(input.readInt())
            if (version != GGUF_VERSION) {
                error("Unsupported GGUF version: $version. Expected $GGUF_VERSION.")
            }
        }
    }

    private fun moveReplacement(temp: File, destination: File) {
        val backup = File(destination.parentFile, "${destination.name}.bak")
        if (backup.exists() && !backup.delete()) {
            error("Could not prepare the module replacement.")
        }

        var oldBackedUp = false
        try {
            if (destination.exists()) {
                if (!destination.renameTo(backup)) {
                    error("Could not prepare the existing module for replacement.")
                }
                oldBackedUp = true
            }

            if (!temp.renameTo(destination)) {
                if (oldBackedUp) {
                    backup.renameTo(destination)
                    oldBackedUp = false
                }
                error("Could not store the module safely.")
            }

            if (oldBackedUp && backup.exists()) {
                backup.delete()
            }
        } catch (t: Throwable) {
            if (oldBackedUp && !destination.exists()) {
                backup.renameTo(destination)
            }
            throw t
        } finally {
            if (backup.exists()) backup.delete()
        }
    }

    companion object {
        @Volatile
        private var instance: ModelManager? = null

        fun getInstance(context: Context): ModelManager =
            instance ?: synchronized(this) {
                instance ?: ModelManager(context.applicationContext).also { instance = it }
            }

        const val EXPECTED_FILE_NAME = "qwen2.5-coder-7b-q4_k_m.gguf"
        private const val GGUF_VERSION = 3
        private const val GGUF_HEADER_BYTES = 8L
        private const val DEFAULT_BUFFER = 1024 * 1024
        private const val HASH_BUFFER = 1024 * 1024
        private const val MAX_MODEL_BYTES = 2L * 1024L * 1024L * 1024L
    }
}

enum class ModuleStatus { NOT_IMPORTED, READY, LOADING, LOADED, ERROR }
