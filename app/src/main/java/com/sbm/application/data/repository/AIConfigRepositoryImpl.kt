package com.sbm.application.data.repository

import com.sbm.application.domain.model.AIAnalysisConfig
import com.sbm.application.domain.repository.AIConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIConfigRepositoryImpl @Inject constructor(
    // TODO: 将来的にはEncryptedSharedPreferencesなどで永続化
    // private val preferences: EncryptedSharedPreferences
) : AIConfigRepository {
    
    private val _configFlow = MutableStateFlow(AIAnalysisConfig())
    
    override suspend fun saveConfig(config: AIAnalysisConfig) {
        _configFlow.value = config
        // TODO: 永続化実装
        // preferences.saveConfig(config)
    }
    
    override suspend fun getConfig(): AIAnalysisConfig {
        return _configFlow.value
        // TODO: 永続化から読み込み
        // return preferences.loadConfig() ?: AIAnalysisConfig()
    }
    
    override fun getConfigFlow(): Flow<AIAnalysisConfig> {
        return _configFlow.asStateFlow()
    }
    
    override suspend fun resetToDefaults() {
        _configFlow.value = AIAnalysisConfig()
        // TODO: 永続化から削除
        // preferences.clearConfig()
    }
}