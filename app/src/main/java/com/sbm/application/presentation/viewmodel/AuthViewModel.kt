package com.sbm.application.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.BuildConfig
import com.sbm.application.domain.model.User
import com.sbm.application.domain.repository.AuthRepository
import com.sbm.application.domain.exception.AccountLockedException
import com.sbm.application.domain.exception.BadCredentialsException
import com.sbm.application.domain.exception.AuthenticationFailedException
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
    
    private fun debugLog(message: String) {
        // BuildConfig.DEBUGの安全な参照（リリースビルド対応）
        try {
            if (BuildConfig.DEBUG) {
                Log.d("AuthViewModel", message)
            }
        } catch (e: Exception) {
            // リリースビルドではログを出力しない
        }
    }
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                debugLog("Login attempt started for user: $username")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, isAuthenticated = false)
                
                val loginResult = authRepository.login(username, password)
                
                loginResult
                    .onSuccess { (_, userId) ->
                        debugLog("Login successful for userId: $userId")
                        
                        // 認証状態を段階的に更新
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        
                        // 少し遅延を入れてから認証状態を更新
                        kotlinx.coroutines.delay(100)
                        
                        _uiState.value = _uiState.value.copy(
                            isAuthenticated = true,
                            user = User(id = userId, username = username, email = "")
                        )
                        debugLog("Authentication state updated successfully")
                    }
                    .onFailure { error ->
                        debugLog("Login failed: ${error.message}")
                        
                        when (error) {
                            is AccountLockedException -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    error = error.message,
                                    errorType = AuthErrorType.ACCOUNT_LOCKED,
                                    lockoutTimeRemaining = error.lockoutTimeRemaining,
                                    remainingAttempts = null
                                )
                            }
                            is BadCredentialsException -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    error = error.message,
                                    errorType = AuthErrorType.BAD_CREDENTIALS,
                                    lockoutTimeRemaining = null,
                                    remainingAttempts = error.remainingAttempts
                                )
                            }
                            is AuthenticationFailedException -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    error = error.message,
                                    errorType = AuthErrorType.AUTHENTICATION_FAILED,
                                    lockoutTimeRemaining = null,
                                    remainingAttempts = null
                                )
                            }
                            else -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    error = error.message ?: "ログインエラーが発生しました",
                                    errorType = AuthErrorType.GENERAL_ERROR,
                                    lockoutTimeRemaining = null,
                                    remainingAttempts = null
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                debugLog("Login exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    error = "予期しないエラーが発生しました: ${e.message}"
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,  // 自動ログインしない
                        user = null,
                        error = null,
                        registrationSuccess = true  // 登録成功フラグを追加
                    )
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
            
            // ローディング状態開始
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(20) // UI更新を確実にする
            
            val token = authRepository.getStoredToken()
            if (token != null) {
                try {
                    authRepository.logout(token)
                } catch (e: Exception) {
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
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            errorType = null,
            lockoutTimeRemaining = null,
            remainingAttempts = null
        )
    }
    
    fun clearRegistrationSuccess() {
        _uiState.value = _uiState.value.copy(registrationSuccess = false)
    }
    
    fun checkAuthStatus() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            val storedUserId = authRepository.getStoredUserId()
            
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
    val errorType: AuthErrorType? = null,
    val lockoutTimeRemaining: Long? = null,
    val remainingAttempts: Int? = null,
    val registrationSuccess: Boolean = false
)

enum class AuthErrorType {
    ACCOUNT_LOCKED,
    BAD_CREDENTIALS,
    AUTHENTICATION_FAILED,
    GENERAL_ERROR
}