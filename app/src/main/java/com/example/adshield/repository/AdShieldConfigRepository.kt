package com.example.adshield.repository

import android.content.Context
import android.util.Log
import com.example.adshield.engine.AdShieldEngine
import com.example.adshield.model.AdShieldConfig
import com.example.adshield.model.InterceptedLogEntry
import com.example.adshield.model.RequestDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AdShieldConfigRepository {

    private const val TAG = "AdShieldRepo"
    private const val CONFIG_FILE_NAME = "adshield_cached_config.json"
    private const val PREFS_NAME = "adshield_prefs"
    private const val KEY_ENABLED = "adshield_enabled"

    private val _configFlow = MutableStateFlow(AdShieldConfig())
    val configFlow: StateFlow<AdShieldConfig> = _configFlow.asStateFlow()

    private val _isEnabledFlow = MutableStateFlow(true)
    val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _interceptedLogs = MutableStateFlow<List<InterceptedLogEntry>>(emptyList())
    val interceptedLogs: StateFlow<List<InterceptedLogEntry>> = _interceptedLogs.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        _isEnabledFlow.value = enabled
        AdShieldEngine.isEnabled = enabled

        val loadedConfig = loadCachedOrAssetConfig(context)
        _configFlow.value = loadedConfig
        AdShieldEngine.updateConfig(loadedConfig)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _isEnabledFlow.value = enabled
        AdShieldEngine.isEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun logInterceptedRequest(decision: RequestDecision) {
        val entry = InterceptedLogEntry(
            url = decision.url,
            host = decision.host,
            isBlocked = decision is RequestDecision.Blocked,
            reason = decision.reason
        )
        val current = _interceptedLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _interceptedLogs.value = current
    }

    fun clearLogs() {
        _interceptedLogs.value = emptyList()
    }

    fun addPermittedAdHost(context: Context, host: String) {
        val cleanHost = AdShieldEngine.sanitizeHost(host)
        val current = _configFlow.value
        if (!current.adPermissions.contains(cleanHost)) {
            val updatedList = current.adPermissions + cleanHost
            val updatedConfig = current.copy(adPermissions = updatedList)
            saveConfig(context, updatedConfig)
        }
    }

    fun addGlobalAllowHost(context: Context, host: String) {
        val cleanHost = AdShieldEngine.sanitizeHost(host)
        val current = _configFlow.value
        if (!current.globalAllow.contains(cleanHost)) {
            val updatedList = current.globalAllow + cleanHost
            val updatedConfig = current.copy(globalAllow = updatedList)
            saveConfig(context, updatedConfig)
        }
    }

    fun saveConfig(context: Context, config: AdShieldConfig) {
        try {
            _configFlow.value = config
            AdShieldEngine.updateConfig(config)
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            file.writeText(config.toJson())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save AdShield config", e)
        }
    }

    private fun loadCachedOrAssetConfig(context: Context): AdShieldConfig {
        // Try file cache first
        try {
            val file = File(context.filesDir, CONFIG_FILE_NAME)
            if (file.exists()) {
                val text = file.readText()
                if (text.isNotBlank()) {
                    return AdShieldConfig.parseJson(text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached config, falling back to assets", e)
        }

        // Fallback to assets
        return try {
            val assetStream = context.assets.open("adshield/config.json")
            val text = assetStream.bufferedReader().use { it.readText() }
            AdShieldConfig.parseJson(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled assets adshield config", e)
            AdShieldConfig()
        }
    }
}
