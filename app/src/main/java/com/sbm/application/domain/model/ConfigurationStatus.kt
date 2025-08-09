package com.sbm.application.domain.model

data class ConfigurationStatus(
    val isConfigured: Boolean,
    val hasValidCredentials: Boolean,
    val mode: String,
    val errorMessage: String? = null
) {
    companion object {
        fun notConfigured(message: String = "AI設定が未完了です"): ConfigurationStatus {
            return ConfigurationStatus(
                isConfigured = false,
                hasValidCredentials = false,
                mode = "none",
                errorMessage = message
            )
        }
        
        fun configured(mode: String): ConfigurationStatus {
            return ConfigurationStatus(
                isConfigured = true,
                hasValidCredentials = true,
                mode = mode
            )
        }
        
        fun invalidCredentials(mode: String, message: String): ConfigurationStatus {
            return ConfigurationStatus(
                isConfigured = true,
                hasValidCredentials = false,
                mode = mode,
                errorMessage = message
            )
        }
    }
}