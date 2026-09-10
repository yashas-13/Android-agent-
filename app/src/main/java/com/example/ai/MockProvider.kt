package com.example.ai

import com.example.accessibility.ScreenSnapshot
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import com.example.agent.ExecutedAction

class MockProvider : AiProvider {

    override val providerType = AiProviderType.MOCK_SIMULATOR
    override val displayName = "Simulation Test Engine (Mock)"

    private var stepCounter = 0

    fun reset() {
        stepCounter = 0
    }

    override suspend fun planNextAction(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        stepCounter++
        return when (stepCounter) {
            1 -> AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.OPEN_APP,
                    payload = "com.whatsapp",
                    reason = "[Simulated] Launch WhatsApp",
                    confidence = 0.99f,
                    expectedOutcome = "WhatsApp Home View"
                ),
                thoughtProcess = "[Mock] Evaluating task; initiating primary target application launch."
            )
            2 -> AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = ActionTarget(text = "Product Team", role = "button"),
                    reason = "[Simulated] Select 'Product Team' group chat",
                    confidence = 0.98f,
                    expectedOutcome = "Group conversation active"
                ),
                thoughtProcess = "[Mock] Detected target group in conversational list."
            )
            3 -> AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.TYPE_TEXT,
                    target = ActionTarget(text = "Type a message", role = "input"),
                    payload = "Sprint report ready for review.",
                    reason = "[Simulated] Input group message",
                    confidence = 0.97f,
                    expectedOutcome = "Message field populated"
                ),
                thoughtProcess = "[Mock] Focussing input element and inserting payload."
            )
            4 -> AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = ActionTarget(text = "Send", role = "button"),
                    reason = "[Simulated] Click send button",
                    confidence = 0.99f,
                    expectedOutcome = "Sent status verified"
                ),
                thoughtProcess = "[Mock] Validating payload format and transmitting."
            )
            else -> AgentPlanResponse(
                action = null,
                isTaskComplete = true,
                completionMessage = "[Simulated] Automation sequence successfully completed!",
                thoughtProcess = "[Mock] All planned sub-goals verified successfully.",
                confidence = 1.0f
            )
        }
    }

    override suspend fun verifyOutcome(
        task: String,
        executedAction: AgentAction,
        previousSnapshot: ScreenSnapshot,
        currentSnapshot: ScreenSnapshot
    ): VerificationResult {
        return VerificationResult(
            isSuccess = true,
            explanation = "[Mock Verification] State transition verified."
        )
    }
}
