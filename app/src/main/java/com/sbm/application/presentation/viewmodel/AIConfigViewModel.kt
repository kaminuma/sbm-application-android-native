package com.sbm.application.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AIConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIConfigUiState(
    val config: AIAnalysisConfig = AIAnalysisConfig(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class AIConfigViewModel @Inject constructor(
    private val aiConfigRepository: AIConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIConfigUiState())
    val uiState: StateFlow<AIConfigUiState> = _uiState.asStateFlow()
    
    init {
        loadConfig()
    }
    
    fun updateAnalysisPeriod(period: AnalysisPeriod) {
        val currentConfig = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = currentConfig.copy(analysisPeriod = period),
            isSaved = false
        )
        autoSave()
    }
    
    fun updateComparisonOption(option: ComparisonOption) {
        val currentConfig = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = currentConfig.copy(comparisonOption = option),
            isSaved = false
        )
        autoSave()
    }
    
    fun updateAnalysisFocus(focus: AnalysisFocus) {
        val currentConfig = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = currentConfig.copy(analysisFocus = focus),
            isSaved = false
        )
        autoSave()
    }
    
    fun updateDetailLevel(level: DetailLevel) {
        val currentConfig = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = currentConfig.copy(detailLevel = level),
            isSaved = false
        )
        autoSave()
    }
    
    fun updateResponseStyle(style: ResponseStyle) {
        val currentConfig = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = currentConfig.copy(responseStyle = style),
            isSaved = false
        )
        autoSave()
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                aiConfigRepository.resetToDefaults()
                _uiState.value = _uiState.value.copy(
                    config = AIAnalysisConfig(),
                    isSaved = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "デフォルト設定への復元に失敗しました: ${e.message}"
                )
            }
        }
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val savedConfig = aiConfigRepository.getConfig()
                _uiState.value = _uiState.value.copy(
                    config = savedConfig,
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "設定の読み込みに失敗しました: ${e.message}"
                )
            }
        }
    }
    
    private fun autoSave() {
        viewModelScope.launch {
            try {
                aiConfigRepository.saveConfig(_uiState.value.config)
                _uiState.value = _uiState.value.copy(
                    isSaved = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "設定の保存に失敗しました: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // 現在の設定に基づいてプロンプト生成用のコンテキストを作成
    fun createPromptContext(request: AIAnalysisRequest): PromptContext {
        return PromptContext(
            config = _uiState.value.config,
            request = request,
            comparisonData = null // TODO: 将来実装
        )
    }
}