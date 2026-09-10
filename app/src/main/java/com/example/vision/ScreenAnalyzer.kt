package com.example.vision

import com.example.agent.ActionTarget
import com.example.agent.ElementBounds

data class VisualElement(
    val label: String,
    val role: String,
    val bounds: ElementBounds,
    val confidence: Float
) {
    fun toActionTarget(): ActionTarget {
        return ActionTarget(
            text = label,
            role = role,
            bounds = bounds,
            x = bounds.centerX.toFloat(),
            y = bounds.centerY.toFloat()
        )
    }
}

object ScreenAnalyzer {

    /**
     * Fallback visual locator when accessibility nodes are missing or hidden.
     * Maps standard phone viewport landmarks and app structures.
     */
    fun estimateVisualTarget(
        targetName: String,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): VisualElement? {
        val q = targetName.lowercase().trim()

        return when {
            // WhatsApp / Messenger Send Button
            q.contains("send") -> {
                val size = 140
                val right = screenWidth - 30
                val bottom = screenHeight - 60
                VisualElement(
                    label = "Send Button",
                    role = "button",
                    bounds = ElementBounds(right - size, bottom - size, right, bottom),
                    confidence = 0.88f
                )
            }

            // Message Input field
            q.contains("type a message") || q.contains("message") || q.contains("input") -> {
                val left = 30
                val right = screenWidth - 180
                val bottom = screenHeight - 60
                val top = bottom - 140
                VisualElement(
                    label = "Message Input Field",
                    role = "input",
                    bounds = ElementBounds(left, top, right, bottom),
                    confidence = 0.85f
                )
            }

            // Top Search Bar / Search Icon
            q.contains("search") -> {
                val size = 120
                val right = screenWidth - 60
                val top = 120
                VisualElement(
                    label = "Search Icon",
                    role = "button",
                    bounds = ElementBounds(right - size, top, right, top + size),
                    confidence = 0.90f
                )
            }

            // Top Back Button
            q.contains("back") || q.contains("navigate up") -> {
                val size = 120
                val left = 40
                val top = 120
                VisualElement(
                    label = "Back Arrow",
                    role = "button",
                    bounds = ElementBounds(left, top, left + size, top + size),
                    confidence = 0.92f
                )
            }

            // Floating Action Button (New Chat)
            q.contains("new chat") || q.contains("floating") || q.contains("fab") -> {
                val size = 160
                val right = screenWidth - 60
                val bottom = screenHeight - 240
                VisualElement(
                    label = "Floating Action Button",
                    role = "button",
                    bounds = ElementBounds(right - size, bottom - size, right, bottom),
                    confidence = 0.86f
                )
            }

            // Top Tab or Header
            q.contains("chats") || q.contains("tab") -> {
                VisualElement(
                    label = "Chats Tab",
                    role = "tab",
                    bounds = ElementBounds(100, 240, 350, 360),
                    confidence = 0.80f
                )
            }

            else -> null
        }
    }
}
