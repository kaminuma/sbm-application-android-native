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
                // Unauthorized - AuthInterceptorで既にリフレッシュ試行済み
                // ここに到達した時点でリフレッシュ失敗またはトークン期限切れ確定
                clearSessionAndNotify(message ?: "認証が無効です。再度ログインしてください。")
            }
            403 -> {
                // Forbidden - アクセス権限なし
                clearSessionAndNotify(message ?: "アクセス権限がありません。")
            }
            else -> {
                // その他の認証エラー
                _authErrorFlow.tryEmit(message ?: "認証エラーが発生しました")
            }
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