package com.pawar.app

import android.content.Context
import java.io.File
import kotlinx.coroutines.yield

/**
 * Generic, result-driven agent orchestrator.
 *
 * Actions are selected from current state and actual capability/tool results;
 * the enum is a vocabulary, not a fixed workflow. Missing capabilities stop
 * honestly at the boundary instead of producing fabricated success.
 */
class DynamicAgentPipeline(context: Context, modelManager: ModelManager) {
    private val toolRegistry = LocalToolRegistry(context, modelManager)

    suspend fun execute(
        request: AgentRequest,
        previousRun: PipelineRun? = null,
        onUpdate: suspend (PipelineRun) -> Unit = {}
    ): PipelineRun {
        val effectiveRequest = resolveContinuation(request, previousRun)
        val profile = TaskPlanner.classify(effectiveRequest.text)
        val steps = mutableListOf<PipelineStep>()
        val toolResults = mutableListOf<ToolResult>()
        val state = AgentState(effectiveRequest, profile)

        suspend fun publish(response: String = "") {
            onUpdate(
                PipelineRun(
                    request = effectiveRequest,
                    steps = steps.toList(),
                    response = response,
                    toolResults = toolResults.toList(),
                    clarification = state.clarification,
                    options = state.options,
                    status = state.status,
                    verification = state.verification,
                    complete = false
                )
            )
        }

        suspend fun completeStep(index: Int, outcome: ActionOutcome) {
            val finalState = when (outcome) {
                ActionOutcome.Success -> PipelineStepState.COMPLETED
                ActionOutcome.Failed -> PipelineStepState.FAILED
                ActionOutcome.Blocked -> PipelineStepState.BLOCKED
            }
            steps[index] = steps[index].copy(state = finalState)
            publish()
        }

        suspend fun emit(
            stage: PipelineStage,
            title: String,
            detail: String,
            toolId: String? = null,
            outcome: ActionOutcome = ActionOutcome.Success
        ) {
            val index = steps.size
            steps += PipelineStep("action-${index + 1}", stage, title, detail, PipelineStepState.ACTIVE, toolId)
            publish()
            yield()
            completeStep(index, outcome)
        }

        suspend fun <T> executeAction(
            stage: PipelineStage,
            title: String,
            detail: String,
            toolId: String? = null,
            operation: suspend () -> T
        ): Result<T> {
            val index = steps.size
            steps += PipelineStep("action-${index + 1}", stage, title, detail, PipelineStepState.ACTIVE, toolId)
            publish()
            yield()
            val result = runCatching { operation() }
            completeStep(index, if (result.isSuccess) ActionOutcome.Success else ActionOutcome.Failed)
            return result
        }

        state.status = AgentStatus.RUNNING
        emit(
            PipelineStage.UNDERSTAND,
            "Understanding request",
            "${effectiveRequest.summary()} · ${profile.intent.name.lowercase()} · ${profile.projectType.label} · ${profile.outputMode.name.lowercase()}"
        )

        if (effectiveRequest.attachments.isNotEmpty()) {
            emit(
                PipelineStage.CONTEXT,
                "Inspecting context",
                "${effectiveRequest.attachments.size} attachment(s) available · ${toolRegistry.capabilities().joinToString() }"
            )
            val inspectedResult = executeAction(
                PipelineStage.INSPECT,
                "Inspecting provided inputs",
                "Reading attachment metadata and safe local structure before deciding the next action.",
                toolId = "attachment-inspection"
            ) { toolRegistry.inspect(effectiveRequest.attachments) }
            val inspected = inspectedResult.getOrElse {
                listOf(ToolResult("attachment-inspection", false, "Attachment inspection failed", it.message ?: "Inspection failed."))
            }
            toolResults += inspected
            state.lastToolResults = inspected
        } else {
            emit(PipelineStage.CONTEXT, "Checking available context", "No attachments were supplied.")
        }

        if (effectiveRequest.text.isBlank()) {
            state.clarification = ClarificationEngine.forRequest(effectiveRequest)
            state.options = state.clarification?.options.orEmpty()
            state.status = AgentStatus.WAITING_FOR_USER
            emit(PipelineStage.CLARIFY, "Clarification needed", state.clarification!!.question)
            return finish(state, steps, toolResults, state.clarification!!.question, onUpdate)
        }

        if (profile.outputMode == OutputMode.OPTIONS_ONLY) {
            state.options = OptionEngine.forProfile(profile, effectiveRequest)
            state.status = AgentStatus.COMPLETED
            emit(PipelineStage.OPTIONS, "Preparing options", "Options were prepared without changing project files.")
            return finish(state, steps, toolResults, optionsText(state.options), onUpdate)
        }

        if (profile.outputMode == OutputMode.CODE_ONLY) {
            if (effectiveRequest.modelLoaded) {
                val generationResult = executeAction(
                    PipelineStage.CODE_ONLY,
                    "Generating code locally",
                    "Running the loaded module through llama.cpp to answer the code-only request.",
                    toolId = toolRegistry.codeGenerationCapability.id
                ) { toolRegistry.generateCode(effectiveRequest.text).getOrThrow() }

                return generationResult.fold(
                    onSuccess = { code ->
                        state.status = AgentStatus.COMPLETED
                        finish(state, steps, toolResults, code, onUpdate)
                    },
                    onFailure = { error ->
                        emit(
                            PipelineStage.CODE_ONLY,
                            "Local generation unavailable",
                            error.message ?: "Local code generation failed.",
                            outcome = ActionOutcome.Blocked
                        )
                        state.status = AgentStatus.BLOCKED
                        finish(
                            state,
                            steps,
                            toolResults,
                            "Module loaded hai, lekin local generation complete nahi ho paya: ${error.message}. Koi fake code nahi diya gaya.",
                            onUpdate
                        )
                    }
                )
            }
            emit(
                PipelineStage.CODE_ONLY,
                "Code-only response boundary",
                "Project mutation and ZIP packaging are disabled for CODE_ONLY mode.",
                outcome = ActionOutcome.Blocked
            )
            state.status = AgentStatus.BLOCKED
            return finish(
                state,
                steps,
                toolResults,
                "CODE_ONLY mode selected, but no local module is loaded (or the native library is not built yet). No fake code was generated.",
                onUpdate
            )
        }

        if (profile.intent == TaskIntent.UNKNOWN) {
            state.options = OptionEngine.forUnknown(effectiveRequest)
            state.clarification = ClarificationRequest(
                "Aap is request par kya karwana chahte hain?",
                "The requested outcome is not specific enough to choose a safe action.",
                state.options
            )
            state.status = AgentStatus.WAITING_FOR_USER
            emit(PipelineStage.CLARIFY, "Choosing the next action", state.clarification!!.question)
            return finish(state, steps, toolResults, state.clarification!!.question, onUpdate)
        }

        emit(PipelineStage.PLAN, "Planning next action", profile.plan)

        // The next action is selected from the current state, not from a fixed
        // stage chain. A new tool result below can change the branch.
        when (profile.intent) {
            TaskIntent.CREATE -> {
                if (profile.outputMode == OutputMode.PROJECT_MUTATION || profile.outputMode == OutputMode.ZIP_OUTPUT) {
                    if (effectiveRequest.attachments.isEmpty()) {
                    val generatedResult = executeAction(
                        PipelineStage.CREATE,
                        "Creating starter project",
                        "Writing a deterministic ${profile.projectType.label} scaffold to the local project workspace.",
                        toolId = toolRegistry.projectGeneratorCapability.id
                    ) { toolRegistry.createStarterProject(profile, "PawarProject").getOrThrow() }
                    if (generatedResult.isSuccess) {
                        state.workspace = generatedResult.getOrThrow()
                        state.workspaceSummary = workspaceSummary(state.workspace!!)
                        emit(PipelineStage.REEVALUATE, "Re-evaluating plan", "Creation returned a real workspace with ${state.workspace!!.listFiles().size} file(s); the plan now moves to verification.")
                        if (profile.outputMode == OutputMode.ZIP_OUTPUT) {
                            val packaged = packageWorkspace(state, toolRegistry)
                            if (packaged != null) {
                                emit(PipelineStage.PACKAGE, "Preparing ZIP", packaged)
                            } else {
                                state.status = AgentStatus.BLOCKED
                                emit(PipelineStage.PACKAGE, "ZIP packaging failed", "The workspace was created, but the ZIP artifact could not be produced.", outcome = ActionOutcome.Failed)
                            }
                        }
                    } else {
                        state.status = AgentStatus.BLOCKED
                        emit(PipelineStage.RESULT, "Project creation blocked", generatedResult.exceptionOrNull()?.message ?: "Starter project could not be created.", outcome = ActionOutcome.Blocked)
                    }
                    } else {
                        state.status = AgentStatus.BLOCKED
                        emit(PipelineStage.CREATE, "Creation needs a workspace decision", "A project attachment was supplied with a CREATE request; Pawar will not overwrite it without a clear target.", outcome = ActionOutcome.Blocked)
                    }
                } else {
                    state.status = AgentStatus.BLOCKED
                    emit(PipelineStage.CODE_ONLY, "Waiting for code-generation capability", "This CREATE request is a code response rather than a project scaffold. No connected code-generation runtime is available in this build.", outcome = ActionOutcome.Blocked)
                }
            }
            TaskIntent.INSPECT, TaskIntent.EXPLAIN -> {
                val workspaceAttachment = effectiveRequest.attachments.firstOrNull { it.kind == AttachmentKind.ZIP }
                if (workspaceAttachment != null) {
                    val openedResult = executeAction(
                        PipelineStage.READ_FILE,
                        "Reading project structure",
                        "Opening the ZIP in a sandboxed workspace and inspecting its file structure.",
                        toolId = toolRegistry.projectWorkspaceCapability.id
                    ) { toolRegistry.openProjectWorkspace(workspaceAttachment).getOrThrow() }
                    if (openedResult.isSuccess) {
                        state.workspace = openedResult.getOrThrow()
                        state.workspaceSummary = workspaceSummary(state.workspace!!)
                        val diagnostics = executeAction(
                            PipelineStage.SEARCH_FILES,
                            "Checking project files for errors",
                            "Running read-only static diagnostics over the extracted workspace; project code is not executed.",
                            toolId = "static-project-diagnostics"
                        ) { toolRegistry.diagnose(state.workspace!!) }
                        if (diagnostics.isSuccess) {
                            toolResults += diagnostics.getOrThrow()
                            state.lastToolResults = toolResults.toList()
                        }
                    } else {
                        state.status = AgentStatus.BLOCKED
                        emit(PipelineStage.RESULT, "Project workspace unavailable", openedResult.exceptionOrNull()?.message ?: "ZIP workspace could not be opened.", outcome = ActionOutcome.Failed)
                    }
                } else {
                    emit(PipelineStage.RESULT, "Preparing result", "The available local inspection results will be used for the read-only response.")
                }
            }
            TaskIntent.FIX, TaskIntent.MODIFY -> {
                val workspaceAttachment = effectiveRequest.attachments.firstOrNull { it.kind == AttachmentKind.ZIP }
                if (workspaceAttachment != null) {
                    val openedResult = executeAction(
                        PipelineStage.READ_FILE,
                        "Reading project structure",
                        "Opening the ZIP in a sandboxed workspace and inspecting its file structure.",
                        toolId = toolRegistry.projectWorkspaceCapability.id
                    ) { toolRegistry.openProjectWorkspace(workspaceAttachment).getOrThrow() }
                    if (openedResult.isSuccess) {
                        state.workspace = openedResult.getOrThrow()
                        state.workspaceSummary = workspaceSummary(state.workspace!!)
                        val diagnostics = executeAction(
                            PipelineStage.SEARCH_FILES,
                            "Finding likely errors",
                            "Running read-only static diagnostics before any edit decision; project code is not executed.",
                            toolId = "static-project-diagnostics"
                        ) { toolRegistry.diagnose(state.workspace!!) }
                        if (diagnostics.isSuccess) {
                            toolResults += diagnostics.getOrThrow()
                            state.lastToolResults = toolResults.toList()
                        }
                        state.status = AgentStatus.BLOCKED
                        emit(
                            PipelineStage.EDIT,
                            "Waiting for code-generation capability",
                            "The real workspace is open and writable, but this build has no connected reasoning/code-generation adapter to decide and apply a safe patch.",
                            outcome = ActionOutcome.Blocked
                        )
                    } else {
                        state.status = AgentStatus.BLOCKED
                        emit(PipelineStage.RESULT, "Project workspace unavailable", openedResult.exceptionOrNull()?.message ?: "ZIP workspace could not be opened.", outcome = ActionOutcome.Failed)
                    }
                } else {
                    state.status = AgentStatus.BLOCKED
                    emit(PipelineStage.EDIT, "Waiting for project input", "A mutation request needs an existing project workspace or a connected code-generation runtime.", outcome = ActionOutcome.Blocked)
                }
            }
            TaskIntent.RESEARCH -> {
                state.status = AgentStatus.BLOCKED
                emit(PipelineStage.RESEARCH, "Research unavailable", "No live research adapter is connected in this build; current information was not fabricated.", outcome = ActionOutcome.Blocked)
            }
            TaskIntent.CLARIFY, TaskIntent.UNKNOWN -> Unit
        }

        if (state.status != AgentStatus.BLOCKED) {
            emit(PipelineStage.REEVALUATE, "Re-evaluating plan", ReEvaluationEngine.describe(state))
        }

        state.verification = VerificationEngine.verify(state, toolResults)
        emit(
            PipelineStage.VERIFY,
            "Verifying output",
            state.verification!!.detail,
            outcome = if (state.verification!!.success) ActionOutcome.Success else ActionOutcome.Failed
        )
        if (!state.verification!!.success) state.status = AgentStatus.BLOCKED
        if (state.status == AgentStatus.RUNNING) state.status = AgentStatus.COMPLETED

        return finish(state, steps, toolResults, FinalResponseEngine.build(state), onUpdate)
    }

    private suspend fun packageWorkspace(state: AgentState, registry: LocalToolRegistry): String? {
        val workspace = state.workspace ?: return null
        return runCatching {
            val output = workspace.packageZip(state.requestPackageDirectory(registry))
            state.packagePath = output.absolutePath
            "ZIP created: ${output.name}"
        }.getOrNull()
    }

    private fun AgentState.requestPackageDirectory(registry: LocalToolRegistry): File =
        File(registry.appCacheDir(), "artifacts")

    private fun workspaceSummary(workspace: ProjectWorkspace): String {
        val files = workspace.listFiles()
        val sample = files.take(12).joinToString(", ")
        return "Workspace contains ${files.size} file(s). Sample: ${if (sample.isBlank()) "(empty)" else sample}"
    }

    private fun optionsText(options: List<AgentOption>): String =
        options.joinToString("\n") { "${it.id}. ${it.title} — ${it.description}" }

    private fun resolveContinuation(request: AgentRequest, previousRun: PipelineRun?): AgentRequest {
        if (previousRun?.status != AgentStatus.WAITING_FOR_USER || previousRun.clarification == null) return request
        val answer = request.text.trim()
        val normalizedAnswer = answer.lowercase()
        val option = previousRun.options.firstOrNull { option ->
            option.id.equals(answer, ignoreCase = true) ||
                option.title.lowercase() == normalizedAnswer ||
                normalizedAnswer == option.title.lowercase().removePrefix("fix ")
        }
        val mergedText = if (option != null) {
            val marker = when (option.title) {
                "Write code" -> "[CODE_REQUEST]"
                "Create a project" -> "[CREATE_PROJECT]"
                "Fix a problem", "Fix bugs" -> "[FIX_REQUEST]"
                "Explain / analyze", "Explain" -> "[EXPLAIN_REQUEST]"
                "Inspect / review" -> "[INSPECT_REQUEST]"
                "Change / add feature" -> "[MODIFY_REQUEST]"
                else -> "[USER_OPTION]"
            }
            "${previousRun.request.text}\n$marker Selected action: ${option.title}. ${option.description}"
        } else if (previousRun.request.text.isNotBlank()) {
            "${previousRun.request.text}\nUser clarification: $answer"
        } else {
            answer
        }
        return AgentRequest(mergedText, if (request.attachments.isNotEmpty()) request.attachments else previousRun.request.attachments, request.modelLoaded)
    }

    private suspend fun finish(
        state: AgentState,
        steps: List<PipelineStep>,
        toolResults: List<ToolResult>,
        response: String,
        onUpdate: suspend (PipelineRun) -> Unit
    ): PipelineRun {
        val result = PipelineRun(
            request = state.request,
            steps = steps.toList(),
            response = response,
            toolResults = toolResults.toList(),
            clarification = state.clarification,
            options = state.options,
            status = state.status,
            verification = state.verification,
            artifactPath = state.packagePath,
            complete = true
        )
        onUpdate(result)
        return result
    }
}

data class AgentRequest(
    val text: String,
    val attachments: List<Attachment>,
    val modelLoaded: Boolean
) {
    fun summary(): String = when {
        text.isNotBlank() && attachments.isNotEmpty() -> "Text request plus ${attachments.size} attached input(s)"
        text.isNotBlank() -> "Text request: ${text.trim().replace(Regex("\\s+"), " ").take(120)}"
        else -> "Attachment-only request"
    }
}

enum class PipelineStage {
    UNDERSTAND, CONTEXT, CLARIFY, OPTIONS, PLAN, INSPECT, ANALYZE_IMAGE, SEARCH_FILES,
    READ_FILE, RESEARCH, CODE_ONLY, CREATE, EDIT, MODIFY, FIX, WIRE, ACTION, RESULT,
    REEVALUATE, RETRY, VERIFY, PACKAGE, FINAL_RESPONSE
}

enum class PipelineStepState { ACTIVE, COMPLETED, FAILED, BLOCKED }
enum class AgentStatus { RUNNING, WAITING_FOR_USER, COMPLETED, BLOCKED }
enum class ActionOutcome { Success, Failed, Blocked }

data class PipelineStep(
    val id: String,
    val stage: PipelineStage,
    val title: String,
    val detail: String,
    val state: PipelineStepState,
    val toolId: String? = null
)

data class PipelineRun(
    val request: AgentRequest,
    val steps: List<PipelineStep>,
    val response: String,
    val toolResults: List<ToolResult> = emptyList(),
    val clarification: ClarificationRequest? = null,
    val options: List<AgentOption> = emptyList(),
    val status: AgentStatus = AgentStatus.RUNNING,
    val verification: VerificationResult? = null,
    val artifactPath: String? = null,
    val complete: Boolean
)

data class AgentOption(val id: String, val title: String, val description: String)
data class ClarificationRequest(val question: String, val reason: String, val options: List<AgentOption>)
data class VerificationResult(val success: Boolean, val detail: String, val checks: List<String> = emptyList())

private data class AgentState(
    val request: AgentRequest,
    val profile: TaskProfile,
    var lastToolResults: List<ToolResult> = emptyList(),
    var clarification: ClarificationRequest? = null,
    var options: List<AgentOption> = emptyList(),
    var verification: VerificationResult? = null,
    var status: AgentStatus = AgentStatus.RUNNING,
    var workspace: ProjectWorkspace? = null,
    var workspaceSummary: String? = null,
    var packagePath: String? = null
)

private object ClarificationEngine {
    fun forRequest(request: AgentRequest): ClarificationRequest? = when {
        request.text.isBlank() && request.attachments.isNotEmpty() -> ClarificationRequest(
            "Attachment mil gaya. Aap iske saath kya karwana chahte hain?",
            "The attachment is available but the desired outcome is missing.",
            OptionEngine.forAttachments()
        )
        request.text.isBlank() -> ClarificationRequest(
            "Aap Pawar se kya karwana chahte hain?",
            "No task description was provided.",
            OptionEngine.forUnknown(request)
        )
        else -> null
    }
}

private object OptionEngine {
    fun forAttachments(): List<AgentOption> = listOf(
        AgentOption("1", "Inspect / review", "Input ko read-only inspect karke findings batao."),
        AgentOption("2", "Fix bugs", "Errors/root causes find karke required fixes route karo."),
        AgentOption("3", "Change / add feature", "Requested feature ya modification route karo."),
        AgentOption("4", "Explain", "Available input ka behavior aur structure samjhao."),
        AgentOption("5", "Compare with an image", "Reference screenshot ke against UI context analyze karo.")
    )

    fun forUnknown(request: AgentRequest): List<AgentOption> = if (request.attachments.isNotEmpty()) {
        forAttachments()
    } else listOf(
        AgentOption("1", "Write code", "Requested code generation route."),
        AgentOption("2", "Create a project", "Naya project scaffold/implementation route."),
        AgentOption("3", "Fix a problem", "Existing error/bug investigation route."),
        AgentOption("4", "Explain / analyze", "Read-only explanation route.")
    )

    fun forProfile(profile: TaskProfile, request: AgentRequest): List<AgentOption> = when (profile.projectType) {
        ProjectType.ANDROID -> listOf(
            AgentOption("1", "Existing Android structure", "Current files inspect karke targeted change."),
            AgentOption("2", "New Android implementation", "Required Android scaffold/implementation."),
            AgentOption("3", "Read-only analysis", "Files ko modify kiye bina findings.")
        )
        ProjectType.WEB -> listOf(
            AgentOption("1", "Existing web structure", "Current web files inspect karo."),
            AgentOption("2", "New web implementation", "Required web scaffold/implementation."),
            AgentOption("3", "Read-only analysis", "Current behavior explain/review karo.")
        )
        else -> forUnknown(request)
    }
}

private object ReEvaluationEngine {
    fun describe(state: AgentState): String = when {
        state.status == AgentStatus.BLOCKED -> "A capability boundary changed the available plan; execution stopped safely at the actual blocker."
        state.workspace != null -> "A real workspace result is now available; the plan is constrained by its file structure and verification can continue."
        state.lastToolResults.isNotEmpty() -> "Actual local inspection returned ${state.lastToolResults.size} result(s); the next action is constrained by those results."
        else -> "Current request, output mode and available capabilities were re-evaluated before finishing."
    }
}

private object VerificationEngine {
    fun verify(state: AgentState, results: List<ToolResult>): VerificationResult {
        val checks = mutableListOf(
            "Task intent and output mode are present.",
            "Only actions represented in the activity stream were executed.",
            "No APK/device execution was started by the agent pipeline."
        )
        if (results.isNotEmpty()) checks += "Attachment tool results were recorded in state."
        if (state.workspace != null) checks += "Workspace canonical-path and bounded file operations are active."
        if (state.packagePath != null) checks += "A real ZIP artifact was created from the workspace."
        val failed = results.any { !it.success }
        val boundaryBlocked = state.status == AgentStatus.BLOCKED
        return VerificationResult(
            // Verification answers whether the checks themselves completed. A blocked
            // task is still allowed to have successful verification of its evidence/safety.
            success = !failed,
            detail = when {
                failed -> "Verification found a failed local tool operation."
                boundaryBlocked -> "Verification checks passed; task remains blocked at an unavailable capability boundary. No build/device execution was performed."
                else -> "State, tool results and local execution boundaries were checked without build/device execution."
            },
            checks = checks
        )
    }
}

private object FinalResponseEngine {
    fun build(state: AgentState): String {
        val diagnostic = state.lastToolResults.firstOrNull { it.toolId == "static-project-diagnostics" }
        if (diagnostic != null) {
            val workspace = state.workspaceSummary?.let { "\nWorkspace: $it" }.orEmpty()
            val boundary = if (state.status == AgentStatus.BLOCKED) {
                "\nStatic findings report ho gaye; automatic code edit ke liye connected code-generation runtime abhi available nahi hai."
            } else ""
            return "Project ko read-only inspect kiya.\n${diagnostic.summary}.\n${diagnostic.detail}$workspace$boundary"
        }
        if (state.status == AgentStatus.BLOCKED) {
            val workspace = state.workspaceSummary?.let { " Workspace: $it" }.orEmpty()
            return "Request samajh liya, lekin required capability boundary par run ruk gaya. Koi fake change/result claim nahi kiya.$workspace"
        }
        if (state.packagePath != null) return "Starter project create karke ZIP package bhi banaya gaya: ${File(state.packagePath!!).name}"
        if (state.workspaceSummary != null) return "Project workspace inspect/create hua. ${state.workspaceSummary}"
        return if (state.lastToolResults.isNotEmpty()) {
            "Request process hua aur available local inspection results ko state me record kiya gaya."
        } else {
            "Request process hua aur available context ke hisaab se result prepare kiya gaya."
        }
    }
}
