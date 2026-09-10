package com.example.agent

enum class AgentActionType {
    CLICK,
    TAP,
    LONG_CLICK,
    TYPE_TEXT,
    CLEAR_TEXT,
    SWIPE,
    SCROLL,
    NEXT,
    MINIMIZE,
    SEND,
    ENTER_KEY,
    BACK,
    HOME,
    OPEN_APP,
    WAIT,
    SCREENSHOT,
    FIND_ELEMENT
}

data class ElementBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class ActionTarget(
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val role: String? = null,
    val bounds: ElementBounds? = null,
    val x: Float? = null,
    val y: Float? = null
) {
    fun summary(): String {
        return when {
            !text.isNullOrBlank() -> "\"$text\" (${role ?: "element"})"
            !contentDescription.isNullOrBlank() -> "[$contentDescription]"
            !resourceId.isNullOrBlank() -> "id:${resourceId.substringAfterLast('/')}"
            x != null && y != null -> "coord:(${x.toInt()}, ${y.toInt()})"
            else -> "generic target"
        }
    }
}

data class AgentAction(
    val action: AgentActionType,
    val target: ActionTarget? = null,
    val payload: String? = null, // e.g. text to type or packageName to open
    val reason: String = "",
    val confidence: Float = 0.95f,
    val requiresConfirmation: Boolean = false,
    val expectedOutcome: String? = null
) {
    fun isValid(): Boolean {
        // Enforce action schema constraints
        return when (action) {
            AgentActionType.TYPE_TEXT -> !payload.isNullOrEmpty()
            AgentActionType.OPEN_APP -> !payload.isNullOrBlank()
            AgentActionType.CLICK, AgentActionType.TAP, AgentActionType.LONG_CLICK ->
                target != null || (payload != null)
            AgentActionType.BACK, AgentActionType.HOME, AgentActionType.WAIT, AgentActionType.SCREENSHOT,
            AgentActionType.MINIMIZE, AgentActionType.NEXT, AgentActionType.ENTER_KEY, AgentActionType.SEND -> true
            AgentActionType.SWIPE, AgentActionType.SCROLL -> true
            AgentActionType.FIND_ELEMENT, AgentActionType.CLEAR_TEXT -> true
        }
    }
}

data class ExecutedAction(
    val action: AgentAction,
    val status: ExecutionStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val resultMessage: String = "",
    val latencyMs: Long = 0L
)

enum class ExecutionStatus {
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED,
    BLOCKED_BY_POLICY,
    CONFIRMED_BY_USER
}
