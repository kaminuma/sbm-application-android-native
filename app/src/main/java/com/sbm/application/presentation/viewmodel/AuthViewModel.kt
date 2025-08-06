package com.sbm.application.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.BuildConfig
import com.sbm.application.domain.model.User
import com.sbm.application.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isAuthenticated = false)
            
            authRepository.login(username, password)
                .onSuccess { (token, userId) ->
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthViewModel", "Login success: [TOKEN_REDACTED], userId=$userId")
                    }
                    
                    // 認証状態を段階的に更新
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    
                    // 少し遅延を入れてから認証状態を更新
                    kotlinx.coroutines.delay(50)
                    
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        user = User(id = userId, username = username, email = "")
                    )
                    
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthViewModel", "Auth state updated: isAuthenticated=true")
                        Log.d("AuthViewModel", "Final UI State: ${_uiState.value}")
                    }
                }
                .onFailure { error ->
                    if (BuildConfig.DEBUG) {
                        Log.e("AuthViewModel", "Login failed: ${error.message}")
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = error.message ?: "ログインエラーが発生しました"
                    )
                }
        }
    }
    
    fun register(username: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // バリデーション
            if (password != confirmPassword) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "パスワードが一致しません"
                )
                return@launch
            }
            
            if (password.length < 6) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "パスワードは6文字以上で入力してください"
                )
                return@launch
            }
            
            authRepository.register(username, email, password)
                .onSuccess { (_, _) ->
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthViewModel", "Register success")
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,  // 自動ログインしない
                        user = null,
                        error = null,
                        registrationSuccess = true  // 登録成功フラグを追加
                    )
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthViewModel", "Registration completed successfully")
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "登録エラーが発生しました"
                    )
                }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                Log.d("AuthViewModel", "Logout started")
            }
            
            // ローディング状態開始
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(20) // UI更新を確実にする
            
            val token = authRepository.getStoredToken()
            if (token != null) {
                try {
                    authRepository.logout(token)
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthViewModel", "Server logout completed")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("AuthViewModel", "Server logout failed: ${e.message}")
                    }
                    // サーバーのログアウトが失敗してもローカル状態はクリア
                }
            }
            
            // 段階的な状態クリア
            _uiState.value = _uiState.value.copy(
                isAuthenticated = false,
                isLoading = false
            )
            delay(30) // 認証状態の更新を確実にする
            
            _uiState.value = _uiState.value.copy(
                user = null,
                error = null
            )
            
            if (BuildConfig.DEBUG) {
                Log.d("AuthViewModel", "Logout completed successfully")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearRegistrationSuccess() {
        _uiState.value = _uiState.value.copy(registrationSuccess = false)
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                Log.d("AuthViewModel", "Checking initial auth status...")
            }
            val isLoggedIn = authRepository.isLoggedIn()
            val storedToken = authRepository.getStoredToken()
            val storedUserId = authRepository.getStoredUserId()
            
            if (BuildConfig.DEBUG) {
                Log.d("AuthViewModel", "Initial auth check: isLoggedIn=$isLoggedIn")
                Log.d("AuthViewModel", "Stored token: ${if (storedToken != null) "[REDACTED]" else "null"}")
                Log.d("AuthViewModel", "Stored userId: $storedUserId")
            }
            
            _uiState.value = _uiState.value.copy(isAuthenticated = isLoggedIn)
            
            if (isLoggedIn && storedUserId != null) {
                // 既存のユーザー情報を復元
                _uiState.value = _uiState.value.copy(
                    user = User(id = storedUserId, username = "User", email = "")
                )
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val registrationSuccess: Boolean = false
)