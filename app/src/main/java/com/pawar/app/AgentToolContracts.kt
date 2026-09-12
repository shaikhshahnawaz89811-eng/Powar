package com.pawar.app

/** Generic contracts used by the orchestrator. Implementations are deliberately replaceable. */
interface AgentCapability {
    val id: String
    val description: String
}

data class ToolCall(
    val id: String,
    val toolId: String,
    val arguments: Map<String, String> = emptyMap()
)

data class ToolExecution(
    val call: ToolCall,
    val result: ToolResult,
    val changedFiles: List<String> = emptyList()
)

interface ProjectWorkspaceCapability : AgentCapability {
    suspend fun open(attachment: Attachment): Result<ProjectWorkspace>
}

interface ImageAnalysisCapability : AgentCapability {
    suspend fun analyze(attachment: Attachment): Result<String>
}

interface ResearchCapability : AgentCapability {
    suspend fun search(query: String): Result<String>
}

interface CodeGenerationCapability : AgentCapability {
    suspend fun generate(request: String): Result<String>
}

/**
 * Adapter boundary for future model-driven action selection. The orchestrator
 * must be able to operate without this adapter and must never fabricate its result.
 */
interface AgentReasoningCapability : AgentCapability {
    suspend fun decide(state: AgentDecisionContext): Result<AgentDecision>
}

data class AgentDecisionContext(
    val request: AgentRequest,
    val profile: TaskProfile,
    val recentSteps: List<PipelineStep>,
    val toolResults: List<ToolResult>
)

data class AgentDecision(
    val action: PipelineStage,
    val reason: String
)
