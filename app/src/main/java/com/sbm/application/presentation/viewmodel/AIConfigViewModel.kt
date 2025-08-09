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
    
    // updateAnalysisPeriod は削除（期間固定のため不要）
    
    // TODO: 将来的に比較分析機能を追加時に実装予定
    // fun updateComparisonOption(option: ComparisonOption) { ... }
    
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
    
    // TODO: 将来的に比較分析機能を追加時に実装予定
    // fun createPromptContext(request: AIAnalysisRequest): PromptContext { ... }
}