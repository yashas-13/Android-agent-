package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhoneAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        captureCurrentHierarchy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        // Ignore our own package events for screen updates to keep target app in focus
        if (pkg != packageName) {
            _currentPackageName.value = pkg
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            // Refresh current hierarchy
            captureCurrentHierarchy()
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _isServiceConnected.value = false
        }
    }

    fun captureCurrentHierarchy(): ScreenSnapshot? {
        val root = rootInActiveWindow ?: return _latestSnapshot.value
        return try {
            val snapshot = AccessibilityNodeParser.parseTree(
                root = root,
                packageNameOverride = _currentPackageName.value.ifEmpty { root.packageName?.toString() }
            )
            _latestSnapshot.value = snapshot
            snapshot
        } catch (e: Exception) {
            _latestSnapshot.value
        }
    }

    suspend fun executeAction(action: AgentAction): Boolean {
        return when (action.action) {
            AgentActionType.CLICK, AgentActionType.TAP -> executeClick(action.target)
            AgentActionType.LONG_CLICK -> executeLongClick(action.target)
            AgentActionType.TYPE_TEXT -> executeTypeText(action.target, action.payload ?: "")
            AgentActionType.CLEAR_TEXT -> executeClearText(action.target)
            AgentActionType.SWIPE -> executeSwipeWithDirection(action.target, action.payload)
            AgentActionType.SCROLL -> executeScroll(action.target, action.payload)
            AgentActionType.NEXT -> executeNextField()
            AgentActionType.MINIMIZE -> executeMinimize()
            AgentActionType.SEND -> executeSend(action.target)
            AgentActionType.ENTER_KEY -> executeEnterKey(action.target)
            AgentActionType.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            AgentActionType.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            AgentActionType.OPEN_APP -> executeOpenApp(action.payload ?: "")
            AgentActionType.WAIT -> true
            AgentActionType.SCREENSHOT -> executeScreenshotCapture()
            AgentActionType.FIND_ELEMENT -> findMatchingNode(action.target) != null
        }
    }

    private suspend fun executeClick(target: ActionTarget?): Boolean {
        if (target == null) return false

        // Strategy 1: Find matching AccessibilityNodeInfo and perform node ACTION_CLICK
        val node = findMatchingNode(target)
        if (node != null) {
            var curr: AccessibilityNodeInfo? = node
            while (curr != null) {
                if (curr.isClickable) {
                    val clicked = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                curr = curr.parent
            }
        }

        // Strategy 2: Fallback to gesture coordinates
        val x = target.x ?: target.bounds?.centerX?.toFloat()
        val y = target.y ?: target.bounds?.centerY?.toFloat()
        if (x != null && y != null) {
            return GestureHelper.clickAt(this, x, y)
        }

        return false
    }

    private suspend fun executeLongClick(target: ActionTarget?): Boolean {
        if (target == null) return false

        val node = findMatchingNode(target)
        if (node != null && node.isLongClickable) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                return true
            }
        }

        val x = target.x ?: target.bounds?.centerX?.toFloat()
        val y = target.y ?: target.bounds?.centerY?.toFloat()
        if (x != null && y != null) {
            return GestureHelper.longClickAt(this, x, y)
        }
        return false
    }

    private fun executeTypeText(target: ActionTarget?, text: String): Boolean {
        var node = findMatchingNode(target)

        // If target not directly found or not editable, try finding currently focused or any editable field
        if (node == null || !node.isEditable) {
            node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
        if (node == null || !node.isEditable) {
            node = findFirstEditableNode(rootInActiveWindow)
        }

        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        return false
    }

    private fun executeClearText(target: ActionTarget?): Boolean {
        var node = findMatchingNode(target)
        if (node == null || !node.isEditable) {
            node = findFirstEditableNode(rootInActiveWindow)
        }
        if (node != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    private suspend fun executeSwipeWithDirection(target: ActionTarget?, payload: String?): Boolean {
        val dir = payload?.lowercase()?.trim() ?: "up"
        val b = target?.bounds

        val (startX, startY, endX, endY) = when (dir) {
            "down" -> {
                val x = b?.centerX?.toFloat() ?: 540f
                val y1 = b?.top?.toFloat() ?: 600f
                val y2 = b?.bottom?.toFloat() ?: 1600f
                arrayOf(x, y1, x, y2)
            }
            "left" -> {
                val y = b?.centerY?.toFloat() ?: 1000f
                val x1 = b?.right?.toFloat() ?: 900f
                val x2 = b?.left?.toFloat() ?: 180f
                arrayOf(x1, y, x2, y)
            }
            "right" -> {
                val y = b?.centerY?.toFloat() ?: 1000f
                val x1 = b?.left?.toFloat() ?: 180f
                val x2 = b?.right?.toFloat() ?: 900f
                arrayOf(x1, y, x2, y)
            }
            else -> { // "up" or default swipe up
                val x = b?.centerX?.toFloat() ?: 540f
                val y1 = (b?.bottom?.toFloat() ?: 1600f) - 50f
                val y2 = (b?.top?.toFloat() ?: 600f) + 50f
                arrayOf(x, y1, x, y2)
            }
        }

        return GestureHelper.swipe(this, startX, startY, endX, endY)
    }

    private fun executeNextField(): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val editables = mutableListOf<AccessibilityNodeInfo>()

        fun collect(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.isEditable && node.isVisibleToUser) editables.add(node)
            for (i in 0 until node.childCount) {
                collect(node.getChild(i))
            }
        }
        collect(root)

        if (editables.isNotEmpty()) {
            val currentIndex = editables.indexOfFirst { it == focused }
            val nextIndex = if (currentIndex in 0 until editables.lastIndex) currentIndex + 1 else 0
            return editables[nextIndex].performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        return focused?.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY) ?: false
    }

    private fun executeMinimize(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private suspend fun executeSend(target: ActionTarget?): Boolean {
        // 1. If explicit target found, click it
        if (target != null && executeClick(target)) {
            return true
        }

        // 2. Search hierarchy for Send button variants
        val root = rootInActiveWindow
        if (root != null) {
            val sendKeywords = listOf("send", "enviar", "envoyer", "submit", "post", "send message")
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val desc = current.contentDescription?.toString()?.lowercase() ?: ""
                val text = current.text?.toString()?.lowercase() ?: ""
                val id = current.viewIdResourceName?.lowercase() ?: ""

                if (sendKeywords.any { desc.contains(it) || text.contains(it) || id.contains(it) }) {
                    if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                    val parent = current.parent
                    if (parent != null && parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                }

                for (i in 0 until current.childCount) {
                    current.getChild(i)?.let { queue.add(it) }
                }
            }
        }

        // 3. Coordinate fallback for standard bottom-right send icon
        return GestureHelper.clickAt(this, 1000f, 2200f)
    }

    private suspend fun executeEnterKey(target: ActionTarget?): Boolean {
        // Fallback: click search or done if visible
        val targetNode = findMatchingNode(target)
        if (targetNode != null && targetNode.isClickable) {
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Tap bottom right of keyboard (standard Enter/Done location)
        return GestureHelper.clickAt(this, 1000f, 2250f)
    }

    private fun executeScreenshotCapture(): Boolean {
        captureCurrentHierarchy()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
                            // Screenshot successfully captured by system accessibility service
                        }
                        override fun onFailure(errorCode: Int) {
                            // Fallback hierarchy remains available
                        }
                    }
                )
            } catch (e: Exception) {
                // Keep hierarchy snapshot
            }
        }
        return true
    }

    private suspend fun executeScroll(target: ActionTarget?, direction: String?): Boolean {
        val node = findMatchingNode(target) ?: findFirstScrollableNode(rootInActiveWindow)
        if (node != null) {
            val action = if (direction?.lowercase() == "backward" || direction?.lowercase() == "up") {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            if (node.performAction(action)) return true
        }

        // Gesture fallback
        val startY = 1300f
        val endY = 600f
        return GestureHelper.swipe(this, 540f, startY, 540f, endY)
    }

    private fun executeOpenApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun findMatchingNode(target: ActionTarget?): AccessibilityNodeInfo? {
        if (target == null) return null
        val root = rootInActiveWindow ?: return null

        // Try text match
        if (!target.text.isNullOrBlank()) {
            val nodes = root.findAccessibilityNodeInfosByText(target.text)
            if (!nodes.isNullOrEmpty()) {
                return nodes.firstOrNull { it.isVisibleToUser } ?: nodes.first()
            }
        }

        // Try view resource id
        if (!target.resourceId.isNullOrBlank()) {
            val nodes = root.findAccessibilityNodeInfosByViewId(target.resourceId)
            if (!nodes.isNullOrEmpty()) {
                return nodes.firstOrNull { it.isVisibleToUser } ?: nodes.first()
            }
        }

        // Fallback: breadth-first search on contentDescription or text substring
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val text = current.text?.toString() ?: ""
            val desc = current.contentDescription?.toString() ?: ""

            if (!target.text.isNullOrBlank() && text.contains(target.text, ignoreCase = true)) {
                return current
            }
            if (!target.contentDescription.isNullOrBlank() && desc.contains(target.contentDescription, ignoreCase = true)) {
                return current
            }

            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }

        return null
    }

    private fun findFirstEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.isEditable && current.isVisibleToUser) {
                return current
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findFirstScrollableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.isScrollable && current.isVisibleToUser) {
                return current
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    companion object {
        @Volatile
        var instance: PhoneAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _latestSnapshot = MutableStateFlow<ScreenSnapshot?>(null)
        val latestSnapshot: StateFlow<ScreenSnapshot?> = _latestSnapshot.asStateFlow()

        private val _currentPackageName = MutableStateFlow("")
        val currentPackageName: StateFlow<String> = _currentPackageName.asStateFlow()
    }
}
