package com.sbm.application.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.sbm.application.domain.model.AIConfig
import com.sbm.application.domain.model.AIMode
import com.sbm.application.domain.model.MigrationState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "ai_config_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun getAIConfig(): AIConfig? {
        val modeString = encryptedPrefs.getString(KEY_AI_MODE, null) ?: return null
        val mode = try {
            AIMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return when (mode) {
            AIMode.DIRECT_GEMINI -> {
                val apiKey = encryptedPrefs.getString(KEY_GEMINI_API_KEY, null)
                if (apiKey.isNullOrBlank()) null
                else AIConfig.createGeminiConfig(apiKey)
            }
            AIMode.CUSTOM_API -> {
                val baseUrl = encryptedPrefs.getString(KEY_CUSTOM_API_URL, null)
                val token = encryptedPrefs.getString(KEY_CUSTOM_API_TOKEN, null)
                if (baseUrl.isNullOrBlank() || token.isNullOrBlank()) null
                else AIConfig.createCustomApiConfig(baseUrl, token)
            }
        }
    }
    
    fun saveAIConfig(config: AIConfig) {
        with(encryptedPrefs.edit()) {
            putString(KEY_AI_MODE, config.mode.name)
            putString(KEY_CONFIG_VERSION, config.configVersion)
            
            when (config.mode) {
                AIMode.DIRECT_GEMINI -> {
                    putString(KEY_GEMINI_API_KEY, config.geminiApiKey)
                    // カスタムAPI設定をクリア
                    remove(KEY_CUSTOM_API_URL)
                    remove(KEY_CUSTOM_API_TOKEN)
                }
                AIMode.CUSTOM_API -> {
                    putString(KEY_CUSTOM_API_URL, config.customApiBaseUrl)
                    putString(KEY_CUSTOM_API_TOKEN, config.customApiToken)
                    // Gemini設定をクリア
                    remove(KEY_GEMINI_API_KEY)
                }
            }
            
            config.migrationState?.let {
                putString(KEY_MIGRATION_STATE, it.name)
            }
            
            apply()
        }
    }
    
    fun clearAIConfig() {
        with(encryptedPrefs.edit()) {
            remove(KEY_AI_MODE)
            remove(KEY_GEMINI_API_KEY)
            remove(KEY_CUSTOM_API_URL)
            remove(KEY_CUSTOM_API_TOKEN)
            remove(KEY_CONFIG_VERSION)
            remove(KEY_MIGRATION_STATE)
            apply()
        }
    }
    
    fun isAIConfigured(): Boolean {
        return getAIConfig()?.isValid() == true
    }
    
    fun getConfigSummary(): String {
        val config = getAIConfig()
        return when {
            config == null -> "設定なし"
            !config.isValid() -> "設定不完全"
            config.mode == AIMode.DIRECT_GEMINI -> "Gemini直接接続 (${config.maskedGeminiApiKey})"
            config.mode == AIMode.CUSTOM_API -> "カスタムAPI (${config.customApiBaseUrl})"
            else -> "不明な設定"
        }
    }
    
    fun getGeminiApiKey(): String? {
        // まずlocal.properties（BuildConfig）から取得を試行
        val buildConfigKey = com.sbm.application.BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank()) {
            return buildConfigKey
        }
        
        // フォールバックとしてencryptedPrefsから取得
        return getAIConfig()?.geminiApiKey
    }
    
    fun hasGeminiApiKeyInBuildConfig(): Boolean {
        return com.sbm.application.BuildConfig.GEMINI_API_KEY.isNotBlank()
    }
    
    // Migration support
    fun setMigrationState(state: MigrationState) {
        encryptedPrefs.edit()
            .putString(KEY_MIGRATION_STATE, state.name)
            .apply()
    }
    
    fun getMigrationState(): MigrationState? {
        val stateString = encryptedPrefs.getString(KEY_MIGRATION_STATE, null)
        return stateString?.let {
            try {
                MigrationState.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    companion object {
        private const val KEY_AI_MODE = "ai_mode"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_CUSTOM_API_TOKEN = "custom_api_token"
        private const val KEY_CONFIG_VERSION = "config_version"
        private const val KEY_MIGRATION_STATE = "migration_state"
    }
}