package com.sbm.application.domain.usecase

import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val _sessionExpiredFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredFlow: SharedFlow<Unit> = _sessionExpiredFlow.asSharedFlow()
    
    private val _authErrorFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val authErrorFlow: SharedFlow<String> = _authErrorFlow.asSharedFlow()
    
    suspend fun handleAuthenticationError(responseCode: Int, message: String? = null) {
        when (responseCode) {
            401 -> {
                // Unauthorized - トークンが無効、リフレッシュを試行
                if (!tryRefreshToken()) {
                    clearSessionAndNotify("認証が無効です。再度ログインしてください。")
                }
            }
            403 -> {
                // Forbidden - アクセス権限なし（JWTの期限切れ等）、リフレッシュを試行
                if (!tryRefreshToken()) {
                    clearSessionAndNotify("セッションが期限切れです。再度ログインしてください。")
                }
            }
            else -> {
                // その他の認証エラー
                _authErrorFlow.tryEmit(message ?: "認証エラーが発生しました")
            }
        }
    }

    
    /**
     * トークンリフレッシュを試行
     * @return リフレッシュに成功した場合はtrue、失敗した場合はfalse
     */
    private suspend fun tryRefreshToken(): Boolean {
        return try {
            val result = authRepository.refreshToken()
            if (result.isSuccess) {
                _authErrorFlow.tryEmit("トークンを更新しました")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * セッション無効化とUI通知
     */
    private suspend fun clearSessionAndNotify(errorMessage: String) {
        try {
            // 認証情報をクリア
            authRepository.clearAuth()
            
            // UIに認証エラーを通知
            _authErrorFlow.tryEmit(errorMessage)
            
            // セッション期限切れイベントを発火
            _sessionExpiredFlow.tryEmit(Unit)
        } catch (e: Exception) {
            _authErrorFlow.tryEmit("セッション管理エラー: ${e.message}")
        }
    }
    
    /**
     * 手動ログアウト
     */
    suspend fun logout() {
        try {
            val token = authRepository.getStoredToken()
            if (!token.isNullOrEmpty()) {
                authRepository.logout(token)
            }
            _sessionExpiredFlow.tryEmit(Unit)
        } catch (e: Exception) {
            // ログアウト失敗でもセッション終了を通知
            _sessionExpiredFlow.tryEmit(Unit)
        }
    }
}