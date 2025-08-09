package com.sbm.application.domain.model

data class AIConfig(
    val mode: AIMode,
    private val _geminiApiKey: String? = null,
    private val _customApiBaseUrl: String? = null,
    private val _customApiToken: String? = null,
    val configVersion: String = "1.0",
    val migrationState: MigrationState? = null
) {
    // APIキー（実際の値）
    val geminiApiKey: String?
        get() = _geminiApiKey
    
    val customApiToken: String?
        get() = _customApiToken
    
    // APIキーのマスク化表示
    val maskedGeminiApiKey: String?
        get() = _geminiApiKey?.let { key ->
            if (key.length > 8) {
                "${key.take(4)}${"*".repeat(key.length - 8)}${key.takeLast(4)}"
            } else "*".repeat(key.length)
        }
    
    val maskedCustomApiToken: String?
        get() = _customApiToken?.let { token ->
            if (token.length > 8) {
                "${token.take(4)}${"*".repeat(token.length - 8)}${token.takeLast(4)}"
            } else "*".repeat(token.length)
        }
    
    // URL検証付きプロパティ
    val customApiBaseUrl: String?
        get() = _customApiBaseUrl?.takeIf { isValidUrl(it) }
    
    fun isValid(): Boolean {
        return when (mode) {
            AIMode.DIRECT_GEMINI -> !_geminiApiKey.isNullOrBlank()
            AIMode.CUSTOM_API -> !_customApiBaseUrl.isNullOrBlank() && 
                                !_customApiToken.isNullOrBlank() && 
                                isValidUrl(_customApiBaseUrl)
        }
    }
    
    fun canMigrateTo(targetMode: AIMode): Boolean {
        return when (this.mode to targetMode) {
            AIMode.DIRECT_GEMINI to AIMode.CUSTOM_API -> 
                !_customApiBaseUrl.isNullOrBlank() && 
                !_customApiToken.isNullOrBlank() && 
                isValidUrl(_customApiBaseUrl)
            AIMode.CUSTOM_API to AIMode.DIRECT_GEMINI ->
                !_geminiApiKey.isNullOrBlank()
            else -> false
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI.create(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        fun createGeminiConfig(apiKey: String): AIConfig {
            return AIConfig(
                mode = AIMode.DIRECT_GEMINI,
                _geminiApiKey = apiKey
            )
        }
        
        fun createCustomApiConfig(baseUrl: String, token: String): AIConfig {
            return AIConfig(
                mode = AIMode.CUSTOM_API,
                _customApiBaseUrl = baseUrl,
                _customApiToken = token
            )
        }
    }
}

enum class AIMode {
    DIRECT_GEMINI,    // Phase 1: 直接Gemini API
    CUSTOM_API        // Phase 2: 独自API経由
}

enum class MigrationState {
    NONE,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}