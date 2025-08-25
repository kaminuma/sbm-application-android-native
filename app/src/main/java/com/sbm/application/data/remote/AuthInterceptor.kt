package com.sbm.application.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sbm.application.config.ApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    
    companion object {
        // 403/401エラー時のコールバック
        var onAuthError: ((Int, String) -> Unit)? = null
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        createEncryptedSharedPreferences(context)
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 認証が不要なエンドポイント（login, register）をスキップ
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") || url.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }
        
        return try {
            // トークンを取得（暗号化されたSharedPreferencesから直接）
            val token = sharedPreferences.getString("auth_token", null)
            
            val response = if (!token.isNullOrEmpty()) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                
                chain.proceed(authenticatedRequest)
            } else {
                chain.proceed(originalRequest)
            }
            
            // レスポンスコードをチェック
            when (response.code) {
                401, 403 -> {
                    // 認証エラーの場合、コールバック実行
                    onAuthError?.invoke(response.code, response.message)
                }
            }
            
            response
        } catch (e: Exception) {
            // エラーが発生した場合は元のリクエストを実行
            chain.proceed(originalRequest)
        }
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