package com.example.ai

import com.example.accessibility.ScreenSnapshot
import com.example.agent.AgentAction
import com.example.agent.ExecutedAction

data class AgentPlanResponse(
    val action: AgentAction?,
    val isTaskComplete: Boolean = false,
    val completionMessage: String? = null,
    val thoughtProcess: String = "",
    val confidence: Float = 0.95f
)

data class VerificationResult(
    val isSuccess: Boolean,
    val explanation: String,
    val requiresRecovery: Boolean = false
)

enum class AiProviderType {
    CACTUS_NEEDLE,
    LOCAL_ENGINE,
    GEMINI_CLOUD,
    MOCK_SIMULATOR
}

interface AiProvider {
    val providerType: AiProviderType
    val displayName: String

    suspend fun planNextAction(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse

    suspend fun verifyOutcome(
        task: String,
        executedAction: AgentAction,
        previousSnapshot: ScreenSnapshot,
        currentSnapshot: ScreenSnapshot
    ): VerificationResult
}
