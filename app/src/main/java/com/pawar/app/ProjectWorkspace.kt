package com.pawar.app

import android.content.Context
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Sandboxed project workspace. It never executes project contents.
 * All filesystem paths are canonicalized before access.
 */
class ProjectWorkspace private constructor(
    val root: File,
    val sourceName: String
) {
    fun listFiles(): List<String> = root.walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(root).invariantSeparatorsPath }
        .filter { it.isNotBlank() }
        .sorted()
        .toList()

    fun readFile(path: String): String {
        val file = safeFile(path)
        require(file.isFile) { "File does not exist: $path" }
        require(file.length() <= MAX_TEXT_FILE_BYTES) { "File is too large to read safely: $path" }
        return file.readText(Charsets.UTF_8)
    }

    fun search(query: String, maxMatches: Int = 100): List<FileMatch> {
        require(query.isNotBlank()) { "Search query cannot be blank." }
        val matches = mutableListOf<FileMatch>()
        for (path in listFiles()) {
            if (matches.size >= maxMatches) break
            val file = safeFile(path)
            if (file.length() > MAX_TEXT_FILE_BYTES) continue
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: continue
            text.lineSequence().forEachIndexed { index, line ->
                if (matches.size < maxMatches && line.contains(query, ignoreCase = true)) {
                    matches += FileMatch(path, index + 1, line.trim().take(240))
                }
            }
        }
        return matches
    }

    fun writeFile(path: String, content: String) {
        val file = safeFile(path)
        require(content.toByteArray(Charsets.UTF_8).size <= MAX_TEXT_FILE_BYTES) { "File is too large." }
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    fun deleteFile(path: String) {
        val file = safeFile(path)
        require(file.isFile) { "File does not exist: $path" }
        require(file.delete()) { "Could not delete: $path" }
    }

    fun packageZip(destinationDir: File): File {
        destinationDir.mkdirs()
        val base = sourceName.substringBeforeLast('.', sourceName).ifBlank { "PawarProject" }
        val output = File(destinationDir, "${base}_pawar_modified.zip")
        if (output.exists()) output.delete()
        ZipOutputStream(output.outputStream().buffered()).use { zip ->
            for (path in listFiles()) {
                val normalized = normalizeRelativePath(path)
                val file = safeFile(normalized)
                val entry = java.util.zip.ZipEntry(normalized)
                zip.putNextEntry(entry)
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return output
    }

    private fun safeFile(path: String): File {
        val normalized = normalizeRelativePath(path)
        val target = File(root, normalized).canonicalFile
        val rootCanonical = root.canonicalFile
        require(target.path == rootCanonical.path || target.path.startsWith(rootCanonical.path + File.separator)) {
            "Unsafe path: $path"
        }
        return target
    }

    private fun normalizeRelativePath(path: String): String {
        val normalized = path.replace('\\', '/')
        require(normalized.isNotBlank()) { "Path cannot be blank." }
        require(!normalized.startsWith('/') && !normalized.contains('\u0000')) { "Unsafe path: $path" }
        require(normalized.split('/').none { it == ".." }) { "Unsafe path: $path" }
        return normalized.removePrefix("./")
    }

    companion object {
        private const val MAX_TEXT_FILE_BYTES = 2L * 1024L * 1024L
        private const val MAX_ENTRIES = 2_000
        private const val MAX_TOTAL_EXTRACTED_BYTES = 128L * 1024L * 1024L

        suspend fun open(context: Context, attachment: Attachment): Result<ProjectWorkspace> = runCatching {
            require(attachment.kind == AttachmentKind.ZIP) { "Only ZIP attachments can open a project workspace." }
            val root = File.createTempFile("pawar-workspace-", "", context.cacheDir).apply {
                delete()
                mkdirs()
            }
            val archive = File.createTempFile("pawar-source-", ".zip", context.cacheDir)
            try {
                context.contentResolver.openInputStream(attachment.uri)?.use { input ->
                    archive.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
                } ?: error("Could not open the ZIP attachment.")

                ZipFile(archive).use { zip ->
                    val entries = zip.entries().asSequence().toList()
                    require(entries.size <= MAX_ENTRIES) { "Archive has more than $MAX_ENTRIES entries." }
                    val normalizedSeen = HashSet<String>()
                    var extractedBytes = 0L
                    for (entry in entries) {
                        val normalized = entry.name.replace('\\', '/')
                        require(!normalized.startsWith('/') && !normalized.contains('\u0000')) { "Unsafe archive path: ${entry.name}" }
                        require(normalized.split('/').none { it == ".." }) { "Unsafe archive path: ${entry.name}" }
                        if (!entry.isDirectory) {
                            require(normalizedSeen.add(normalized)) { "Duplicate archive path: $normalized" }
                        }
                        if (entry.isDirectory) {
                            val dir = File(root, normalized).canonicalFile
                            require(dir.path == root.canonicalPath || dir.path.startsWith(root.canonicalPath + File.separator))
                            dir.mkdirs()
                            continue
                        }
                        val destination = File(root, normalized).canonicalFile
                        val rootCanonical = root.canonicalFile
                        require(destination.path.startsWith(rootCanonical.path + File.separator)) { "Unsafe archive path: ${entry.name}" }
                        destination.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            destination.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    extractedBytes += read
                                    require(extractedBytes <= MAX_TOTAL_EXTRACTED_BYTES) {
                                        "Archive expands beyond the safe ${MAX_TOTAL_EXTRACTED_BYTES / (1024 * 1024)} MiB workspace limit."
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                }
                ProjectWorkspace(root, attachment.name)
            } catch (t: Throwable) {
                root.deleteRecursively()
                throw t
            } finally {
                archive.delete()
            }
        }

        fun fromDirectory(root: File, sourceName: String): ProjectWorkspace {
            require(root.isDirectory) { "Workspace directory does not exist: $root" }
            return ProjectWorkspace(root.canonicalFile, sourceName)
        }
    }
}

data class FileMatch(val path: String, val line: Int, val text: String)
