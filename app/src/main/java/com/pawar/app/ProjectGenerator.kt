package com.pawar.app

import android.content.Context
import java.io.File

/**
 * Local starter-project generator. It creates a safe, deterministic scaffold
 * for the project types Pawar can recognize without pretending to generate
 * arbitrary business logic without a code-generation engine.
 */
class LocalProjectGeneratorCapability(private val context: Context) : ProjectGeneratorCapability {
    override val id: String = "local-project-generator"
    override val description: String = "Creates deterministic starter scaffolds for supported project types."

    override suspend fun create(profile: TaskProfile, projectName: String): Result<ProjectWorkspace> = runCatching {
        require(profile.intent == TaskIntent.CREATE) { "Project generation requires a CREATE intent." }
        val safeName = projectName.trim().replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-').ifBlank { "PawarProject" }
        val root = File(context.filesDir, "generated-projects/$safeName-${System.currentTimeMillis()}").apply {
            mkdirs()
        }
        val blueprint = ProjectBlueprintPlanner.blueprint(profile)
            ?: error("No project blueprint is available for ${profile.projectType.label}.")
        blueprint.templates.forEach { (path, content) ->
            val target = File(root, path)
            target.parentFile?.mkdirs()
            target.writeText(content, Charsets.UTF_8)
        }
        ProjectWorkspace.fromDirectory(root, "$safeName.zip")
    }
}

interface ProjectGeneratorCapability : AgentCapability {
    suspend fun create(profile: TaskProfile, projectName: String): Result<ProjectWorkspace>
}
