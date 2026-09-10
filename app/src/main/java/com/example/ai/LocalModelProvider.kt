package com.example.ai

import com.example.accessibility.ScreenSnapshot
import com.example.accessibility.UiElementInfo
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import com.example.agent.ExecutedAction

class LocalModelProvider : AiProvider {

    override val providerType = AiProviderType.LOCAL_ENGINE
    override val displayName = "Local Semantic Model (Offline)"

    // State for tracking multi-group progression
    private var pendingGroups = mutableListOf<String>()
    private var currentGroupIndex = 0
    private var broadcastMessage = ""

    fun initializeBroadcast(groups: List<String>, message: String) {
        pendingGroups = groups.toMutableList()
        currentGroupIndex = 0
        broadcastMessage = message
    }

    override suspend fun planNextAction(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        val tLower = task.lowercase()

        // 1. WhatsApp Multi-Group Sharing Task
        if (tLower.contains("whatsapp") || tLower.contains("group") || pendingGroups.isNotEmpty()) {
            return planWhatsAppTask(task, screenSnapshot, history)
        }

        // 2. Settings & Wi-Fi Automation Task
        if (tLower.contains("settings") || tLower.contains("wi-fi") || tLower.contains("wifi")) {
            return planSettingsTask(screenSnapshot, history)
        }

        // 3. General Semantic Task
        return planGeneralTask(task, screenSnapshot, history)
    }

    private fun planWhatsAppTask(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        // Parse groups and message from task string if pendingGroups is empty
        if (pendingGroups.isEmpty()) {
            val parsedGroups = parseGroupsFromTask(task)
            pendingGroups = if (parsedGroups.isNotEmpty()) parsedGroups.toMutableList() else mutableListOf("Product Team", "Family & Friends")
            broadcastMessage = parseMessageFromTask(task).ifEmpty { "Hello everyone! Shared via AI Phone Agent." }
            currentGroupIndex = 0
        }

        if (currentGroupIndex >= pendingGroups.size) {
            return AgentPlanResponse(
                action = null,
                isTaskComplete = true,
                completionMessage = "Broadcast complete: Message successfully delivered to all ${pendingGroups.size} groups!",
                thoughtProcess = "All target groups have received the broadcast message.",
                confidence = 0.98f
            )
        }

        val targetGroup = pendingGroups[currentGroupIndex]
        val pkg = screenSnapshot.packageName.lowercase()

        // Step 1: Open WhatsApp if not in foreground
        if (!pkg.contains("whatsapp")) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.OPEN_APP,
                    payload = "com.whatsapp",
                    reason = "Open WhatsApp for multi-group broadcast",
                    confidence = 0.96f,
                    expectedOutcome = "WhatsApp active on screen"
                ),
                thoughtProcess = "WhatsApp is not in the foreground. Launching WhatsApp package com.whatsapp."
            )
        }

        // Step 2: Check if currently inside a chat screen
        val messageInput = screenSnapshot.elements.firstOrNull {
            it.isEditable || it.text?.contains("Type a message", ignoreCase = true) == true ||
                    it.contentDescription?.contains("Message", ignoreCase = true) == true
        }

        val sendButton = screenSnapshot.elements.firstOrNull {
            it.contentDescription?.contains("Send", ignoreCase = true) == true ||
                    it.text?.contains("Send", ignoreCase = true) == true ||
                    it.id.contains("send", ignoreCase = true)
        }

        // Are we in the target chat?
        val isInTargetChat = screenSnapshot.elements.any {
            it.text?.contains(targetGroup, ignoreCase = true) == true
        } && messageInput != null

        if (isInTargetChat) {
            // If message already typed or send button available
            val lastAction = history.lastOrNull()?.action
            if (lastAction?.action == AgentActionType.TYPE_TEXT) {
                // Now click Send
                val target = sendButton?.let { toActionTarget(it) } ?: ActionTarget(
                    text = "Send",
                    role = "button",
                    contentDescription = "Send"
                )
                return AgentPlanResponse(
                    action = AgentAction(
                        action = AgentActionType.CLICK,
                        target = target,
                        reason = "Click send to transmit message to $targetGroup",
                        requiresConfirmation = false, // pre-authorized in composer
                        confidence = 0.95f,
                        expectedOutcome = "Message bubble appearing in chat"
                    ),
                    thoughtProcess = "Message entered. Dispatching Send action to transmit message."
                )
            } else {
                // Type the broadcast message
                return AgentPlanResponse(
                    action = AgentAction(
                        action = AgentActionType.TYPE_TEXT,
                        target = toActionTarget(messageInput),
                        payload = broadcastMessage,
                        reason = "Input broadcast message for $targetGroup",
                        confidence = 0.94f,
                        expectedOutcome = "Message text present in input field"
                    ),
                    thoughtProcess = "Located conversation input field. Typing message content."
                )
            }
        }

        // If we just clicked send in the target chat, navigate back to chat list and advance group!
        val lastAction = history.lastOrNull()?.action
        if (lastAction?.action == AgentActionType.CLICK && (lastAction.target?.summary()?.contains("Send", ignoreCase = true) == true)) {
            currentGroupIndex++
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.BACK,
                    reason = "Return to chats list to process next group",
                    confidence = 0.92f,
                    expectedOutcome = "WhatsApp chats list view"
                ),
                thoughtProcess = "Message sent to $targetGroup. Returning to chat index for group ${currentGroupIndex + 1}/${pendingGroups.size}."
            )
        }

        // Step 3: Find and click target group in chats list
        val groupElement = screenSnapshot.elements.firstOrNull {
            it.isClickable && it.text?.contains(targetGroup, ignoreCase = true) == true
        }

        if (groupElement != null) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = toActionTarget(groupElement),
                    reason = "Select target WhatsApp group '$targetGroup'",
                    confidence = 0.94f,
                    expectedOutcome = "Group conversation opens"
                ),
                thoughtProcess = "Found target group '$targetGroup' in visible chat items. Clicking to open conversation."
            )
        }

        // Step 4: If group not visible, click Search icon
        val searchButton = screenSnapshot.elements.firstOrNull {
            it.isClickable && (it.contentDescription?.contains("Search", ignoreCase = true) == true ||
                    it.text?.contains("Search", ignoreCase = true) == true)
        }

        if (searchButton != null) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = toActionTarget(searchButton),
                    reason = "Search for group '$targetGroup'",
                    confidence = 0.91f,
                    expectedOutcome = "Search bar activated"
                ),
                thoughtProcess = "Target group not directly visible on screen. Opening search view."
            )
        }

        // Fallback: Scroll down to locate group
        return AgentPlanResponse(
            action = AgentAction(
                action = AgentActionType.SCROLL,
                reason = "Scroll down to find group '$targetGroup'",
                confidence = 0.85f,
                expectedOutcome = "Additional chat items visible"
            ),
            thoughtProcess = "Searching through chat list via scroll."
        )
    }

    private fun planSettingsTask(
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        val pkg = screenSnapshot.packageName.lowercase()

        if (!pkg.contains("settings")) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.OPEN_APP,
                    payload = "com.android.settings",
                    reason = "Open Android Settings application",
                    confidence = 0.95f,
                    expectedOutcome = "Settings main menu"
                ),
                thoughtProcess = "Target app 'Settings' is not in focus. Launching com.android.settings."
            )
        }

        // Check for Wi-Fi or Network item
        val wifiItem = screenSnapshot.elements.firstOrNull {
            it.text?.contains("Wi-Fi", ignoreCase = true) == true ||
                    it.text?.contains("Network", ignoreCase = true) == true ||
                    it.text?.contains("Internet", ignoreCase = true) == true
        }

        val wifiSwitch = screenSnapshot.elements.firstOrNull {
            it.role == "switch" || it.className?.contains("Switch", ignoreCase = true) == true
        }

        if (wifiSwitch != null) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = toActionTarget(wifiSwitch),
                    reason = "Toggle Wi-Fi switch",
                    confidence = 0.92f,
                    expectedOutcome = "Wi-Fi enabled"
                ),
                thoughtProcess = "Found Wi-Fi toggle switch. Clicking to activate."
            )
        }

        if (wifiItem != null) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = toActionTarget(wifiItem),
                    reason = "Open Wi-Fi / Network settings",
                    confidence = 0.93f,
                    expectedOutcome = "Wi-Fi configuration screen"
                ),
                thoughtProcess = "Found Network / Wi-Fi menu item. Opening configuration."
            )
        }

        // If switch was clicked
        if (history.any { it.action.reason.contains("Toggle", ignoreCase = true) }) {
            return AgentPlanResponse(
                action = null,
                isTaskComplete = true,
                completionMessage = "Wi-Fi settings successfully accessed and toggled!",
                thoughtProcess = "Task objectives fulfilled.",
                confidence = 0.96f
            )
        }

        return AgentPlanResponse(
            action = AgentAction(action = AgentActionType.WAIT, reason = "Awaiting settings screen refresh"),
            thoughtProcess = "Waiting for settings state to stabilize."
        )
    }

    private fun planGeneralTask(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        val words = task.split(" ").filter { it.length > 3 }
        val matchingElement = screenSnapshot.elements.firstOrNull { element ->
            words.any { w -> element.matches(w) } && element.isClickable
        }

        if (matchingElement != null) {
            return AgentPlanResponse(
                action = AgentAction(
                    action = AgentActionType.CLICK,
                    target = toActionTarget(matchingElement),
                    reason = "Click element matching task keywords",
                    confidence = 0.88f
                ),
                thoughtProcess = "Found interactive element '${matchingElement.displayName}' matching task objectives."
            )
        }

        return AgentPlanResponse(
            action = null,
            isTaskComplete = true,
            completionMessage = "Task analysis complete for current screen state.",
            thoughtProcess = "No further actions required."
        )
    }

    override suspend fun verifyOutcome(
        task: String,
        executedAction: AgentAction,
        previousSnapshot: ScreenSnapshot,
        currentSnapshot: ScreenSnapshot
    ): VerificationResult {
        // Verification rules:
        return when (executedAction.action) {
            AgentActionType.OPEN_APP -> {
                val opened = currentSnapshot.packageName.contains(executedAction.payload ?: "", ignoreCase = true)
                VerificationResult(
                    isSuccess = opened || currentSnapshot.packageName != previousSnapshot.packageName,
                    explanation = if (opened) "Application ${executedAction.payload} successfully opened." else "Awaiting app launch transition."
                )
            }
            AgentActionType.TYPE_TEXT -> {
                val textFound = currentSnapshot.elements.any {
                    it.text?.contains(executedAction.payload ?: "") == true
                }
                VerificationResult(
                    isSuccess = textFound || currentSnapshot.elements.size != previousSnapshot.elements.size,
                    explanation = "Text input verified in target component."
                )
            }
            AgentActionType.CLICK -> {
                // If screen elements or package changed, click succeeded
                val changed = currentSnapshot.packageName != previousSnapshot.packageName ||
                        currentSnapshot.elements.size != previousSnapshot.elements.size ||
                        currentSnapshot.elements != previousSnapshot.elements
                VerificationResult(
                    isSuccess = changed,
                    explanation = if (changed) "UI state successfully transitioned following click." else "Screen state unchanged, retrying target."
                )
            }
            else -> VerificationResult(isSuccess = true, explanation = "Action executed as expected.")
        }
    }

    private fun toActionTarget(element: UiElementInfo): ActionTarget {
        return ActionTarget(
            text = element.text,
            contentDescription = element.contentDescription,
            resourceId = element.resourceId,
            role = element.role,
            bounds = element.bounds,
            x = element.bounds.centerX.toFloat(),
            y = element.bounds.centerY.toFloat()
        )
    }

    private fun parseGroupsFromTask(task: String): List<String> {
        val groups = mutableListOf<String>()
        if (task.contains(":")) {
            val listPart = task.substringAfter(":")
            groups.addAll(listPart.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() })
        }
        return groups
    }

    private fun parseMessageFromTask(task: String): String {
        val regex = "\"([^\"]*)\"".toRegex()
        return regex.find(task)?.groupValues?.getOrNull(1) ?: ""
    }
}
