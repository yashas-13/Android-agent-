package com.example.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.agent.ElementBounds

object AccessibilityNodeParser {

    private const val MAX_DEPTH = 12
    private const val MAX_NODES = 250

    fun parseTree(
        root: AccessibilityNodeInfo?,
        packageNameOverride: String? = null,
        windowTitle: String? = null
    ): ScreenSnapshot {
        if (root == null) {
            return ScreenSnapshot(packageName = packageNameOverride ?: "")
        }

        val elements = mutableListOf<UiElementInfo>()
        val packageName = packageNameOverride ?: root.packageName?.toString() ?: ""
        var isKeyboardVisible = false

        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || elements.size >= MAX_NODES) return

            val rect = Rect()
            node.getBoundsInScreen(rect)

            // Check if keyboard is likely active
            val pkg = node.packageName?.toString() ?: ""
            if (pkg.contains("inputmethod") || pkg.contains("keyboard") || pkg.contains("gboard")) {
                isKeyboardVisible = true
            }

            val isVisible = node.isVisibleToUser && rect.width() > 0 && rect.height() > 0
            if (isVisible) {
                val role = determineRole(node)
                val element = UiElementInfo(
                    id = "${node.viewIdResourceName ?: "node"}_${elements.size}",
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    resourceId = node.viewIdResourceName,
                    className = node.className?.toString(),
                    role = role,
                    bounds = ElementBounds(rect.left, rect.top, rect.right, rect.bottom),
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    isEnabled = node.isEnabled,
                    isFocused = node.isFocused,
                    isSelected = node.isSelected,
                    packageName = pkg.ifEmpty { packageName },
                    depth = depth
                )

                // Only record elements that contain text, content description, or are interactive/scrollable
                if (!element.text.isNullOrBlank() ||
                    !element.contentDescription.isNullOrBlank() ||
                    element.isClickable ||
                    element.isEditable ||
                    element.isScrollable
                ) {
                    elements.add(element)
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    traverse(child, depth + 1)
                } finally {
                    child.recycle()
                }
            }
        }

        traverse(root, 0)

        return ScreenSnapshot(
            packageName = packageName,
            windowTitle = windowTitle ?: "",
            elements = elements,
            isKeyboardVisible = isKeyboardVisible
        )
    }

    private fun determineRole(node: AccessibilityNodeInfo): String {
        val cls = (node.className ?: "").toString().lowercase()
        return when {
            node.isEditable || cls.contains("edittext") -> "input"
            cls.contains("button") -> "button"
            cls.contains("checkbox") -> "checkbox"
            cls.contains("switch") -> "switch"
            cls.contains("imageview") || cls.contains("image") -> if (node.isClickable) "icon_button" else "image"
            cls.contains("recyclerview") || cls.contains("listview") || cls.contains("scrollview") -> "scroll_container"
            cls.contains("textview") -> if (node.isClickable) "link_text" else "text"
            cls.contains("radio") -> "radio"
            cls.contains("tab") -> "tab"
            node.isClickable -> "clickable"
            else -> "view"
        }
    }
}
