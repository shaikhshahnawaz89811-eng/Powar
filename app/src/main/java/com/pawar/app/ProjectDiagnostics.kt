package com.pawar.app

/**
 * Read-only static diagnostics for a project workspace.
 * It never executes project code or Gradle tasks. Findings are evidence from
 * files that can be inspected locally; runtime/build failures require execution.
 */
object ProjectDiagnostics {
    fun inspect(workspace: ProjectWorkspace): DiagnosticReport {
        val files = workspace.listFiles()
        val findings = mutableListOf<DiagnosticFinding>()
        var scanned = 0
        files.forEach { path ->
            if (!isTextCandidate(path)) return@forEach
            val text = runCatching { workspace.readFile(path) }.getOrNull() ?: return@forEach
            scanned++
            val suffix = path.substringAfterLast('.', "").lowercase()
            when (suffix) {
                "kt", "kts", "java", "js", "ts", "tsx", "jsx", "py", "c", "cc", "cpp", "h", "hpp", "rs", "go" -> {
                    checkDelimiters(path, text, findings)
                }
            }
            checkObviousErrorMarkers(path, text, findings)
        }

        val names = files.map { it.substringAfterLast('/') }.toSet()
        if (names.any { it == "build.gradle.kts" || it == "settings.gradle.kts" } &&
            names.none { it == "gradlew" || it == "gradlew.bat" }) {
            findings += DiagnosticFinding(
                severity = DiagnosticSeverity.INFO,
                path = null,
                line = null,
                message = "Gradle wrapper is not included; this is not itself a source-code error, but build execution may require a compatible Gradle installation."
            )
        }

        return DiagnosticReport(scanned, findings.distinctBy { Triple(it.path, it.line, it.message) })
    }

    private fun isTextCandidate(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".java") ||
            lower.endsWith(".gradle") || lower.endsWith(".gradle.kts") || lower.endsWith(".xml") ||
            lower.endsWith(".json") || lower.endsWith(".toml") || lower.endsWith(".yaml") ||
            lower.endsWith(".yml") || lower.endsWith(".md") || lower.endsWith(".py") ||
            lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".tsx") ||
            lower.endsWith(".jsx") || lower.endsWith(".c") || lower.endsWith(".cc") ||
            lower.endsWith(".cpp") || lower.endsWith(".h") || lower.endsWith(".hpp") ||
            lower.endsWith(".rs") || lower.endsWith(".go")
    }

    private fun checkDelimiters(path: String, text: String, findings: MutableList<DiagnosticFinding>) {
        val stack = ArrayDeque<Pair<Char, Int>>()
        var inString = false
        var escaped = false
        var inLineComment = false
        var inBlockComment = false
        var line = 1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val next = if (i + 1 < text.length) text[i + 1] else '\u0000'
            if (c == '\n') {
                line++
                inLineComment = false
                i++
                continue
            }
            if (inLineComment) { i++; continue }
            if (inBlockComment) {
                if (c == '*' && next == '/') { inBlockComment = false; i += 2 } else i++
                continue
            }
            if (!inString && c == '/' && next == '/') { inLineComment = true; i += 2; continue }
            if (!inString && c == '/' && next == '*') { inBlockComment = true; i += 2; continue }
            if (c == '"' && !escaped) { inString = !inString; i++; continue }
            if (inString) {
                escaped = c == '\\' && !escaped
                if (c != '\\') escaped = false
                i++
                continue
            }
            when (c) {
                '(', '{', '[' -> stack.add(c to line)
                ')', '}', ']' -> {
                    val expected = when (c) { ')' -> '('; '}' -> '{'; else -> '[' }
                    val top = stack.removeLastOrNull()
                    if (top == null || top.first != expected) {
                        findings += DiagnosticFinding(DiagnosticSeverity.ERROR, path, line, "Unmatched '$c' delimiter.")
                        return
                    }
                }
            }
            i++
        }
        if (inBlockComment) findings += DiagnosticFinding(DiagnosticSeverity.WARNING, path, line, "Unclosed block comment detected.")
        if (inString) findings += DiagnosticFinding(DiagnosticSeverity.WARNING, path, line, "Unclosed string literal may be present.")
        stack.lastOrNull()?.let { (open, openLine) ->
            findings += DiagnosticFinding(DiagnosticSeverity.ERROR, path, openLine, "Unclosed '$open' delimiter.")
        }
    }

    private fun checkObviousErrorMarkers(path: String, text: String, findings: MutableList<DiagnosticFinding>) {
        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.startsWith("<<<<<<<") || line.startsWith("=======") || line.startsWith(">>>>>>>")) {
                findings += DiagnosticFinding(DiagnosticSeverity.ERROR, path, index + 1, "Git merge-conflict marker is still present.")
            }
        }
    }
}

data class DiagnosticReport(val filesScanned: Int, val findings: List<DiagnosticFinding>) {
    val hasErrors: Boolean get() = findings.any { it.severity == DiagnosticSeverity.ERROR }
}

data class DiagnosticFinding(
    val severity: DiagnosticSeverity,
    val path: String?,
    val line: Int?,
    val message: String
)

enum class DiagnosticSeverity { ERROR, WARNING, INFO }
