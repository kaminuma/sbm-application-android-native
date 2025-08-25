package com.sbm.application.data.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * AI API専用のリトライインターセプター
 * 429エラー（Rate Limit）は通常のリトライ対象外とし、他のエラーのみリトライ
 */
class AIRetryInterceptor @Inject constructor(
    private val networkUtil: NetworkUtil
) : Interceptor {
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
        private const val BACKOFF_FACTOR = 2.0
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // AI関連のエンドポイントのみ対象とする
        if (!isAIEndpoint(request.url.encodedPath)) {
            return chain.proceed(request)
        }
        
        var response: Response? = null
        var exception: Exception? = null
        var currentDelay = INITIAL_DELAY_MS
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                response?.close() // 前回のレスポンスを閉じる
                response = chain.proceed(request)
                
                when {
                    response!!.isSuccessful -> {
                        return response!!
                    }
                    response!!.code == 429 -> {
                        // 429エラーはリトライしない（Rate Limit Exceeded）
                        return response!!
                    }
                    shouldRetry(response!!.code, attempt) -> {
                        // リトライ対象エラー（5xx系）
                        if (attempt < MAX_RETRIES - 1) {
                            response!!.close()
                            Thread.sleep(currentDelay)
                            currentDelay = (currentDelay * BACKOFF_FACTOR).toLong()
                                .coerceAtMost(MAX_DELAY_MS)
                        }
                    }
                    else -> {
                        // リトライしない（4xx系など）
                        return response!!
                    }
                }
                
            } catch (e: Exception) {
                exception = e
                when (e) {
                    is SocketTimeoutException,
                    is UnknownHostException -> {
                        // ネットワークエラーはリトライする
                        if (attempt < MAX_RETRIES - 1 && networkUtil.isNetworkAvailable()) {
                            Thread.sleep(currentDelay)
                            currentDelay = (currentDelay * BACKOFF_FACTOR).toLong()
                                .coerceAtMost(MAX_DELAY_MS)
                        }
                    }
                    else -> {
                        // その他の例外はリトライしない
                        throw e
                    }
                }
            }
        }
        
        // 最後の試行で失敗
        return response ?: createErrorResponse(exception)
    }
    
    private fun isAIEndpoint(path: String): Boolean {
        return path.contains("/ai/") || 
               path.contains("/analysis") ||
               path.contains("/gemini")
    }
    
    private fun shouldRetry(responseCode: Int, attempt: Int): Boolean {
        return responseCode in 500..599 && // 5xx系エラー
               attempt < MAX_RETRIES - 1 &&
               networkUtil.isNetworkAvailable()
    }
    
    private fun createErrorResponse(exception: Exception?): Response {
        val errorMessage = when (exception) {
            is SocketTimeoutException -> "接続タイムアウトが発生しました"
            is UnknownHostException -> "ネットワーク接続を確認してください"
            else -> "ネットワークエラーが発生しました: ${exception?.message}"
        }
        
        // ダミーのエラーレスポンスを作成
        return Response.Builder()
            .code(503) // Service Unavailable
            .message("Network Error")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .request(okhttp3.Request.Builder().url("http://localhost").build())
            .body(errorMessage.toResponseBody())
            .build()
    }
}