package com.sbm.application.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.data.network.NetworkError
import com.sbm.application.data.network.NetworkMonitor
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.domain.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
// import android.util.Log // Removed for production
import com.sbm.application.BuildConfig

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val moodRepository: MoodRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()
    
    init {
        // ネットワーク状態を監視
        networkMonitor.networkState()
            .onEach { networkState ->
                _uiState.value = _uiState.value.copy(
                    isNetworkAvailable = networkState.isConnected
                )
            }
            .launchIn(viewModelScope)
    }
    
    fun loadMoodRecords() {
        viewModelScope.launch {
                        
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                networkError = null,
                error = null  // エラーもクリア
            )
            
            moodRepository.getMoodRecords()
                .onSuccess { moodRecords ->
                                        _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        moodRecords = moodRecords,
                        networkError = null,
                        error = null  // エラーをクリア
                    )
                }
                .onFailure { error ->
                                        val networkError = if (error is NetworkError) error else null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        networkError = networkError,
                        error = if (networkError == null) error.message ?: "エラーが発生しました" else null
                    )
                }
        }
    }
    
    fun createMoodRecord(date: String, mood: Int, note: String?) {
        viewModelScope.launch {
                        
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 直接API呼び出し（楽観的更新なし）
            moodRepository.createMoodRecord(date, mood, note)
                .onSuccess { _ ->
                                        // API成功後にデータを再取得
                    loadMoodRecords()
                }
                .onFailure { error ->
                                        _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "エラーが発生しました"
                    )
                }
        }
    }
    
    fun updateMoodRecord(date: String, mood: Int, note: String?) {
        viewModelScope.launch {
                        
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 直接API呼び出し（楽観的更新なし）
            moodRepository.updateMoodRecord(date, mood, note)
                .onSuccess { _ ->
                                        // API成功後にデータを再取得
                    loadMoodRecords()
                }
                .onFailure { error ->
                                        _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "エラーが発生しました"
                    )
                }
        }
    }
    
    fun deleteMoodRecord(date: String) {
        viewModelScope.launch {
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                networkError = null
            )
            
            // API呼び出し
            moodRepository.deleteMoodRecord(date)
                .onSuccess {
                    // 削除成功後に強制的にデータを再取得
                                        loadMoodRecords()
                }
                .onFailure { error ->
                                        // 削除失敗時はエラー表示
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "削除に失敗しました"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, networkError = null)
    }
    
    fun refreshMoodRecords() {
        viewModelScope.launch {
            moodRepository.getMoodRecords()
                .onSuccess { moodRecords ->
                    _uiState.value = _uiState.value.copy(
                        moodRecords = moodRecords,
                        networkError = null,
                        error = null
                    )
                }
                .onFailure { error ->
                    val networkError = if (error is NetworkError) error else null
                    _uiState.value = _uiState.value.copy(
                        networkError = networkError,
                        error = if (networkError == null) error.message ?: "エラーが発生しました" else null
                    )
                }
        }
    }
    
}

data class MoodUiState(
    val isLoading: Boolean = false,
    val moodRecords: List<MoodRecord> = emptyList(),
    val error: String? = null,
    val networkError: NetworkError? = null,
    val isNetworkAvailable: Boolean = true
)