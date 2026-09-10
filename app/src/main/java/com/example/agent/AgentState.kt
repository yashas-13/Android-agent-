package com.example.agent

enum class AgentState {
    IDLE,
    OBSERVING,
    UNDERSTANDING,
    PLANNING,
    WAITING_FOR_CONFIRMATION,
    EXECUTING,
    VERIFYING,
    RECOVERING,
    SUCCESS,
    FAILED,
    STOPPED
}

enum class AgentEventType {
    OBSERVE,
    UNDERSTAND,
    PLAN,
    ACT,
    VERIFY,
    RECOVER,
    CONFIRM,
    COMPLETE,
    ERROR
}

data class AgentEvent(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: AgentEventType,
    val title: String,
    val details: String = "",
    val badge: String? = null
)
