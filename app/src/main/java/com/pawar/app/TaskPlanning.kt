package com.pawar.app

import java.util.Locale

/**
 * Lightweight deterministic fallback planner.
 *
 * It decides intent and response mode only. It is deliberately not a model,
 * code generator, or project editor. A future reasoning runtime can replace
 * this decision layer without changing the orchestration contracts.
 */
object TaskPlanner {
    fun classify(text: String): TaskProfile {
        val normalized = text.trim().lowercase(Locale.ROOT)
        val explicitOutputMode = detectOutputMode(normalized)
        val intent = detectIntent(normalized, explicitOutputMode)
        val projectType = detectProjectType(normalized)
        val projectTargetRequested = containsAny(
            normalized,
            "create project", "create a project", "new project", "project bana", "project banao",
            "build an app", "build app", "build a website", "website bana",
            "android app", "new app", "scaffold", "starter project",
            "project create", "project modify", "project fix", "zip"
        )
        val codeRequest = containsAny(
            normalized,
            "write code", "code likho", "code likh", "generate code", "code bana",
            "function likho", "class likho", "snippet", "code do", "code chahiye"
        ) || (containsAny(normalized, "code") && containsAny(
            normalized, "python", "kotlin", "java", "javascript", "typescript", "c++", "cpp", "rust", "go"
        ))
        val outputMode = when {
            explicitOutputMode != OutputMode.NORMAL_RESPONSE -> explicitOutputMode
            intent in setOf(TaskIntent.MODIFY, TaskIntent.FIX) -> {
                if (containsAny(normalized, "zip", "archive", "downloadable zip", "zip bana", "zip do")) OutputMode.ZIP_OUTPUT
                else OutputMode.PROJECT_MUTATION
            }
            intent == TaskIntent.CREATE && projectTargetRequested && !codeRequest -> {
                if (containsAny(normalized, "zip", "archive", "downloadable zip", "zip bana", "zip do")) OutputMode.ZIP_OUTPUT
                else OutputMode.PROJECT_MUTATION
            }
            else -> OutputMode.NORMAL_RESPONSE
        }

        return TaskProfile(
            intent = intent,
            projectType = projectType,
            outputMode = outputMode,
            requiresWrite = outputMode == OutputMode.PROJECT_MUTATION || outputMode == OutputMode.ZIP_OUTPUT,
            plan = planFor(intent, outputMode)
        )
    }

    private fun detectOutputMode(text: String): OutputMode = when {
        text.isBlank() -> OutputMode.NORMAL_RESPONSE
        containsAny(
            text,
            "sirf code", "sirf kotlin code", "sirf java code", "only code",
            "code only", "just code", "write code only", "just write the code",
            "bas code", "bas python code", "bas kotlin code", "bas java code", "kuch nahin bas"
        ) || Regex("(?:sirf|only|just|bas)\\s+(?:python|kotlin|java|javascript|typescript|c\\+\\+|cpp|rust|go)\\s+code(?:\\s|$)").containsMatchIn(text) -> OutputMode.CODE_ONLY
        containsAny(
            text,
            "options only", "sirf options", "only options", "mere options batao",
            "options batao", "what are my options"
        ) -> OutputMode.OPTIONS_ONLY
        else -> OutputMode.NORMAL_RESPONSE
    }

    private fun detectIntent(text: String, outputMode: OutputMode): TaskIntent {
        if (text.isBlank()) return TaskIntent.CLARIFY
        when {
            text.contains("[create_project]") -> return TaskIntent.CREATE
            text.contains("[fix_request]") -> return TaskIntent.FIX
            text.contains("[modify_request]") -> return TaskIntent.MODIFY
            text.contains("[explain_request]") -> return TaskIntent.EXPLAIN
            text.contains("[inspect_request]") -> return TaskIntent.INSPECT
            text.contains("[code_request]") -> return TaskIntent.CREATE
        }
        if (outputMode == OutputMode.CODE_ONLY && containsAny(text, "code", "kotlin", "java", "python", "javascript", "function", "class")) {
            return TaskIntent.CREATE
        }
        return when {
            containsAny(text, "find error", "find errors", "find bug", "find bugs", "find the error", "find the bug", "error find", "errors find", "bug find", "bugs find", "error dhundo", "bug dhundo", "error dhoondo", "bug dhoondo", "गलती ढूंढ", "बग ढूंढ") -> TaskIntent.INSPECT
            containsAny(text, "fix", "repair", "debug", "patch", "resolve", "ठीक", "सुधार") -> TaskIntent.FIX
            containsAny(text, "bug", "error", "crash", "issue", "exception", "failure", "गलती", "बग") -> TaskIntent.FIX
            containsAny(text, "modify", "change", "update", "add", "implement", "replace", "remove", "बदल", "जोड़", "बनाओ", "बना") -> TaskIntent.MODIFY
            containsAny(text, "create", "build", "make", "generate", "new project", "new app", "website", "बनाना", "तैयार") -> TaskIntent.CREATE
            (containsAny(text, "code") && containsAny(text, "likho", "likh", "do", "chahiye", "write", "generate")) -> TaskIntent.CREATE
            containsAny(text, "inspect", "analyze", "analyse", "review", "audit", "read", "देख", "जांच") -> TaskIntent.INSPECT
            containsAny(text, "explain", "why", "what is", "how does", "क्यों", "समझाओ") -> TaskIntent.EXPLAIN
            containsAny(text, "research", "search", "latest", "documentation", "compare", "तुलना", "रिसर्च") -> TaskIntent.RESEARCH
            else -> TaskIntent.UNKNOWN
        }
    }

    private fun detectProjectType(text: String): ProjectType = when {
        containsAny(text, "android", "jetpack compose", "apk", "android studio", "androidmanifest", "android activity") -> ProjectType.ANDROID
        containsAny(text, "website", "web app", "frontend", "react", "next.js", "html", "css", "javascript", "typescript") -> ProjectType.WEB
        containsAny(text, "python", "django", "flask", "fastapi", "pandas") -> ProjectType.PYTHON
        containsAny(text, "c++", "cpp", "cmake", "native", "embedded") -> ProjectType.NATIVE
        containsAny(text, "kotlin/jvm", "kotlin jvm", "jvm", "java", "spring", "gradle project", "kotlin project", "kotlin") -> ProjectType.JVM
        containsAny(text, "rust", "cargo") -> ProjectType.RUST
        containsAny(text, "golang", "go service", "go project") -> ProjectType.GO
        else -> ProjectType.GENERIC
    }

    private fun planFor(intent: TaskIntent, outputMode: OutputMode): String {
        if (outputMode == OutputMode.CODE_ONLY) {
            return "Generate only the requested code; do not mutate the project or package a ZIP."
        }
        return when (intent) {
            TaskIntent.CLARIFY -> "Determine the missing outcome and ask only the necessary question."
            TaskIntent.CREATE -> "Understand the requested target, inspect available context, create only required parts, wire them, and verify."
            TaskIntent.MODIFY -> "Inspect the existing structure, change only affected parts, wire required integrations, and verify."
            TaskIntent.FIX -> "Inspect evidence, isolate the root cause, patch the smallest necessary area, re-evaluate, and verify."
            TaskIntent.INSPECT -> "Inspect relevant context and report evidence without mutating files."
            TaskIntent.EXPLAIN -> "Inspect enough context to explain the behavior accurately and avoid unsupported claims."
            TaskIntent.RESEARCH -> "Research only when current/external information is required, then re-evaluate the implementation plan."
            TaskIntent.UNKNOWN -> "Inspect context, determine the user's desired outcome, clarify if necessary, then choose the smallest safe action."
        }
    }

    private fun containsAny(value: String, vararg candidates: String): Boolean =
        candidates.any(value::contains)
}

data class TaskProfile(
    val intent: TaskIntent,
    val projectType: ProjectType,
    val outputMode: OutputMode,
    val requiresWrite: Boolean,
    val plan: String
)

enum class TaskIntent {
    CREATE,
    MODIFY,
    FIX,
    INSPECT,
    EXPLAIN,
    RESEARCH,
    CLARIFY,
    UNKNOWN
}

enum class OutputMode {
    NORMAL_RESPONSE,
    CODE_ONLY,
    OPTIONS_ONLY,
    CLARIFICATION,
    PROJECT_MUTATION,
    ZIP_OUTPUT
}

enum class ProjectType(val label: String) {
    ANDROID("Android"),
    WEB("web"),
    PYTHON("Python"),
    NATIVE("native"),
    JVM("JVM"),
    RUST("Rust"),
    GO("Go"),
    GENERIC("generic")
}
