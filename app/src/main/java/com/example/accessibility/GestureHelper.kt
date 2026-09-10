package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object GestureHelper {

    suspend fun clickAt(
        service: AccessibilityService,
        x: Float,
        y: Float,
        durationMs: Long = 100L
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGestureSuspending(service, gesture)
    }

    suspend fun longClickAt(
        service: AccessibilityService,
        x: Float,
        y: Float,
        durationMs: Long = 650L
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGestureSuspending(service, gesture)
    }

    suspend fun swipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGestureSuspending(service, gesture)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun dispatchGestureSuspending(
        service: AccessibilityService,
        gesture: GestureDescription
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (continuation.isActive) continuation.resume(false)
            }
        }

        val dispatched = service.dispatchGesture(gesture, callback, null)
        if (!dispatched && continuation.isActive) {
            continuation.resume(false)
        }
    }
}
