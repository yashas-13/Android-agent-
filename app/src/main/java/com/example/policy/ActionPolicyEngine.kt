package com.example.policy

import com.example.agent.AgentAction
import com.example.agent.AgentActionType

enum class PolicyDecision {
    ALLOWED,
    REQUIRES_CONFIRMATION,
    BLOCKED
}

data class PolicyEvaluation(
    val decision: PolicyDecision,
    val reason: String,
    val requiresExplicitUserApproval: Boolean = false
)

object ActionPolicyEngine {

    const val DEFAULT_MAX_ACTIONS_PER_TASK = 20
    const val ACTION_TIMEOUT_MS = 8000L
    const val TASK_TIMEOUT_MS = 120_000L

    private val SENSITIVE_KEYWORDS = listOf(
        "password", "pin", "cvv", "otp", "private key", "seed phrase", "secret"
    )

    private val CONFIRMATION_ACTIONS = listOf(
        "send", "delete", "remove", "pay", "buy", "purchase", "transfer", "submit", "reset", "uninstall"
    )

    /**
     * Evaluates whether an action can execute safely, requires user confirmation, or is blocked.
     */
    fun evaluate(action: AgentAction, isBatchPreApproved: Boolean = false): PolicyEvaluation {
        val targetDesc = action.target?.summary()?.lowercase() ?: ""
        val payload = action.payload?.lowercase() ?: ""
        val combined = "$targetDesc $payload ${action.reason.lowercase()}"

        // Rule 1: CRITICAL CREDENTIAL SAFETY - Strictly blocked
        for (kw in SENSITIVE_KEYWORDS) {
            if (combined.contains(kw) && (action.action == AgentActionType.TYPE_TEXT || action.action == AgentActionType.CLICK || action.action == AgentActionType.TAP)) {
                return PolicyEvaluation(
                    decision = PolicyDecision.BLOCKED,
                    reason = "Autonomous interaction with credentials/passwords/PINs ($kw) is strictly forbidden for security."
                )
            }
        }

        // Rule 2: Explicit confirmation flag in action
        if (action.requiresConfirmation && !isBatchPreApproved) {
            return PolicyEvaluation(
                decision = PolicyDecision.REQUIRES_CONFIRMATION,
                reason = "Action explicitly requested user confirmation before executing.",
                requiresExplicitUserApproval = true
            )
        }

        // Rule 3: Sending messages or modifying data
        for (kw in CONFIRMATION_ACTIONS) {
            val isSendAction = action.action == AgentActionType.SEND
            val isClickOrTap = action.action == AgentActionType.CLICK || action.action == AgentActionType.TAP
            if ((isClickOrTap && (targetDesc.contains(kw) || action.reason.lowercase().contains(kw))) || (isSendAction && kw == "send")) {
                if (isBatchPreApproved) {
                    return PolicyEvaluation(
                        decision = PolicyDecision.ALLOWED,
                        reason = "User pre-authorized batch operation (e.g. WhatsApp multi-group broadcast)."
                    )
                } else {
                    return PolicyEvaluation(
                        decision = PolicyDecision.REQUIRES_CONFIRMATION,
                        reason = "Safety check: High-impact action '$kw' detected (${action.reason.ifEmpty { targetDesc }}).",
                        requiresExplicitUserApproval = true
                    )
                }
            }
        }

        // Safe standard actions: navigation, search, reading
        return PolicyEvaluation(
            decision = PolicyDecision.ALLOWED,
            reason = "Action approved: Standard safe navigation and automation."
        )
    }
}
