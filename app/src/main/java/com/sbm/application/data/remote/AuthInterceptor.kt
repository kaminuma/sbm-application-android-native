package com.sbm.application.data.remote

import android.content.Context
import com.sbm.application.data.security.SecureTokenManager
import com.sbm.application.data.security.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    companion object {
        // 認証エラー時のコールバック
        var onAuthError: ((Int, String) -> Unit)? = null
        // トークンリフレッシュコールバック（AuthRepositoryから設定）
        // 戻り値: Success=成功, RetryableFailure=リトライ可能, PermanentFailure=永続的失敗
        var refreshTokenCallback: (suspend () -> RefreshResult)? = null
        private const val TAG = "AuthInterceptor"
    }

    enum class RefreshResult {
        SUCCESS,           // リフレッシュ成功
        RETRYABLE_FAILURE, // 一時的なエラー（リトライ可能）
        PERMANENT_FAILURE  // リフレッシュトークン期限切れ（リトライ不可）
    }

    // セキュアなトークン管理
    private val tokenManager: SecureTokenManager by lazy {
        SecureTokenManager(context)
    }

    // 同時リフレッシュを防ぐための排他制御
    @Volatile
    private var isRefreshing = false

    // リフレッシュ頻度制限
    @Volatile
    private var lastRefreshTime = 0L
    @Volatile
    private var refreshCount = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 認証が不要なエンドポイントをスキップ
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") ||
            url.contains("/auth/register") ||
            url.contains("/auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        return try {
            // アクセストークン取得
            val accessToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                tokenManager.getAccessToken()
            } else {
                null
            }

            // 最初のリクエスト実行
            var response = if (!accessToken.isNullOrEmpty()) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                chain.proceed(authenticatedRequest)
            } else {
                chain.proceed(originalRequest)
            }

            // 401エラーの場合、リフレッシュを試行
            if (response.code == 401 && !isRefreshing && refreshTokenCallback != null && canRefresh()) {
                response.close()

                synchronized(this) {
                    if (!isRefreshing) {
                        isRefreshing = true

                        try {
                            // TODO: runBlockingの使用はOkHttpスレッドプールをブロックするため非推奨
                            // 将来的にAuthenticatorへの移行を検討 (Issue #XX)
                            val refreshResult = kotlinx.coroutines.runBlocking {
                                refreshTokenCallback?.invoke() ?: RefreshResult.PERMANENT_FAILURE
                            }

                            when (refreshResult) {
                                RefreshResult.SUCCESS -> {
                                    recordRefresh()
                                    // リフレッシュ成功：新しいトークンで再試行
                                    val newAccessToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        tokenManager.getAccessToken()
                                    } else {
                                        null
                                    }

                                    if (!newAccessToken.isNullOrEmpty()) {
                                        val retryRequest = originalRequest.newBuilder()
                                            .header("Authorization", "Bearer $newAccessToken")
                                            .build()
                                        response = chain.proceed(retryRequest)
                                        SecureLogger.debug(TAG, "トークンリフレッシュ後の再試行成功")
                                    } else {
                                        SecureLogger.warn(TAG, "リフレッシュ後のトークン取得に失敗")
                                        onAuthError?.invoke(401, "トークンリフレッシュ後のトークン取得に失敗")
                                    }
                                }
                                RefreshResult.RETRYABLE_FAILURE -> {
                                    // 一時的なエラー：1回だけリトライ
                                    SecureLogger.debug(TAG, "一時的なエラーのためリトライ")

                                    val retryResult = kotlinx.coroutines.runBlocking {
                                        kotlinx.coroutines.delay(500) // 短い遅延
                                        refreshTokenCallback?.invoke() ?: RefreshResult.PERMANENT_FAILURE
                                    }

                                    if (retryResult == RefreshResult.SUCCESS) {
                                        recordRefresh()
                                        val newAccessToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                            tokenManager.getAccessToken()
                                        } else {
                                            null
                                        }

                                        if (!newAccessToken.isNullOrEmpty()) {
                                            val retryRequest = originalRequest.newBuilder()
                                                .header("Authorization", "Bearer $newAccessToken")
                                                .build()
                                            response = chain.proceed(retryRequest)
                                            SecureLogger.debug(TAG, "リトライ後にリフレッシュ成功")
                                        } else {
                                            SecureLogger.warn(TAG, "リトライ後のトークン取得に失敗")
                                            onAuthError?.invoke(401, "認証エラー")
                                        }
                                    } else {
                                        // リトライしても失敗
                                        SecureLogger.warn(TAG, "リトライ後も失敗")
                                        onAuthError?.invoke(401, "ネットワークエラーのため認証に失敗しました")
                                    }
                                }
                                RefreshResult.PERMANENT_FAILURE -> {
                                    // リフレッシュトークン期限切れ：ログイン画面へ
                                    SecureLogger.warn(TAG, "リフレッシュトークンが無効")
                                    onAuthError?.invoke(401, "リフレッシュトークンが無効です")
                                }
                            }
                        } catch (e: Exception) {
                            SecureLogger.error(TAG, "リフレッシュ処理中にエラー", e)
                            onAuthError?.invoke(401, "認証エラー")
                        } finally {
                            isRefreshing = false
                        }
                    }
                }
            } else if (response.code == 401) {
                // リフレッシュコールバックが設定されていない場合
                SecureLogger.warn(TAG, "401エラーですが、リフレッシュコールバックが未設定")
                onAuthError?.invoke(401, "認証が必要です")
            } else if (response.code == 403) {
                // 権限エラー
                onAuthError?.invoke(403, "アクセス権限がありません")
            }

            response
        } catch (securityException: SecurityException) {
            SecureLogger.error(TAG, "認証システムエラー", securityException)
            onAuthError?.invoke(500, "認証システムエラー")
            chain.proceed(originalRequest)
        } catch (e: Exception) {
            SecureLogger.error(TAG, "インターセプターエラー", e)
            chain.proceed(originalRequest)
        }
    }

    /**
     * リフレッシュ頻度制限チェック
     * @return リフレッシュ可能な場合true
     */
    private fun canRefresh(): Boolean {
        val now = System.currentTimeMillis()

        // 1分間経過したらカウントリセット
        if (now - lastRefreshTime > 60000) {
            refreshCount = 0
            lastRefreshTime = now
        }

        // 1分間に3回まで
        val canRefresh = refreshCount < 3

        if (!canRefresh) {
            SecureLogger.warn(TAG, "リフレッシュ頻度制限に達しました ($refreshCount/3)")
        }

        return canRefresh
    }

    /**
     * リフレッシュ実行を記録
     */
    private fun recordRefresh() {
        refreshCount++
        SecureLogger.debug(TAG, "リフレッシュ実行記録: $refreshCount/3")
    }
}
