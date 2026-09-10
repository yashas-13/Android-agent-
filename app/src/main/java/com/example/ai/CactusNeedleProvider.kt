package com.example.ai

import com.example.accessibility.ScreenSnapshot
import com.example.accessibility.UiElementInfo
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import com.example.agent.ElementBounds
import com.example.agent.ExecutedAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cactus Needle AI Provider for autonomous mobile screen understanding and phone agent automation.
 * Default endpoint: https://cactuscompute.com/needle
 */
class CactusNeedleProvider(
    private var customEndpoint: String? = null,
    private var customApiKey: String? = null
) : AiProvider {

    override val providerType = AiProviderType.CACTUS_NEEDLE
    override val displayName = "Cactus Needle (https://cactuscompute.com/needle)"

    val endpoint: String
        get() = customEndpoint?.trim()?.ifEmpty { null } ?: DEFAULT_ENDPOINT

    private val localFallback = LocalModelProvider()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun setEndpoint(url: String) {
        customEndpoint = url.trim()
    }

    fun setApiKey(key: String) {
        customApiKey = key.trim()
    }

    override suspend fun planNextAction(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse = withContext(Dispatchers.IO) {
        try {
            val requestJson = buildCactusNeedlePayload(task, screenSnapshot, history)
            val responseBody = callCactusNeedleApi(requestJson)
            parseCactusNeedleResponse(responseBody, task, screenSnapshot, history)
        } catch (e: Exception) {
            // Graceful resilient fallback to local semantic model when offline/network failure
            val fallback = localFallback.planNextAction(task, screenSnapshot, history)
            fallback.copy(
                thoughtProcess = "[Cactus Needle: ${e.message?.take(60) ?: "Local Fallback"}] ${fallback.thoughtProcess}"
            )
        }
    }

    override suspend fun verifyOutcome(
        task: String,
        executedAction: AgentAction,
        previousSnapshot: ScreenSnapshot,
        currentSnapshot: ScreenSnapshot
    ): VerificationResult = withContext(Dispatchers.IO) {
        try {
            val verifyPayload = JSONObject().apply {
                put("mode", "verify")
                put("task", task)
                put("action", executedAction.action.name)
                put("action_target", executedAction.target?.summary())
                put("previous_package", previousSnapshot.packageName)
                put("current_package", currentSnapshot.packageName)
                put("previous_elements_count", previousSnapshot.interactiveCount)
                put("current_elements_count", currentSnapshot.interactiveCount)
            }

            val responseBody = callCactusNeedleApi(verifyPayload.toString())
            parseVerificationResponse(responseBody)
        } catch (e: Exception) {
            localFallback.verifyOutcome(task, executedAction, previousSnapshot, currentSnapshot)
        }
    }

    private fun buildCactusNeedlePayload(
        task: String,
        snapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): String {
        val root = JSONObject()
        root.put("task", task)
        root.put("model", "needle-phone-agent")

        // Screen state representation
        val screenObj = JSONObject().apply {
            put("package_name", snapshot.packageName)
            put("window_title", snapshot.windowTitle)
            put("keyboard_visible", snapshot.isKeyboardVisible)
            put("interactive_count", snapshot.interactiveCount)
            put("total_elements", snapshot.totalCount)
            snapshot.screenshotBase64?.let { put("screenshot_base64", it) }

            val elementsArray = JSONArray()
            snapshot.interactiveElements.take(50).forEach { el ->
                val elObj = JSONObject().apply {
                    put("id", el.id)
                    put("role", el.role)
                    el.text?.let { put("text", it) }
                    el.contentDescription?.let { put("desc", it) }
                    el.resourceId?.let { put("res_id", it.substringAfterLast('/')) }
                    put("bounds", JSONObject().apply {
                        put("left", el.bounds.left)
                        put("top", el.bounds.top)
                        put("right", el.bounds.right)
                        put("bottom", el.bounds.bottom)
                        put("center_x", el.bounds.centerX)
                        put("center_y", el.bounds.centerY)
                    })
                    put("clickable", el.isClickable)
                    put("editable", el.isEditable)
                }
                elementsArray.put(elObj)
            }
            put("elements", elementsArray)
        }
        root.put("screen", screenObj)

        // Action history
        val historyArray = JSONArray()
        history.takeLast(10).forEach { h ->
            val hObj = JSONObject().apply {
                put("action", h.action.action.name)
                put("target", h.action.target?.summary() ?: "")
                put("status", h.status.name)
                put("result", h.resultMessage)
            }
            historyArray.put(hObj)
        }
        root.put("history", historyArray)

        // Capable actions
        val supportedActions = JSONArray().apply {
            put("SCREENSHOT")
            put("TAP")
            put("CLICK")
            put("SWIPE")
            put("NEXT")
            put("MINIMIZE")
            put("SEND")
            put("ENTER_KEY")
            put("TYPE_TEXT")
            put("CLEAR_TEXT")
            put("SCROLL")
            put("BACK")
            put("HOME")
            put("OPEN_APP")
            put("WAIT")
        }
        root.put("supported_actions", supportedActions)

        // Instructional prompt
        val systemPrompt = """
            You are Cactus Needle AI, an autonomous Android Phone Agent.
            Analyze the current screen elements and decide the single next best action to advance the user's task.
            Always output a JSON object:
            {
              "thought": "Reasoning about current screen and next action",
              "action": "TAP" | "SWIPE" | "NEXT" | "MINIMIZE" | "SEND" | "ENTER_KEY" | "SCREENSHOT" | "TYPE_TEXT" | "BACK" | "HOME" | "OPEN_APP" | "WAIT",
              "target": {
                "text": "Element label or null",
                "role": "button|input|view",
                "resourceId": "resource ID or null",
                "x": 540,
                "y": 1200
              },
              "payload": "Text to type or swipe direction (up/down/left/right) or package name",
              "confidence": 0.98,
              "isTaskComplete": false,
              "completionMessage": null
            }
        """.trimIndent()
        root.put("instructions", systemPrompt)

        return root.toString()
    }

    private fun callCactusNeedleApi(jsonPayload: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "CactusNeedle-AndroidPhoneAgent/1.0")

        val key = customApiKey?.trim()
        if (!key.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $key")
            requestBuilder.addHeader("X-API-Key", key)
        }

        val request = requestBuilder.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code}: ${response.message}")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response body from Cactus Needle")
        }
    }

    private fun parseCactusNeedleResponse(
        responseBody: String,
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        val root = JSONObject(responseBody)

        // Check if response contains direct JSON action or wrapped in "data" or "choices"
        val actionJson = when {
            root.has("action") -> root
            root.has("data") && root.getJSONObject("data").has("action") -> root.getJSONObject("data")
            root.has("choices") -> {
                val content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                extractJsonObject(content)
            }
            else -> root
        }

        val isComplete = actionJson.optBoolean("isTaskComplete", false)
        val completionMessage = actionJson.optString("completionMessage", "").ifEmpty { null }
        val thought = actionJson.optString("thought", "Cactus Needle evaluated screen.")
        val confidence = actionJson.optDouble("confidence", 0.95).toFloat()

        if (isComplete) {
            return AgentPlanResponse(
                action = null,
                isTaskComplete = true,
                completionMessage = completionMessage ?: "Task completed successfully via Cactus Needle.",
                thoughtProcess = thought,
                confidence = confidence
            )
        }

        val actionStr = actionJson.optString("action", "WAIT").uppercase().trim()
        val actionType = mapActionType(actionStr)
        val payload = actionJson.optString("payload", "").ifEmpty { null }

        var target: ActionTarget? = null
        if (actionJson.has("target") && !actionJson.isNull("target")) {
            val targetObj = actionJson.getJSONObject("target")
            val text = targetObj.optString("text", "").ifEmpty { null }
            val role = targetObj.optString("role", "").ifEmpty { null }
            val resId = targetObj.optString("resourceId", "").ifEmpty { null }
            val x = if (targetObj.has("x")) targetObj.optDouble("x").toFloat() else null
            val y = if (targetObj.has("y")) targetObj.optDouble("y").toFloat() else null

            var bounds: ElementBounds? = null
            if (targetObj.has("bounds") && !targetObj.isNull("bounds")) {
                val b = targetObj.getJSONObject("bounds")
                bounds = ElementBounds(
                    left = b.optInt("left", 0),
                    top = b.optInt("top", 0),
                    right = b.optInt("right", 0),
                    bottom = b.optInt("bottom", 0)
                )
            }

            target = ActionTarget(
                text = text,
                role = role,
                resourceId = resId,
                bounds = bounds,
                x = x,
                y = y
            )
        }

        val agentAction = AgentAction(
            action = actionType,
            target = target,
            payload = payload,
            reason = thought,
            confidence = confidence
        )

        return AgentPlanResponse(
            action = agentAction,
            isTaskComplete = false,
            thoughtProcess = thought,
            confidence = confidence
        )
    }

    private fun mapActionType(actionStr: String): AgentActionType {
        return when (actionStr) {
            "TAP" -> AgentActionType.TAP
            "CLICK" -> AgentActionType.CLICK
            "SWIPE" -> AgentActionType.SWIPE
            "NEXT" -> AgentActionType.NEXT
            "MINIMIZE" -> AgentActionType.MINIMIZE
            "SEND" -> AgentActionType.SEND
            "ENTER", "ENTER_KEY" -> AgentActionType.ENTER_KEY
            "SCREENSHOT", "CAPTURE" -> AgentActionType.SCREENSHOT
            "TYPE", "TYPE_TEXT" -> AgentActionType.TYPE_TEXT
            "CLEAR", "CLEAR_TEXT" -> AgentActionType.CLEAR_TEXT
            "SCROLL" -> AgentActionType.SCROLL
            "BACK" -> AgentActionType.BACK
            "HOME" -> AgentActionType.HOME
            "OPEN_APP", "LAUNCH" -> AgentActionType.OPEN_APP
            "LONG_CLICK", "LONG_PRESS" -> AgentActionType.LONG_CLICK
            "FIND_ELEMENT" -> AgentActionType.FIND_ELEMENT
            else -> AgentActionType.WAIT
        }
    }

    private fun extractJsonObject(text: String): JSONObject {
        val trimmed = text.trim()
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            val jsonStr = trimmed.substring(startIndex, endIndex + 1)
            return JSONObject(jsonStr)
        }
        return JSONObject()
    }

    private fun parseVerificationResponse(responseBody: String): VerificationResult {
        return try {
            val json = JSONObject(responseBody)
            VerificationResult(
                isSuccess = json.optBoolean("isSuccess", true),
                explanation = json.optString("explanation", "Action verified successfully by Cactus Needle"),
                requiresRecovery = json.optBoolean("requiresRecovery", false)
            )
        } catch (e: Exception) {
            VerificationResult(
                isSuccess = true,
                explanation = "Verification completed.",
                requiresRecovery = false
            )
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://cactuscompute.com/needle"
    }
}
