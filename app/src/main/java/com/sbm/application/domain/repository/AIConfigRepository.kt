package com.sbm.application.domain.repository

import com.sbm.application.domain.model.AIAnalysisConfig
import kotlinx.coroutines.flow.Flow

interface AIConfigRepository {
    suspend fun saveConfig(config: AIAnalysisConfig)
    suspend fun getConfig(): AIAnalysisConfig
    fun getConfigFlow(): Flow<AIAnalysisConfig>
    suspend fun resetToDefaults()
}