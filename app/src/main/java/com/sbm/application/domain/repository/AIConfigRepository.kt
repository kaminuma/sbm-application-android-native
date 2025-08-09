package com.sbm.application.domain.repository

import com.sbm.application.domain.model.AIAnalysisConfig

interface AIConfigRepository {
    suspend fun getConfig(): AIAnalysisConfig
    suspend fun saveConfig(config: AIAnalysisConfig)
    suspend fun clearConfig()
    suspend fun resetToDefaults()
}