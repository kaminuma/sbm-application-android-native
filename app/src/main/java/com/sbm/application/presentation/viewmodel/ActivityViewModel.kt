package com.sbm.application.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.data.network.NetworkError
import com.sbm.application.data.network.NetworkMonitor
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    
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
    
    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, networkError = null)
            
            activityRepository.getActivities()
                .onSuccess { activities ->
                    android.util.Log.d("ActivityViewModel", "loadActivities: Success - loaded ${activities.size} activities")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activities = activities,
                        networkError = null
                    )
                    android.util.Log.d("ActivityViewModel", "loadActivities: UI state updated with ${activities.size} activities")
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
    
    fun refreshActivities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, networkError = null)
            
            activityRepository.getActivities()
                .onSuccess { activities ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        activities = activities,
                        networkError = null
                    )
                }
                .onFailure { error ->
                    val networkError = if (error is NetworkError) error else null
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        networkError = networkError,
                        error = if (networkError == null) error.message ?: "エラーが発生しました" else null
                    )
                }
        }
    }
    
    fun createActivity(title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?) {
        viewModelScope.launch {
            android.util.Log.d("ActivityViewModel", "createActivity: Starting API call")
            
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 直接API呼び出し
            activityRepository.createActivity(title, contents, start, end, date, category, categorySub)
                .onSuccess { _ ->
                    android.util.Log.d("ActivityViewModel", "createActivity: API Success - reloading activities")
                    // API成功後にデータを再取得
                    loadActivities()
                }
                .onFailure { error ->
                    android.util.Log.e("ActivityViewModel", "createActivity: API Error - ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "エラーが発生しました"
                    )
                }
        }
    }
    
    fun updateActivity(activityId: Long, title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?) {
        viewModelScope.launch {
            android.util.Log.d("ActivityViewModel", "updateActivity: Starting API call")
            
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 直接API呼び出し
            activityRepository.updateActivity(activityId, title, contents, start, end, date, category, categorySub)
                .onSuccess { _ ->
                    android.util.Log.d("ActivityViewModel", "updateActivity: API Success - reloading activities")
                    // API成功後にデータを再取得
                    loadActivities()
                }
                .onFailure { error ->
                    android.util.Log.e("ActivityViewModel", "updateActivity: API Error - ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "エラーが発生しました"
                    )
                }
        }
    }
    
    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            android.util.Log.d("ActivityViewModel", "deleteActivity: Starting API call")
            
            // ローディング表示
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            // 直接API呼び出し
            activityRepository.deleteActivity(activityId)
                .onSuccess {
                    android.util.Log.d("ActivityViewModel", "deleteActivity: API Success - reloading activities")
                    // API成功後にデータを再取得
                    loadActivities()
                }
                .onFailure { error ->
                    android.util.Log.e("ActivityViewModel", "deleteActivity: API Error - ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "エラーが発生しました"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, networkError = null)
    }
}

data class ActivityUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activities: List<Activity> = emptyList(),
    val error: String? = null,
    val networkError: NetworkError? = null,
    val isNetworkAvailable: Boolean = true
)