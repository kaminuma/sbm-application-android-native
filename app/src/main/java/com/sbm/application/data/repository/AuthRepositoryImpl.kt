package com.sbm.application.data.repository

import android.content.Context

import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AuthDto
import com.sbm.application.data.remote.dto.LoginResponse
import com.sbm.application.data.remote.AuthInterceptor
import com.sbm.application.domain.repository.AuthRepository
import com.sbm.application.domain.exception.AccountLockedException
import com.sbm.application.domain.exception.BadCredentialsException
import com.sbm.application.domain.exception.AuthenticationFailedException

import retrofit2.Response
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import org.json.JSONObject

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {
    
    // セキュアなトークン管理に変更
    private val tokenManager: com.sbm.application.data.security.SecureTokenManager by lazy {
        com.sbm.application.data.security.SecureTokenManager(context)
    }
    private var cachedToken: String? = null
    
    init {
        // AuthInterceptorにリフレッシュコールバックを設定
        AuthInterceptor.refreshTokenCallback = { refreshTokenForInterceptor() }
    }
    
    override suspend fun login(username: String, password: String): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                // セキュアなログ出力
                com.sbm.application.data.security.SecureLogger.debug("AuthRepository", "ログイン試行開始")
                
                val response = apiService.login(
                    AuthDto.LoginRequest(username = username, password = password)
                )
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    
                    if (loginResponse != null && !loginResponse.token.isNullOrEmpty() && !loginResponse.userId.isNullOrEmpty()) {
                        // セキュアなトークン保存
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                tokenManager.saveAccessToken(loginResponse.token!!)
                                tokenManager.saveUserId(loginResponse.userId!!)
                                if (!loginResponse.refreshToken.isNullOrEmpty()) {
                                    tokenManager.saveRefreshToken(loginResponse.refreshToken)
                                }
                            } else {
                                return@withContext Result.failure(
                                    SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
                                )
                            }
                        } catch (e: SecurityException) {
                            return@withContext Result.failure(e)
                        }
                        
                        cachedToken = loginResponse.token
                        Result.success(Pair(loginResponse.token!!, loginResponse.userId!!))
                    } else {
                        Result.failure(Exception("ログインの応答が無効です: ユーザーデータが見つかりません"))
                    }
                } else {
                    // エラーレスポンスを解析
                    val errorException = parseAuthErrorResponse(response)
                    Result.failure(errorException)
                }
            } catch (e: Exception) {

                // エラー情報の匿名化
                com.sbm.application.data.security.SecureLogger.error("AuthRepository", "ログイン処理でエラーが発生", e)
                
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> "ネットワークに接続できません。"
                    e.message?.contains("timeout") == true -> "接続がタイムアウトしました。"
                    else -> "ログインできませんでした。${e.message ?: ""}"
                }
                Result.failure(AuthenticationFailedException(errorMessage))
            }
        }
    }
    
    override suspend fun register(username: String, email: String, password: String): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    AuthDto.RegisterRequest(
                        username = username,
                        email = email,
                        password = password
                    )
                )
                
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    
                    if (registerResponse != null) {
                        // 登録成功の場合、成功のダミー値を返す（自動ログインは行わない）
                        Result.success(Pair("registration_success", "registration_success"))
                    } else {
                        Result.failure(Exception("登録の応答が無効です: レスポンスボディが空です"))
                    }
                } else {
                    Result.failure(Exception("登録に失敗しました: ${response.message()} (${response.code()})"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("登録処理中にエラーが発生しました: ${e.message}"))
            }
        }
    }
    
    override suspend fun logout(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    tokenManager.getRefreshToken()
                } else null
                
                if (!refreshToken.isNullOrEmpty()) {
                    // サーバーにログアウト通知（エラーは無視）
                    try {
                        apiService.logout(AuthDto.LogoutRequest(refreshToken))
                    } catch (e: Exception) {
                        // ネットワークエラーは無視してローカルクリーンアップを続行
                        com.sbm.application.data.security.SecureLogger.debug("AuthRepository", "ログアウト通知失敗（続行）")
                    }
                }
                
                clearAuth()
                Result.success(Unit)
            } catch (e: Exception) {
                clearAuth()
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        val storedToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                tokenManager.getAccessToken()
            } catch (e: SecurityException) {
                null
            }
        } else {
            null
        }
        
        cachedToken = storedToken
        
        // トークンが存在しない場合はfalse
        if (storedToken.isNullOrEmpty()) {
            return false
        }
        
        // アクセストークンが有効ならOK
        if (isTokenValid(storedToken)) {
            return true
        }
        
        // アクセストークンが無効 → リフレッシュトークンで復帰を試行
        com.sbm.application.data.security.SecureLogger.debug("AuthRepository", "アクセストークンが無効のため、リフレッシュを試行します")
        val refreshResult = refreshToken()
        if (refreshResult.isSuccess) {
            com.sbm.application.data.security.SecureLogger.debug("AuthRepository", "リフレッシュ成功：ログイン状態を維持します")
            return true
        }
        
        com.sbm.application.data.security.SecureLogger.debug("AuthRepository", "リフレッシュ失敗：ログアウトします")
        return false
    }
    
    override suspend fun getStoredToken(): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val token = tokenManager.getAccessToken()
                cachedToken = token
                token
            } else {
                null
            }
        } catch (e: SecurityException) {
            // トークン取得失敗時は認証失効とみなす
            cachedToken = null
            null
        }
    }
    
    override suspend fun getStoredUserId(): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.getUserId()
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }
    }
    
    override suspend fun getStoredRefreshToken(): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.getRefreshToken()
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }
    }
    
    override suspend fun refreshToken(): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = getStoredRefreshToken()
                if (refreshToken.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("リフレッシュトークンが見つかりません"))
                }
                
                val response = apiService.refreshToken(
                    AuthDto.RefreshTokenRequest(refreshToken = refreshToken)
                )
                
                if (response.isSuccessful) {
                    val refreshResponse = response.body()
                    if (refreshResponse != null && !refreshResponse.token.isNullOrEmpty()) {
                        // セキュアなトークン保存
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                tokenManager.saveAccessToken(refreshResponse.token!!)
                                if (!refreshResponse.refreshToken.isNullOrEmpty()) {
                                    tokenManager.saveRefreshToken(refreshResponse.refreshToken)
                                }
                            } else {
                                return@withContext Result.failure(
                                    SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
                                )
                            }
                        } catch (e: SecurityException) {
                            return@withContext Result.failure(e)
                        }
                        
                        cachedToken = refreshResponse.token
                        
                        Result.success(Pair(refreshResponse.token!!, refreshResponse.refreshToken ?: refreshToken))
                    } else {
                        Result.failure(Exception("トークンリフレッシュ応答が無効です"))
                    }
                } else {
                    Result.failure(Exception("トークンリフレッシュに失敗しました: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun clearAuth() {
        tokenManager.clearAllTokens()
        cachedToken = null
    }
    
    override suspend fun getOAuth2Session(sessionId: String): Response<LoginResponse> {
        return withContext(Dispatchers.IO) {
            apiService.getOAuth2Session(sessionId)
        }
    }
    
    override suspend fun saveToken(token: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.saveAccessToken(token)
                cachedToken = token
            } else {
                throw SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
            }
        } catch (e: SecurityException) {
            throw e
        }
    }
    
    override suspend fun saveUserId(userId: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.saveUserId(userId)
            } else {
                throw SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
            }
        } catch (e: SecurityException) {
            throw e
        }
    }
    
    override suspend fun saveRefreshToken(refreshToken: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.saveRefreshToken(refreshToken)
            } else {
                throw SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
            }
        } catch (e: SecurityException) {
            throw e
        }
    }
    

    private fun parseAuthErrorResponse(response: Response<AuthDto.LoginResponse>): Exception {
        val errorBody = response.errorBody()?.string()
        
        if (!errorBody.isNullOrEmpty()) {
            try {
                // AuthDto.ErrorResponseを使用してデシリアライズ
                val gson = com.google.gson.Gson()
                val errorResponse = gson.fromJson(errorBody, AuthDto.ErrorResponse::class.java)
                
                val errorMessage = errorResponse.error ?: errorResponse.message ?: errorBody
                
                return when (response.code()) {
                    401 -> BadCredentialsException(errorMessage, errorResponse.remainingAttempts)
                    423 -> AccountLockedException(errorMessage, errorResponse.lockoutTimeRemaining)
                    else -> AuthenticationFailedException(errorMessage)
                }
            } catch (e: Exception) {
                // JSON解析失敗時は既存のロジックにフォールバック
                val errorMessage = try {
                    val json = JSONObject(errorBody)
                    json.optString("error", errorBody)
                } catch (ex: Exception) {
                    errorBody
                }
                
                return when (response.code()) {
                    401 -> BadCredentialsException(errorMessage, null)
                    423 -> AccountLockedException(errorMessage, null)
                    else -> AuthenticationFailedException(errorMessage)
                }
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "ユーザー名またはパスワードが正しくありません"
                423 -> "アカウントがロックされています"
                else -> "ログインに失敗しました"
            }
            
            return when (response.code()) {
                401 -> BadCredentialsException(errorMessage, null)
                423 -> AccountLockedException(errorMessage, null)
                else -> AuthenticationFailedException(errorMessage)
            }
        }
    }

    /**
     * JWT トークンの包括的検証
     * @param token JWTトークン
     * @return トークンが有効な場合はtrue
     */
    private fun isTokenValid(token: String): Boolean {
        return try {
            // 包括的なJWT検証を実行
            com.sbm.application.data.security.JWTValidator.validateToken(token)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * AuthInterceptor専用のリフレッシュメソッド
     * @return 成功した場合true、失敗した場合false
     */
    private suspend fun refreshTokenForInterceptor(): Boolean {
        return try {
            val result = refreshToken()
            result.isSuccess
        } catch (e: Exception) {
            com.sbm.application.data.security.SecureLogger.error("AuthRepository", "インターセプターからのリフレッシュ失敗", e)
            false
        }
    }


}