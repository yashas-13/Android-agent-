package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val delayBetweenGroupsSeconds: Int = 3,
    val autoRetryCount: Int = 1,
    val notifyOnCompletion: Boolean = true,
    val notifyOnFailure: Boolean = true,
    val preApproveBatch: Boolean = true,
    val customGeminiApiKey: String = "",
    val cactusEndpoint: String = "https://cactuscompute.com/needle",
    val cactusApiKey: String = "",
    val activeAiProvider: String = "CACTUS_NEEDLE",
    val overlayFloatingIconEnabled: Boolean = true,
    val overlayTransparency: Float = 0.95f,
    val overlayVerboseLogging: Boolean = true
)

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ai_agent_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private fun loadSettings(): SettingsState {
        return SettingsState(
            delayBetweenGroupsSeconds = prefs.getInt(KEY_DELAY_SECONDS, 3),
            autoRetryCount = prefs.getInt(KEY_AUTO_RETRY, 1),
            notifyOnCompletion = prefs.getBoolean(KEY_NOTIFY_COMPLETION, true),
            notifyOnFailure = prefs.getBoolean(KEY_NOTIFY_FAILURE, true),
            preApproveBatch = prefs.getBoolean(KEY_PRE_APPROVE_BATCH, true),
            customGeminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: "",
            cactusEndpoint = prefs.getString(KEY_CACTUS_ENDPOINT, "https://cactuscompute.com/needle") ?: "https://cactuscompute.com/needle",
            cactusApiKey = prefs.getString(KEY_CACTUS_API_KEY, "") ?: "",
            activeAiProvider = prefs.getString(KEY_AI_PROVIDER, "CACTUS_NEEDLE") ?: "CACTUS_NEEDLE",
            overlayFloatingIconEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, true),
            overlayTransparency = prefs.getFloat(KEY_OVERLAY_TRANSPARENCY, 0.95f),
            overlayVerboseLogging = prefs.getBoolean(KEY_OVERLAY_VERBOSE, true)
        )
    }

    fun updateDelayBetweenGroups(seconds: Int) {
        prefs.edit().putInt(KEY_DELAY_SECONDS, seconds).apply()
        _settings.value = _settings.value.copy(delayBetweenGroupsSeconds = seconds)
    }

    fun updateAutoRetryCount(count: Int) {
        prefs.edit().putInt(KEY_AUTO_RETRY, count).apply()
        _settings.value = _settings.value.copy(autoRetryCount = count)
    }

    fun updateNotifyOnCompletion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_COMPLETION, enabled).apply()
        _settings.value = _settings.value.copy(notifyOnCompletion = enabled)
    }

    fun updateNotifyOnFailure(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_FAILURE, enabled).apply()
        _settings.value = _settings.value.copy(notifyOnFailure = enabled)
    }

    fun updatePreApproveBatch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRE_APPROVE_BATCH, enabled).apply()
        _settings.value = _settings.value.copy(preApproveBatch = enabled)
    }

    fun updateCustomGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
        _settings.value = _settings.value.copy(customGeminiApiKey = key.trim())
    }

    fun updateCactusEndpoint(endpoint: String) {
        val target = endpoint.trim().ifEmpty { "https://cactuscompute.com/needle" }
        prefs.edit().putString(KEY_CACTUS_ENDPOINT, target).apply()
        _settings.value = _settings.value.copy(cactusEndpoint = target)
    }

    fun updateCactusApiKey(key: String) {
        prefs.edit().putString(KEY_CACTUS_API_KEY, key.trim()).apply()
        _settings.value = _settings.value.copy(cactusApiKey = key.trim())
    }

    fun updateActiveAiProvider(provider: String) {
        prefs.edit().putString(KEY_AI_PROVIDER, provider).apply()
        _settings.value = _settings.value.copy(activeAiProvider = provider)
    }

    fun updateOverlayFloatingIconEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(overlayFloatingIconEnabled = enabled)
    }

    fun updateOverlayTransparency(alpha: Float) {
        prefs.edit().putFloat(KEY_OVERLAY_TRANSPARENCY, alpha).apply()
        _settings.value = _settings.value.copy(overlayTransparency = alpha)
    }

    fun updateOverlayVerboseLogging(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_VERBOSE, enabled).apply()
        _settings.value = _settings.value.copy(overlayVerboseLogging = enabled)
    }

    companion object {
        private const val KEY_DELAY_SECONDS = "delay_seconds"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_NOTIFY_COMPLETION = "notify_completion"
        private const val KEY_NOTIFY_FAILURE = "notify_failure"
        private const val KEY_PRE_APPROVE_BATCH = "pre_approve_batch"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_CACTUS_ENDPOINT = "cactus_endpoint"
        private const val KEY_CACTUS_API_KEY = "cactus_api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_OVERLAY_TRANSPARENCY = "overlay_transparency"
        private const val KEY_OVERLAY_VERBOSE = "overlay_verbose"
    }
}
