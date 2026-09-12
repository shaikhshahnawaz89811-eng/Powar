package com.pawar.app

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * Local capability registry. Every result is real and structured; unavailable
 * capabilities are not represented as successful fake tool calls.
 */
class LocalToolRegistry(context: Context, modelManager: ModelManager) {
    private val appContext = context.applicationContext
    private val tools: List<LocalPipelineTool> = listOf(
        ZipInspectionTool(appContext),
        ImageInspectionTool(appContext)
    )

    val projectWorkspaceCapability: ProjectWorkspaceCapability = LocalProjectWorkspaceCapability(appContext)
    val projectGeneratorCapability: ProjectGeneratorCapability = LocalProjectGeneratorCapability(appContext)
    val codeGenerationCapability: LlamaCodeGenerationCapability = LlamaCodeGenerationCapability(modelManager)

    fun appCacheDir(): File = appContext.cacheDir

    fun capabilities(): List<String> = listOf(
        "attachment-inspection",
        "zip-safe-listing",
        "image-metadata",
        "static-project-diagnostics",
        projectWorkspaceCapability.id,
        projectGeneratorCapability.id,
        codeGenerationCapability.id
    )

    suspend fun generateCode(request: String): Result<String> = codeGenerationCapability.generate(request)

    suspend fun openProjectWorkspace(attachment: Attachment): Result<ProjectWorkspace> =
        projectWorkspaceCapability.open(attachment)

    suspend fun createStarterProject(profile: TaskProfile, projectName: String): Result<ProjectWorkspace> =
        projectGeneratorCapability.create(profile, projectName)

    suspend fun diagnose(workspace: ProjectWorkspace): ToolResult =
        runCatching {
            val report = ProjectDiagnostics.inspect(workspace)
            val errors = report.findings.filter { it.severity == DiagnosticSeverity.ERROR }
            val warnings = report.findings.filter { it.severity == DiagnosticSeverity.WARNING }
            val infos = report.findings.filter { it.severity == DiagnosticSeverity.INFO }
            val details = buildString {
                append("Scanned ${report.filesScanned} text file(s). ")
                append("${errors.size} error(s), ${warnings.size} warning(s), ${infos.size} info item(s).")
                report.findings.take(20).forEach { finding ->
                    append("\n")
                    append(finding.severity.name)
                    if (finding.path != null) append(" ${finding.path}")
                    if (finding.line != null) append(":${finding.line}")
                    append(" — ${finding.message}")
                }
                if (report.findings.size > 20) append("\nAdditional findings omitted from the compact activity view.")
            }
            // The diagnostic operation itself succeeded even when it found source errors.
            // Findings are evidence for the agent; they must not masquerade as a failed tool call.
            ToolResult(
                toolId = "static-project-diagnostics",
                success = true,
                summary = if (report.hasErrors) "Static diagnostics found ${errors.size} error(s)" else "Static diagnostics completed with no syntax-level errors",
                detail = details
            )
        }.getOrElse { error ->
            ToolResult("static-project-diagnostics", false, "Static diagnostics failed", error.message ?: "Could not inspect project files.")
        }

    suspend fun inspect(attachments: List<Attachment>): List<ToolResult> =
        attachments.map { attachment ->
            tools.firstOrNull { it.supports(attachment) }?.inspect(attachment)
                ?: ToolResult(
                    toolId = "attachment-metadata",
                    success = true,
                    summary = "${attachment.name} registered",
                    detail = "No specialized local inspector is registered for this input type."
                )
        }
}

private interface LocalPipelineTool {
    val id: String
    fun supports(attachment: Attachment): Boolean
    suspend fun inspect(attachment: Attachment): ToolResult
}

private class ImageInspectionTool(private val context: Context) : LocalPipelineTool {
    override val id: String = "image-metadata-inspector"

    override fun supports(attachment: Attachment): Boolean = attachment.kind == AttachmentKind.IMAGE

    override suspend fun inspect(attachment: Attachment): ToolResult = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(attachment.uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: error("Could not open the image.")
            ToolResult(
                toolId = id,
                success = bounds.outWidth > 0 && bounds.outHeight > 0,
                summary = "${attachment.name} · ${bounds.outWidth}×${bounds.outHeight}",
                detail = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    "Image bytes were opened safely and dimensions were read. Semantic visual analysis is a separate capability."
                } else {
                    "Image dimensions could not be decoded."
                }
            )
        }.getOrElse { error ->
            ToolResult(id, false, "${attachment.name} could not be inspected", error.message ?: "Image could not be read safely.")
        }
    }
}

private class ZipInspectionTool(private val context: Context) : LocalPipelineTool {
    override val id: String = "zip-safe-inspector"

    override fun supports(attachment: Attachment): Boolean = attachment.kind == AttachmentKind.ZIP

    override suspend fun inspect(attachment: Attachment): ToolResult = withContext(Dispatchers.IO) {
        runCatching {
            val archive = copyBoundedToCache(attachment)
            try {
                inspectArchive(attachment.name, archive)
            } finally {
                archive.delete()
            }
        }.getOrElse { error ->
            ToolResult(id, false, "${attachment.name} could not be inspected", error.message ?: "Archive could not be read safely.")
        }
    }

    private fun copyBoundedToCache(attachment: Attachment): File {
        val output = File.createTempFile("pawar-inspect-", ".zip", context.cacheDir)
        var copied = 0L
        try {
            context.contentResolver.openInputStream(attachment.uri)?.use { input ->
                output.outputStream().use { target ->
                    val buffer = ByteArray(COPY_BUFFER)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        copied += read
                        if (copied > MAX_ARCHIVE_BYTES) error("Archive exceeds the 32 MiB inspection limit.")
                        target.write(buffer, 0, read)
                    }
                }
            } ?: error("Could not open the selected archive.")
            return output
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }

    private fun inspectArchive(name: String, archive: File): ToolResult {
        val entries = mutableListOf<String>()
        var unsafe = 0
        var truncated = false

        ZipFile(archive).use { zip ->
            val enumeration = zip.entries()
            while (enumeration.hasMoreElements()) {
                if (entries.size >= MAX_ENTRIES) {
                    truncated = true
                    break
                }
                val entry = enumeration.nextElement()
                entries += entry.name
                if (isUnsafe(entry.name)) unsafe++
            }
        }

        val projectType = detectProjectType(entries)
        val visible = entries.take(12).joinToString(", ")
        val safety = when {
            unsafe > 0 -> "$unsafe unsafe path(s) detected; no extraction or execution was performed."
            truncated -> "Entry list capped at $MAX_ENTRIES; no extraction or execution was performed."
            else -> "Archive paths passed the local safety check; no executable content was run."
        }

        return ToolResult(
            toolId = id,
            success = unsafe == 0,
            summary = "$name · ${entries.size}${if (truncated) "+" else ""} entries · $projectType",
            detail = "$safety Sample: ${if (visible.isBlank()) "(empty archive)" else visible}"
        )
    }

    private fun isUnsafe(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.startsWith("/") || normalized.split('/').any { it == ".." } ||
            normalized.contains("\u0000")
    }

    private fun detectProjectType(entries: List<String>): String {
        val fileNames = entries.map { it.substringAfterLast('/') }.toSet()
        return when {
            "settings.gradle.kts" in fileNames || "build.gradle.kts" in fileNames -> "Gradle/Kotlin project"
            "package.json" in fileNames -> "JavaScript/TypeScript project"
            "pyproject.toml" in fileNames || "requirements.txt" in fileNames -> "Python project"
            "Cargo.toml" in fileNames -> "Rust project"
            "go.mod" in fileNames -> "Go project"
            "CMakeLists.txt" in fileNames || "Makefile" in fileNames -> "C/C++ project"
            else -> "generic project/archive"
        }
    }

    private companion object {
        const val COPY_BUFFER = 64 * 1024
        const val MAX_ARCHIVE_BYTES = 32L * 1024L * 1024L
        const val MAX_ENTRIES = 2_000
    }
}

data class ToolResult(
    val toolId: String,
    val success: Boolean,
    val summary: String,
    val detail: String
)

/** Real ZIP workspace adapter exposed separately from the read-only inspectors. */
class LocalProjectWorkspaceCapability(private val context: android.content.Context) : ProjectWorkspaceCapability {
    override val id: String = "zip-project-workspace"
    override val description: String = "Safely extract, inspect, edit and package a ZIP project without executing its contents."

    override suspend fun open(attachment: Attachment): Result<ProjectWorkspace> =
        ProjectWorkspace.open(context.applicationContext, attachment)
}
