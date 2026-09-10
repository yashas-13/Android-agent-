package com.example.accessibility

import com.example.agent.ElementBounds

data class UiElementInfo(
    val id: String,
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val role: String = "view",
    val bounds: ElementBounds,
    val isClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val isFocused: Boolean = false,
    val isSelected: Boolean = false,
    val packageName: String? = null,
    val depth: Int = 0
) {
    val displayName: String
        get() = when {
            !text.isNullOrBlank() -> text
            !contentDescription.isNullOrBlank() -> contentDescription
            !resourceId.isNullOrBlank() -> resourceId.substringAfterLast('/')
            else -> "$role@(${bounds.centerX},${bounds.centerY})"
        }

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return (text?.lowercase()?.contains(q) == true) ||
                (contentDescription?.lowercase()?.contains(q) == true) ||
                (resourceId?.lowercase()?.contains(q) == true)
    }
}

data class ScreenSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val windowTitle: String = "",
    val elements: List<UiElementInfo> = emptyList(),
    val isKeyboardVisible: Boolean = false,
    val screenshotBase64: String? = null
) {
    val interactiveElements: List<UiElementInfo>
        get() = elements.filter { it.isClickable || it.isEditable || it.isScrollable }

    val interactiveCount: Int
        get() = interactiveElements.size

    val totalCount: Int
        get() = elements.size

    /**
     * Formats a token-efficient semantic hierarchy for AI reasoning.
     */
    fun toCompactText(): String {
        val sb = StringBuilder()
        sb.appendLine("APPLICATION: ${packageName.ifEmpty { "Unknown" }}")
        if (windowTitle.isNotBlank()) sb.appendLine("WINDOW: $windowTitle")
        sb.appendLine("KEYBOARD_VISIBLE: $isKeyboardVisible")
        sb.appendLine("ELEMENTS (${interactiveElements.size} interactive):")

        interactiveElements.take(40).forEachIndexed { idx, item ->
            val label = when {
                !item.text.isNullOrBlank() -> "\"${item.text}\""
                !item.contentDescription.isNullOrBlank() -> "desc=\"${item.contentDescription}\""
                else -> ""
            }
            val idStr = item.resourceId?.substringAfterLast('/')?.let { "id=$it" } ?: ""
            val b = item.bounds
            sb.appendLine(" [$idx] role=${item.role} $label $idStr bounds=(${b.left},${b.top},${b.right},${b.bottom}) clickable=${item.isClickable} editable=${item.isEditable}")
        }
        return sb.toString()
    }
}
