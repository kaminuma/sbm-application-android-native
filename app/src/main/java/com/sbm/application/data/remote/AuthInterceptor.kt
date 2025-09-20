package com.sbm.application.data.remote

import android.content.Context
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
    
    // セキュアなトークン管理に変更
    private val tokenManager: com.sbm.application.data.security.SecureTokenManager by lazy {
        com.sbm.application.data.security.SecureTokenManager(context)
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 認証が不要なエンドポイント（login, register）をスキップ
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") || url.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }
        
        return try {
            // セキュアなトークン取得
            val token = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.getToken()
            } else {
                // Android 6.0 未満の場合は認証なしで進行
                null
            }
            
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
        } catch (securityException: SecurityException) {
            // セキュリティエラーを適切にハンドリング
            onAuthError?.invoke(500, "認証システムエラー: ${securityException.message}")
            chain.proceed(originalRequest)
        } catch (e: Exception) {
            // エラーが発生した場合は元のリクエストを実行
            chain.proceed(originalRequest)
        }
    }
}