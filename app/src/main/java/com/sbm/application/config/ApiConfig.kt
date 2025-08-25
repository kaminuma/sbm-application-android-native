package com.sbm.application.config

import android.content.Context
import java.util.Properties

object ApiConfig {
    private const val DEFAULT_API_BASE_URL = "https://api.sbm-app.com/api/v1/"
    private const val DEFAULT_BACKEND_URL = "https://api.sbm-app.com"
    private var _apiBaseUrl: String? = null
    private var _backendUrl: String? = null
    private var isInitialized = false
    
    val API_BASE_URL: String
        get() {
            if (!isInitialized) {
                initializeFromSystemProperties()
            }
            return _apiBaseUrl ?: DEFAULT_API_BASE_URL
        }
    
    val BACKEND_URL: String
        get() {
            if (!isInitialized) {
                initializeFromSystemProperties()
            }
            return _backendUrl ?: DEFAULT_BACKEND_URL
        }
    
    private fun initializeFromSystemProperties() {
        try {
            val apiUrl = com.sbm.application.BuildConfig.API_BASE_URL
            
            if (apiUrl.isNotEmpty()) {
                _apiBaseUrl = apiUrl
                _backendUrl = apiUrl.removeSuffix("/api/v1/").removeSuffix("/api/v1")
            } else {
                _apiBaseUrl = DEFAULT_API_BASE_URL
                _backendUrl = DEFAULT_BACKEND_URL
            }
        } catch (e: Exception) {
            _apiBaseUrl = DEFAULT_API_BASE_URL
            _backendUrl = DEFAULT_BACKEND_URL
        }
        isInitialized = true
    }
    fun setUrls(apiBaseUrl: String, backendUrl: String? = null) {
        _apiBaseUrl = apiBaseUrl
        _backendUrl = backendUrl ?: apiBaseUrl.removeSuffix("/api/v1/").removeSuffix("/api/v1")
        isInitialized = true
    }
}