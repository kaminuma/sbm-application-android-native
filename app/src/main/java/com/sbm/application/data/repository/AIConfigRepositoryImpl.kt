package com.sbm.application.data.repository

import com.sbm.application.data.local.ConfigManager
import com.sbm.application.domain.model.AIAnalysisConfig
import com.sbm.application.domain.repository.AIConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIConfigRepositoryImpl @Inject constructor(
    private val configManager: ConfigManager
) : AIConfigRepository {

    override suspend fun getConfig(): AIAnalysisConfig = withContext(Dispatchers.IO) {
        configManager.getAIAnalysisConfig()
    }

    override suspend fun saveConfig(config: AIAnalysisConfig) = withContext(Dispatchers.IO) {
        configManager.saveAIAnalysisConfig(config)
    }

    override suspend fun clearConfig() = withContext(Dispatchers.IO) {
        configManager.clearAIAnalysisConfig()
    }

    override suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        val defaultConfig = AIAnalysisConfig()
        configManager.saveAIAnalysisConfig(defaultConfig)
    }
}