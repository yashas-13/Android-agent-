package com.example.agent

import android.os.SystemClock
import com.example.accessibility.PhoneAccessibilityService
import com.example.accessibility.ScreenSnapshot
import com.example.accessibility.UiElementInfo
import com.example.ai.AiProvider
import com.example.ai.CactusNeedleProvider
import com.example.ai.GeminiProvider
import com.example.ai.LocalModelProvider
import com.example.ai.MockProvider
import com.example.data.db.AuditLogEntity
import com.example.data.repository.AgentRepository
import com.example.policy.ActionPolicyEngine
import com.example.policy.PolicyDecision
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AgentController(
    private val repository: AgentRepository,
    private val scope: CoroutineScope
) {
    // Current AI Provider
    val cactusProvider = CactusNeedleProvider()
    val localProvider = LocalModelProvider()
    val geminiProvider = GeminiProvider()
    val mockProvider = MockProvider()

    private val _activeProvider = MutableStateFlow<AiProvider>(cactusProvider)
    val activeProvider: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    // Agent State Machine
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state.asStateFlow()
    val agentState: StateFlow<AgentState> get() = state

    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()

    private val _currentAction = MutableStateFlow<AgentAction?>(null)
    val currentAction: StateFlow<AgentAction?> = _currentAction.asStateFlow()

    private val _verificationResult = MutableStateFlow<String?>(null)
    val verificationResult: StateFlow<String?> = _verificationResult.asStateFlow()

    private val _groupDeliveryResults = MutableStateFlow<List<GroupDeliveryResult>>(emptyList())
    val groupDeliveryResults: StateFlow<List<GroupDeliveryResult>> = _groupDeliveryResults.asStateFlow()

    private var lastBroadcastGroups: List<String> = emptyList()
    private var lastBroadcastMessage: String = ""

    private val _actionHistory = MutableStateFlow<List<ExecutedAction>>(emptyList())
    val actionHistory: StateFlow<List<ExecutedAction>> = _actionHistory.asStateFlow()

    private val _liveEvents = MutableStateFlow<List<AgentEvent>>(emptyList())
    val liveEvents: StateFlow<List<AgentEvent>> = _liveEvents.asStateFlow()

    private val _pendingConfirmationAction = MutableStateFlow<AgentAction?>(null)
    val pendingConfirmationAction: StateFlow<AgentAction?> = _pendingConfirmationAction.asStateFlow()

    private val _latestSnapshot = MutableStateFlow<ScreenSnapshot?>(null)
    val latestSnapshot: StateFlow<ScreenSnapshot?> = _latestSnapshot.asStateFlow()

    private val _debugMetrics = MutableStateFlow(AgentDebugMetrics())
    val debugMetrics: StateFlow<AgentDebugMetrics> = _debugMetrics.asStateFlow()

    private var agentJob: Job? = null
    private var confirmationDeferred: CompletableDeferred<Boolean>? = null
    private val recovery = AgentRecovery()
    private var currentTaskId = ""
    private var isBatchPreApproved = false

    fun setProvider(provider: AiProvider) {
        _activeProvider.value = provider
        addEvent(
            AgentEventType.PLAN,
            "AI Provider Changed",
            "Active provider set to: ${provider.displayName}"
        )
    }

    fun startTask(
        taskDescription: String,
        preApprovedBatch: Boolean = false,
        whatsAppGroups: List<String> = emptyList(),
        whatsAppMessage: String = ""
    ) {
        stopAgent("Starting new task")

        currentTaskId = UUID.randomUUID().toString().take(8)
        _currentTask.value = taskDescription
        isBatchPreApproved = preApprovedBatch
        _actionHistory.value = emptyList()
        _liveEvents.value = emptyList()
        _currentAction.value = null
        recovery.reset()
        mockProvider.reset()

        if (whatsAppGroups.isNotEmpty()) {
            lastBroadcastGroups = whatsAppGroups
            lastBroadcastMessage = whatsAppMessage
            _groupDeliveryResults.value = whatsAppGroups.map {
                GroupDeliveryResult(groupName = it, status = DeliveryStatus.PENDING)
            }
            localProvider.initializeBroadcast(whatsAppGroups, whatsAppMessage)
        }

        addEvent(
            AgentEventType.PLAN,
            "Task Initiated",
            "Goal: \"$taskDescription\" (Batch Pre-Approved: $preApprovedBatch)"
        )

        agentJob = scope.launch(Dispatchers.Default) {
            runAgentLoop(taskDescription)
        }
    }

    fun startWhatsAppBroadcast(
        targetGroupNames: List<String>,
        message: String,
        isPreApproved: Boolean = true
    ) {
        startTask(
            taskDescription = "Share message to ${targetGroupNames.size} WhatsApp groups: ${targetGroupNames.joinToString(", ")}",
            preApprovedBatch = isPreApproved,
            whatsAppGroups = targetGroupNames,
            whatsAppMessage = message
        )
    }

    fun retryFailedGroups() {
        val failed = _groupDeliveryResults.value.filter { it.status == DeliveryStatus.FAILED }.map { it.groupName }
        if (failed.isNotEmpty()) {
            startWhatsAppBroadcast(failed, lastBroadcastMessage, isPreApproved = isBatchPreApproved)
        }
    }

    fun generateDiagnosticReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== AI PHONE AGENT DIAGNOSTIC REPORT ===")
        sb.appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine("App Version: 1.0.0 (Production)")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Accessibility Service Connected: ${PhoneAccessibilityService.isServiceConnected.value}")
        sb.appendLine("Active AI Provider: ${_activeProvider.value.displayName}")
        sb.appendLine("Task: ${_currentTask.value}")
        sb.appendLine("State: ${_state.value}")
        sb.appendLine("Last Verification: ${_verificationResult.value ?: "N/A"}")
        sb.appendLine()
        sb.appendLine("--- GROUP DELIVERY RESULTS ---")
        if (_groupDeliveryResults.value.isEmpty()) {
            sb.appendLine("No broadcast group results recorded.")
        } else {
            _groupDeliveryResults.value.forEach { res ->
                sb.appendLine("• ${res.groupName}: [${res.status}] ${res.failureReason?.let { "-> Reason: $it" } ?: ""}")
            }
        }
        sb.appendLine()
        sb.appendLine("--- RECENT ACTION HISTORY ---")
        _actionHistory.value.takeLast(10).forEach { item ->
            sb.appendLine("[${item.status}] ${item.action.action} -> ${item.action.target?.summary()} (${item.resultMessage}) Latency: ${item.latencyMs}ms")
        }
        sb.appendLine()
        sb.appendLine("--- TELEMETRY METRICS ---")
        val m = _debugMetrics.value
        sb.appendLine("Total Actions: ${m.totalActions}, Failures: ${m.failures}, Recoveries: ${m.recoveryAttempts}")
        sb.appendLine("AI Latency: ${m.aiLatencyMs}ms, Action Latency: ${m.actionLatencyMs}ms")
        sb.appendLine("=========================================")
        return sb.toString()
    }

    fun stopAgent(reason: String = "User requested stop") {
        if (agentJob?.isActive == true) {
            agentJob?.cancel()
            agentJob = null
        }
        confirmationDeferred?.complete(false)
        confirmationDeferred = null
        _pendingConfirmationAction.value = null
        _currentAction.value = null
        _state.value = AgentState.STOPPED
        addEvent(AgentEventType.ERROR, "Agent Stopped", reason)
        updateMetrics { copy(currentState = AgentState.STOPPED) }
    }

    fun emergencyStop() {
        stopAgent("EMERGENCY STOP TRIGGERED")
        scope.launch(Dispatchers.IO) {
            repository.recordAuditLog(
                AuditLogEntity(
                    taskId = currentTaskId,
                    taskDescription = _currentTask.value,
                    actionType = "EMERGENCY_STOP",
                    targetDescription = "System Kill Switch",
                    packageName = _latestSnapshot.value?.packageName ?: "system",
                    status = "EMERGENCY_STOP",
                    reason = "User invoked immediate emergency stop protocol.",
                    confidence = 1.0f
                )
            )
        }
    }

    fun approvePendingConfirmation() {
        val action = _pendingConfirmationAction.value
        _pendingConfirmationAction.value = null
        addEvent(
            AgentEventType.CONFIRM,
            "User Approved Action",
            "Proceeding with: ${action?.action} -> ${action?.target?.summary()}"
        )
        confirmationDeferred?.complete(true)
    }

    fun rejectPendingConfirmation() {
        val action = _pendingConfirmationAction.value
        _pendingConfirmationAction.value = null
        addEvent(
            AgentEventType.CONFIRM,
            "User Rejected Action",
            "Halted execution of: ${action?.action} -> ${action?.target?.summary()}"
        )
        confirmationDeferred?.complete(false)
        stopAgent("User rejected action confirmation")
    }

    fun executeDirectAction(action: AgentAction, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch(Dispatchers.Default) {
            _state.value = AgentState.EXECUTING
            addEvent(
                AgentEventType.ACT,
                "Direct Action: ${action.action}",
                "Executing ${action.action} -> ${action.target?.summary() ?: action.payload ?: "system"}"
            )
            val service = PhoneAccessibilityService.instance
            val success = if (service != null && PhoneAccessibilityService.isServiceConnected.value) {
                service.executeAction(action)
            } else {
                delay(250)
                true
            }
            addEvent(
                if (success) AgentEventType.COMPLETE else AgentEventType.ERROR,
                "Direct Action ${if (success) "Completed" else "Failed"}",
                "${action.action} finished with status: $success"
            )
            _state.value = AgentState.IDLE
            onComplete?.invoke(success)
        }
    }

    private suspend fun runAgentLoop(task: String) {
        var actionCount = 0
        val maxActions = ActionPolicyEngine.DEFAULT_MAX_ACTIONS_PER_TASK

        while (actionCount < maxActions) {
            actionCount++

            // --- 1. OBSERVE ---
            _state.value = AgentState.OBSERVING
            updateMetrics { copy(currentState = AgentState.OBSERVING) }
            delay(400) // Stabilize frame

            val snapshot = captureScreen()
            _latestSnapshot.value = snapshot
            addEvent(
                AgentEventType.OBSERVE,
                "Screen Captured",
                "Package: ${snapshot.packageName.ifEmpty { "system" }} | Elements: ${snapshot.totalCount} total (${snapshot.interactiveCount} interactive)",
                badge = "${snapshot.interactiveCount} items"
            )

            // --- 2. UNDERSTAND ---
            _state.value = AgentState.UNDERSTANDING
            updateMetrics {
                copy(
                    currentState = AgentState.UNDERSTANDING,
                    currentPackage = snapshot.packageName,
                    accessibilityNodeCount = snapshot.totalCount,
                    latestScreenshotTimestamp = snapshot.timestamp
                )
            }
            delay(150)

            addEvent(
                AgentEventType.UNDERSTAND,
                "UI Hierarchy Understood",
                "Analyzed layout structure; identified ${snapshot.interactiveCount} actionable nodes."
            )

            // --- 3. PLAN ---
            _state.value = AgentState.PLANNING
            updateMetrics { copy(currentState = AgentState.PLANNING) }

            val planStartTime = SystemClock.elapsedRealtime()
            val plan = _activeProvider.value.planNextAction(task, snapshot, _actionHistory.value)
            val planDuration = SystemClock.elapsedRealtime() - planStartTime
            updateMetrics { copy(aiLatencyMs = planDuration, confidence = plan.confidence) }

            if (plan.isTaskComplete || plan.action == null) {
                _state.value = AgentState.SUCCESS
                val completeMsg = plan.completionMessage ?: "All goals achieved."
                _verificationResult.value = completeMsg
                // Mark any pending groups as successful
                _groupDeliveryResults.value = _groupDeliveryResults.value.map {
                    if (it.status == DeliveryStatus.PENDING || it.status == DeliveryStatus.SENDING) {
                        it.copy(status = DeliveryStatus.SUCCESS)
                    } else it
                }
                addEvent(
                    AgentEventType.COMPLETE,
                    "Task Completed Successfully",
                    completeMsg,
                    badge = "SUCCESS"
                )
                recordAudit(
                    actionType = "COMPLETE",
                    target = "Task Goal",
                    status = "SUCCESS",
                    reason = completeMsg,
                    confidence = plan.confidence,
                    latencyMs = planDuration
                )
                updateMetrics { copy(currentState = AgentState.SUCCESS) }
                return
            }

            val action = plan.action
            _currentAction.value = action
            addEvent(
                AgentEventType.PLAN,
                "Action Decided",
                "${action.action} → ${action.target?.summary()} (${action.reason})",
                badge = "${(action.confidence * 100).toInt()}% conf"
            )

            // --- 4. SAFETY & POLICY EVALUATION ---
            val policyResult = ActionPolicyEngine.evaluate(action, isBatchPreApproved)
            if (policyResult.decision == PolicyDecision.BLOCKED) {
                _state.value = AgentState.FAILED
                _verificationResult.value = policyResult.reason
                _groupDeliveryResults.value = _groupDeliveryResults.value.map {
                    if (it.status == DeliveryStatus.PENDING || it.status == DeliveryStatus.SENDING) {
                        it.copy(status = DeliveryStatus.FAILED, failureReason = policyResult.reason)
                    } else it
                }
                addEvent(
                    AgentEventType.ERROR,
                    "Action Blocked by Safety Policy",
                    policyResult.reason,
                    badge = "POLICY BLOCK"
                )
                recordAudit(
                    actionType = action.action.name,
                    target = action.target?.summary() ?: "",
                    status = "BLOCKED_BY_POLICY",
                    reason = policyResult.reason,
                    confidence = action.confidence
                )
                updateMetrics { copy(currentState = AgentState.FAILED, failures = failures + 1) }
                return
            }

            if (policyResult.decision == PolicyDecision.REQUIRES_CONFIRMATION) {
                _state.value = AgentState.WAITING_FOR_CONFIRMATION
                updateMetrics { copy(currentState = AgentState.WAITING_FOR_CONFIRMATION) }
                _pendingConfirmationAction.value = action
                addEvent(
                    AgentEventType.CONFIRM,
                    "Confirmation Required",
                    policyResult.reason,
                    badge = "AWAITING APPROVAL"
                )

                val deferred = CompletableDeferred<Boolean>()
                confirmationDeferred = deferred
                val approved = deferred.await()
                confirmationDeferred = null

                if (!approved) {
                    _state.value = AgentState.STOPPED
                    return
                }
            }

            // --- 5. ACT ---
            _state.value = AgentState.EXECUTING
            updateMetrics { copy(currentState = AgentState.EXECUTING) }

            val actStartTime = SystemClock.elapsedRealtime()
            val service = PhoneAccessibilityService.instance
            val executionSuccess: Boolean

            if (service != null && PhoneAccessibilityService.isServiceConnected.value) {
                executionSuccess = service.executeAction(action)
            } else {
                // In demo / test / ungranted permission mode, perform simulated execution
                delay(300)
                executionSuccess = true
            }

            val actDuration = SystemClock.elapsedRealtime() - actStartTime
            updateMetrics {
                copy(
                    actionLatencyMs = actDuration,
                    totalActions = totalActions + 1
                )
            }

            addEvent(
                AgentEventType.ACT,
                "Executed ${action.action}",
                "Target: ${action.target?.summary()} | Latency: ${actDuration}ms",
                badge = if (executionSuccess) "DISPATCHED" else "FAILED"
            )

            // --- 6. VERIFY ---
            _state.value = AgentState.VERIFYING
            updateMetrics { copy(currentState = AgentState.VERIFYING) }
            delay(500) // Allow UI transition to settle

            val postSnapshot = captureScreen()
            val verifyStartTime = SystemClock.elapsedRealtime()
            val verification = _activeProvider.value.verifyOutcome(task, action, snapshot, postSnapshot)
            val verifyDuration = SystemClock.elapsedRealtime() - verifyStartTime
            updateMetrics { copy(verificationLatencyMs = verifyDuration) }

            val status = if (verification.isSuccess) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED

            val executedAction = ExecutedAction(
                action = action,
                status = status,
                resultMessage = verification.explanation,
                latencyMs = actDuration + verifyDuration
            )
            _actionHistory.value = _actionHistory.value + executedAction

            recordAudit(
                actionType = action.action.name,
                target = action.target?.summary() ?: "",
                status = status.name,
                reason = action.reason,
                confidence = action.confidence,
                latencyMs = actDuration + verifyDuration
            )

            addEvent(
                AgentEventType.VERIFY,
                if (verification.isSuccess) "Verification Passed" else "Verification Warning",
                verification.explanation,
                badge = if (verification.isSuccess) "VERIFIED" else "UNVERIFIED"
            )

            // --- 7. RECOVERY & REPLANNING IF NEEDED ---
            if (!verification.isSuccess) {
                _state.value = AgentState.RECOVERING
                updateMetrics { copy(currentState = AgentState.RECOVERING, recoveryAttempts = recoveryAttempts + 1) }

                val strategy = recovery.determineRecovery(action, postSnapshot, verification.explanation)
                when (strategy) {
                    is RecoveryStrategy.ScrollAndRetry -> {
                        addEvent(AgentEventType.RECOVER, "Recovery: Scroll", "Scrolling to reveal off-screen targets.")
                        service?.executeAction(AgentAction(AgentActionType.SCROLL, payload = strategy.direction))
                        delay(600)
                    }
                    is RecoveryStrategy.VisionCoordinateFallback -> {
                        addEvent(AgentEventType.RECOVER, "Recovery: Vision Coordinates", "Falling back to estimated visual touch coordinates.")
                        service?.executeAction(AgentAction(AgentActionType.CLICK, target = strategy.target))
                        delay(600)
                    }
                    is RecoveryStrategy.DismissKeyboardOrDialog -> {
                        addEvent(AgentEventType.RECOVER, "Recovery: Dismiss Keyboard", "Dismissing software keyboard to clear viewport.")
                        service?.executeAction(strategy.action)
                        delay(400)
                    }
                    is RecoveryStrategy.Replan -> {
                        addEvent(AgentEventType.RECOVER, "Recovery: Replanning", strategy.reason)
                    }
                    is RecoveryStrategy.AbortSafely -> {
                        _state.value = AgentState.FAILED
                        val failMsg = "Recovery limit exceeded: ${verification.explanation}"
                        _verificationResult.value = failMsg
                        _groupDeliveryResults.value = _groupDeliveryResults.value.map {
                            if (it.status == DeliveryStatus.PENDING || it.status == DeliveryStatus.SENDING) {
                                it.copy(status = DeliveryStatus.FAILED, failureReason = failMsg)
                            } else it
                        }
                        addEvent(AgentEventType.ERROR, "Recovery Limit Exceeded", failMsg)
                        updateMetrics { copy(currentState = AgentState.FAILED, failures = failures + 1) }
                        return
                    }
                }
            }
        }

        // Loop finished without explicit complete
        _state.value = AgentState.SUCCESS
        addEvent(AgentEventType.COMPLETE, "Task Finished", "Completed action limit.")
    }

    private fun captureScreen(): ScreenSnapshot {
        val service = PhoneAccessibilityService.instance
        val serviceSnapshot = service?.captureCurrentHierarchy()
        if (serviceSnapshot != null && serviceSnapshot.elements.isNotEmpty()) {
            return serviceSnapshot
        }

        // Realistic Simulated Snapshot if AccessibilityService not connected on emulator
        val isWhatsApp = _currentTask.value.lowercase().contains("whatsapp") || _currentTask.value.lowercase().contains("group")
        val pkg = if (isWhatsApp) "com.whatsapp" else "com.android.settings"

        val elements = if (isWhatsApp) {
            listOf(
                UiElementInfo(id = "search_icon", text = "Search", role = "button", bounds = com.example.agent.ElementBounds(900, 100, 1020, 220), isClickable = true),
                UiElementInfo(id = "chat_1", text = "Product Team", contentDescription = "Product Team group", role = "button", bounds = com.example.agent.ElementBounds(40, 240, 1040, 420), isClickable = true),
                UiElementInfo(id = "chat_2", text = "Family & Friends", contentDescription = "Family group", role = "button", bounds = com.example.agent.ElementBounds(40, 440, 1040, 620), isClickable = true),
                UiElementInfo(id = "chat_3", text = "Developers Hub", contentDescription = "Developers Hub group", role = "button", bounds = com.example.agent.ElementBounds(40, 640, 1040, 820), isClickable = true),
                UiElementInfo(id = "msg_input", text = "Type a message", role = "input", bounds = com.example.agent.ElementBounds(40, 2200, 900, 2340), isClickable = true, isEditable = true),
                UiElementInfo(id = "send_btn", text = "Send", contentDescription = "Send", role = "button", bounds = com.example.agent.ElementBounds(920, 2200, 1040, 2340), isClickable = true)
            )
        } else {
            listOf(
                UiElementInfo(id = "wifi_row", text = "Network & internet", role = "button", bounds = com.example.agent.ElementBounds(40, 300, 1040, 460), isClickable = true),
                UiElementInfo(id = "wifi_switch", text = "Wi-Fi", role = "switch", bounds = com.example.agent.ElementBounds(880, 500, 1020, 620), isClickable = true)
            )
        }

        return ScreenSnapshot(
            packageName = pkg,
            elements = elements
        )
    }

    private fun addEvent(type: AgentEventType, title: String, details: String = "", badge: String? = null) {
        val event = AgentEvent(
            type = type,
            title = title,
            details = details,
            badge = badge
        )
        _liveEvents.value = _liveEvents.value + event
    }

    private fun recordAudit(
        actionType: String,
        target: String,
        status: String,
        reason: String,
        confidence: Float,
        latencyMs: Long = 0L
    ) {
        scope.launch(Dispatchers.IO) {
            repository.recordAuditLog(
                AuditLogEntity(
                    taskId = currentTaskId,
                    taskDescription = _currentTask.value,
                    actionType = actionType,
                    targetDescription = target,
                    packageName = _latestSnapshot.value?.packageName ?: "system",
                    status = status,
                    reason = reason,
                    confidence = confidence,
                    latencyMs = latencyMs
                )
            )
        }
    }

    private inline fun updateMetrics(block: AgentDebugMetrics.() -> AgentDebugMetrics) {
        _debugMetrics.value = _debugMetrics.value.block()
    }
}
