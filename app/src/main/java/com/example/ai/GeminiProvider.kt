package com.example.ai

import com.example.BuildConfig
import com.example.accessibility.ScreenSnapshot
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
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

class GeminiProvider(
    private var customApiKey: String? = null
) : AiProvider {

    override val providerType = AiProviderType.GEMINI_CLOUD
    override val displayName = "Gemini 3.5 Flash (Cloud Intelligence)"

    private val localFallback = LocalModelProvider()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() {
            val custom = customApiKey?.trim()
            if (!custom.isNullOrEmpty()) return custom
            return try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

    fun setApiKey(key: String) {
        customApiKey = key
    }

    override suspend fun planNextAction(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse = withContext(Dispatchers.IO) {
        val key = apiKey
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            // Fall back gracefully to LocalModelProvider
            return@withContext localFallback.planNextAction(task, screenSnapshot, history)
        }

        try {
            val prompt = buildPlanningPrompt(task, screenSnapshot, history)
            val jsonResponse = callGeminiRestApi(prompt, key)
            parseGeminiResponse(jsonResponse, task, screenSnapshot, history)
        } catch (e: Exception) {
            // Graceful fallback to local model on error
            val fallback = localFallback.planNextAction(task, screenSnapshot, history)
            fallback.copy(
                thoughtProcess = "[Gemini Fallback: ${e.message?.take(50)}] ${fallback.thoughtProcess}"
            )
        }
    }

    override suspend fun verifyOutcome(
        task: String,
        executedAction: AgentAction,
        previousSnapshot: ScreenSnapshot,
        currentSnapshot: ScreenSnapshot
    ): VerificationResult = withContext(Dispatchers.IO) {
        val key = apiKey
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext localFallback.verifyOutcome(task, executedAction, previousSnapshot, currentSnapshot)
        }

        try {
            val prompt = """
                You are verifying an Android AI Phone Agent action.
                TASK: "$task"
                ACTION EXECUTED: ${executedAction.action} -> ${executedAction.target?.summary()}
                PREVIOUS SCREEN: ${previousSnapshot.packageName} (${previousSnapshot.interactiveCount} elements)
                CURRENT SCREEN: ${currentSnapshot.packageName} (${currentSnapshot.interactiveCount} elements)
                CURRENT ELEMENTS:
                ${currentSnapshot.toCompactText().take(500)}
                
                Respond in JSON only:
                {
                  "isSuccess": true,
                  "explanation": "Brief verification summary",
                  "requiresRecovery": false
                }
            """.trimIndent()

            val jsonResponse = callGeminiRestApi(prompt, key)
            val json = JSONObject(extractJsonFromText(jsonResponse))
            VerificationResult(
                isSuccess = json.optBoolean("isSuccess", true),
                explanation = json.optString("explanation", "Action verified."),
                requiresRecovery = json.optBoolean("requiresRecovery", false)
            )
        } catch (e: Exception) {
            localFallback.verifyOutcome(task, executedAction, previousSnapshot, currentSnapshot)
        }
    }

    private fun callGeminiRestApi(prompt: String, key: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

        val requestBodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("topK", 20)
            }
            put("generationConfig", generationConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw RuntimeException("Gemini HTTP ${response.code}: $errorBody")
            }
            val responseString = response.body?.string() ?: ""
            val parsed = JSONObject(responseString)
            val candidates = parsed.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""
            return text
        }
    }

    private fun buildPlanningPrompt(
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): String {
        val historyStr = if (history.isEmpty()) "None" else history.takeLast(5).joinToString("\n") {
            "- ${it.action.action} on ${it.action.target?.summary()} [${it.status}]"
        }

        return """
            You are the planning engine of an Android AI Phone Agent.
            Follow the closed-loop architecture: OBSERVE -> UNDERSTAND -> PLAN -> ACT -> VERIFY.
            Decide strictly ONE safe next action at a time.
            Prefer semantic targets (text, role, contentDescription) instead of coordinates.
            
            TASK: "$task"
            RECENT ACTION HISTORY:
            $historyStr
            
            CURRENT SCREEN STATE:
            ${screenSnapshot.toCompactText()}
            
            OUTPUT FORMAT: Return ONLY valid JSON with no markdown wrapping:
            {
              "thoughtProcess": "Short reasoning about screen and next step",
              "action": "CLICK | TYPE_TEXT | OPEN_APP | BACK | HOME | SCROLL | SWIPE | WAIT",
              "targetText": "Text of target button or field (or null)",
              "targetRole": "button | input | icon_button | switch | view (or null)",
              "payload": "Text to type or package name (or null)",
              "reason": "Why this action is chosen",
              "confidence": 0.95,
              "requiresConfirmation": false,
              "expectedOutcome": "What should change on screen",
              "isTaskComplete": false,
              "completionMessage": "Completion summary if finished"
            }
        """.trimIndent()
    }

    private fun parseGeminiResponse(
        rawText: String,
        task: String,
        screenSnapshot: ScreenSnapshot,
        history: List<ExecutedAction>
    ): AgentPlanResponse {
        val jsonStr = extractJsonFromText(rawText)
        val json = JSONObject(jsonStr)

        val isTaskComplete = json.optBoolean("isTaskComplete", false)
        val completionMessage = if (json.has("completionMessage")) json.optString("completionMessage") else null
        val thoughtProcess = json.optString("thoughtProcess", "Action formulated by Gemini.")
        val confidence = json.optDouble("confidence", 0.92).toFloat()

        if (isTaskComplete) {
            return AgentPlanResponse(
                action = null,
                isTaskComplete = true,
                completionMessage = completionMessage ?: "Task successfully accomplished!",
                thoughtProcess = thoughtProcess,
                confidence = confidence
            )
        }

        val actionName = json.optString("action", "WAIT").uppercase()
        val actionType = try {
            AgentActionType.valueOf(actionName)
        } catch (e: Exception) {
            AgentActionType.WAIT
        }

        val targetText = json.optString("targetText").ifBlank { null }
        val targetRole = json.optString("targetRole").ifBlank { null }
        val payload = json.optString("payload").ifBlank { null }
        val reason = json.optString("reason", "Execute step toward goal")
        val requiresConfirmation = json.optBoolean("requiresConfirmation", false)
        val expectedOutcome = if (json.has("expectedOutcome")) json.optString("expectedOutcome") else null

        val target = if (targetText != null || targetRole != null) {
            ActionTarget(text = targetText, role = targetRole)
        } else null

        val action = AgentAction(
            action = actionType,
            target = target,
            payload = payload,
            reason = reason,
            confidence = confidence,
            requiresConfirmation = requiresConfirmation,
            expectedOutcome = expectedOutcome
        )

        return AgentPlanResponse(
            action = action,
            isTaskComplete = false,
            thoughtProcess = thoughtProcess,
            confidence = confidence
        )
    }

    private fun extractJsonFromText(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            clean.substring(start, end + 1)
        } else {
            clean
        }
    }
}
