package com.sbm.application.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    fun fetchJwtToken(sessionId: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val response = authRepository.getOAuth2Session(sessionId)
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    // アクセストークンとユーザーIDを保存
                    authRepository.saveToken(loginResponse.token)
                    authRepository.saveUserId(loginResponse.userId)
                    
                    // リフレッシュトークンが存在する場合は保存
                    if (!loginResponse.refreshToken.isNullOrEmpty()) {
                        try {
                            authRepository.saveRefreshToken(loginResponse.refreshToken)
                        } catch (e: Exception) {
                            // リフレッシュトークン保存失敗時もログインは成功とする
                            // ログは出力しない（セキュリティ上の理由）
                        }
                    }
                    
                    emit(Result.Success(Unit))
                } ?: emit(Result.Error("レスポンスが空です"))
            } else {
                emit(Result.Error("認証失敗: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "不明なエラー"))
        }
    }
}

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}