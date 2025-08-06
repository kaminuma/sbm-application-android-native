package com.sbm.application.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sbm.application.BuildConfig
import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AuthDto
import com.sbm.application.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {
    
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences(context)
    }
    private var cachedToken: String? = null
    
    override suspend fun login(username: String, password: String): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(
                    AuthDto.LoginRequest(username = username, password = password)
                )
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    // デバッグ用ログ出力（セキュリティ考慮）
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthRepository", "Login API Response: [REDACTED FOR SECURITY]")
                        Log.d("AuthRepository", "Response code: ${response.code()}")
                    }
                    
                    if (loginResponse != null && !loginResponse.token.isNullOrEmpty() && !loginResponse.userId.isNullOrEmpty()) {
                        // トークンを保存
                        sharedPreferences.edit()
                            .putString("auth_token", loginResponse.token)
                            .putString("user_id", loginResponse.userId)
                            .putString("refresh_token", loginResponse.refreshToken)
                            .apply()
                        
                        cachedToken = loginResponse.token
                        
                        Result.success(Pair(loginResponse.token!!, loginResponse.userId!!))
                    } else {
                        Result.failure(Exception("ログインの応答が無効です: ユーザーデータが見つかりません"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (BuildConfig.DEBUG) {
                        Log.e("AuthRepository", "Login API Error: ${response.code()} - ${response.message()}")
                        Log.e("AuthRepository", "Login Error Body: $errorBody")
                    }
                    Result.failure(Exception("ログインに失敗しました: ${response.message()} (${response.code()})"))
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("AuthRepository", "Login API Exception: ${e.message}", e)
                }
                Result.failure(Exception("ログイン処理中にエラーが発生しました: ${e.message}"))
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
                    if (BuildConfig.DEBUG) {
                        Log.d("AuthRepository", "Register API Response: [REDACTED FOR SECURITY]")
                        Log.d("AuthRepository", "Register response code: ${response.code()}")
                    }
                    
                    if (registerResponse != null) {
                        // 登録成功の場合、成功のダミー値を返す（自動ログインは行わない）
                        if (BuildConfig.DEBUG) {
                            Log.d("AuthRepository", "Registration successful")
                        }
                        Result.success(Pair("registration_success", "registration_success"))
                    } else {
                        Result.failure(Exception("登録の応答が無効です: レスポンスボディが空です"))
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.e("AuthRepository", "Register API Error: ${response.code()} - ${response.message()}")
                    }
                    Result.failure(Exception("登録に失敗しました: ${response.message()} (${response.code()})"))
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("AuthRepository", "Register API Exception: ${e.message}", e)
                }
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
        val storedToken = sharedPreferences.getString("auth_token", null)
        cachedToken = storedToken
        return !storedToken.isNullOrEmpty()
    }
    
    override suspend fun getStoredToken(): String? {
        val storedToken = sharedPreferences.getString("auth_token", null)
        cachedToken = storedToken
        return storedToken
    }
    
    override suspend fun getStoredUserId(): String? {
        return sharedPreferences.getString("user_id", null)
    }
    
    override suspend fun getStoredRefreshToken(): String? {
        return sharedPreferences.getString("refresh_token", null)
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
                        // 新しいトークンを保存
                        sharedPreferences.edit()
                            .putString("auth_token", refreshResponse.token)
                            .putString("refresh_token", refreshResponse.refreshToken ?: refreshToken)
                            .apply()
                        
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
        sharedPreferences.edit()
            .remove("auth_token")
            .remove("user_id")
            .remove("refresh_token")
            .apply()
        cachedToken = null
    }
    
    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "encrypted_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }
}