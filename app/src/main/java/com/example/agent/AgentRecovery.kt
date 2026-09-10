package com.example.agent

import com.example.accessibility.ScreenSnapshot
import com.example.vision.ScreenAnalyzer

sealed class RecoveryStrategy {
    data class ScrollAndRetry(val direction: String = "forward") : RecoveryStrategy()
    data class VisionCoordinateFallback(val target: ActionTarget) : RecoveryStrategy()
    data class DismissKeyboardOrDialog(val action: AgentAction) : RecoveryStrategy()
    data class Replan(val reason: String) : RecoveryStrategy()
    data object AbortSafely : RecoveryStrategy()
}

class AgentRecovery(private val maxRecoveryAttempts: Int = 3) {

    private var consecutiveFailures = 0

    fun reset() {
        consecutiveFailures = 0
    }

    fun determineRecovery(
        failedAction: AgentAction,
        screenSnapshot: ScreenSnapshot,
        errorMessage: String
    ): RecoveryStrategy {
        consecutiveFailures++

        if (consecutiveFailures > maxRecoveryAttempts) {
            return RecoveryStrategy.AbortSafely
        }

        // Recovery 1: If keyboard is active and might be blocking target
        if (screenSnapshot.isKeyboardVisible && failedAction.action == AgentActionType.CLICK) {
            return RecoveryStrategy.DismissKeyboardOrDialog(
                AgentAction(
                    action = AgentActionType.BACK,
                    reason = "Dismiss keyboard obscuring interactive target",
                    confidence = 0.90f
                )
            )
        }

        // Recovery 2: Try vision bounds estimation fallback
        val targetName = failedAction.target?.text ?: failedAction.target?.contentDescription
        if (!targetName.isNullOrBlank()) {
            val visualTarget = ScreenAnalyzer.estimateVisualTarget(targetName)
            if (visualTarget != null) {
                return RecoveryStrategy.VisionCoordinateFallback(visualTarget.toActionTarget())
            }
        }

        // Recovery 3: Scroll to bring element into view
        if (failedAction.action == AgentActionType.CLICK || failedAction.action == AgentActionType.FIND_ELEMENT) {
            return RecoveryStrategy.ScrollAndRetry("forward")
        }

        return RecoveryStrategy.Replan("Screen state updated; triggering agent replanning.")
    }
}
