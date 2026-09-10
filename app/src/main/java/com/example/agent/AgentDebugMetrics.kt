package com.example.agent

data class AgentDebugMetrics(
    val currentState: AgentState = AgentState.IDLE,
    val currentPackage: String = "com.example",
    val accessibilityNodeCount: Int = 0,
    val latestScreenshotTimestamp: Long = System.currentTimeMillis(),
    val aiLatencyMs: Long = 0L,
    val actionLatencyMs: Long = 0L,
    val verificationLatencyMs: Long = 0L,
    val totalActions: Int = 0,
    val failures: Int = 0,
    val recoveryAttempts: Int = 0,
    val confidence: Float = 0.95f
)
