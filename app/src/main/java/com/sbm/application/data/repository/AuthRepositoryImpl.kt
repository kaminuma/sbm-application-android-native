package com.sbm.application.data.repository

import android.content.Context

import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AuthDto
import com.sbm.application.data.remote.dto.LoginResponse
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
                                tokenManager.saveToken(loginResponse.token!!)
                                // ユーザーIDは別途保存（暗号化対象外の基本情報）
                                saveUserIdSecurely(loginResponse.userId!!)
                                saveRefreshTokenSecurely(loginResponse.refreshToken)
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
                // API doesn't have logout endpoint, just clear local auth
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
                tokenManager.getToken()
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
        
        // 包括的なJWT検証
        return isTokenValid(storedToken)
    }
    
    override suspend fun getStoredToken(): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val token = tokenManager.getToken()
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
        return context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
            .getString("user_id", null)
    }
    
    override suspend fun getStoredRefreshToken(): String? {
        return context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
            .getString("refresh_token", null)
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
                                tokenManager.saveToken(refreshResponse.token!!)
                                saveRefreshTokenSecurely(refreshResponse.refreshToken ?: refreshToken)
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
        tokenManager.clearToken()
        clearUserIdSecurely()
        clearRefreshTokenSecurely()
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
                tokenManager.saveToken(token)
                cachedToken = token
            } else {
                throw SecurityException("Android 6.0 未満はセキュリティ上サポート対象外です")
            }
        } catch (e: SecurityException) {
            throw e
        }
    }
    
    override suspend fun saveUserId(userId: String) {
        saveUserIdSecurely(userId)
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
    
    // セキュアなユーザー情報保存のヘルパーメソッド
    private fun saveUserIdSecurely(userId: String) {
        // ユーザーIDは暗号化不要だが、専用のpreferencesに保存
        context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
            .edit()
            .putString("user_id", userId)
            .apply()
    }
    
    private fun saveRefreshTokenSecurely(refreshToken: String?) {
        if (refreshToken != null) {
            context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
                .edit()
                .putString("refresh_token", refreshToken)
                .apply()
        }
    }
    
    private fun clearUserIdSecurely() {
        context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
    
    private fun clearRefreshTokenSecurely() {
        context.getSharedPreferences("sbm_user_info", Context.MODE_PRIVATE)
            .edit()
            .remove("refresh_token")
            .apply()
    }


}