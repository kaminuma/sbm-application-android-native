package com.sbm.application.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkErrorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val _networkErrorFlow = MutableSharedFlow<NetworkError>(extraBufferCapacity = 1)
    val networkErrorFlow: SharedFlow<NetworkError> = _networkErrorFlow.asSharedFlow()
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * ネットワークエラーの種類
     */
    sealed class NetworkError(val message: String, val isRetryable: Boolean = false) {
        object NoInternet : NetworkError("インターネット接続を確認してください", true)
        object Timeout : NetworkError("接続がタイムアウトしました。再試行してください", true)
        object ServerUnavailable : NetworkError("サーバーにアクセスできません", true)
        data class ServerError(val code: Int) : NetworkError("サーバーエラーが発生しました (${code})", true)
        data class ClientError(val code: Int) : NetworkError("リクエストエラーが発生しました (${code})", false)
        data class UnknownError(val error: String) : NetworkError("不明なエラー: $error", false)
    }
    
    /**
     * 例外からネットワークエラーを判定し、適切なエラーメッセージを生成
     */
    fun handleException(exception: Throwable): NetworkError {
        val networkError = when (exception) {
            is UnknownHostException -> {
                if (isNetworkAvailable()) {
                    NetworkError.ServerUnavailable
                } else {
                    NetworkError.NoInternet
                }
            }
            is SocketTimeoutException -> NetworkError.Timeout
            is IOException -> {
                if (isNetworkAvailable()) {
                    NetworkError.ServerUnavailable
                } else {
                    NetworkError.NoInternet
                }
            }
            is HttpException -> {
                when (exception.code()) {
                    in 500..599 -> NetworkError.ServerError(exception.code())
                    in 400..499 -> NetworkError.ClientError(exception.code())
                    else -> NetworkError.UnknownError("HTTP ${exception.code()}: ${exception.message()}")
                }
            }
            else -> NetworkError.UnknownError(exception.message ?: "Unknown error")
        }
        
        // エラーをFlowに送信
        _networkErrorFlow.tryEmit(networkError)
        
        return networkError
    }
    
    fun isNetworkAvailable(): Boolean {
        return try {
            val activeNetwork: Network? = connectivityManager.activeNetwork
            val networkCapabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            // NET_CAPABILITY_VALIDATEDの要求を削除して、基本的なインターネット接続のみをチェック
            // これにより、ネットワーク検証が失敗してもHTTP通信が可能な場合は接続ありとみなす
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * エラーの種類に応じたリトライ可能性を判定
     */
    fun shouldRetry(error: NetworkError): Boolean {
        return error.isRetryable && isNetworkAvailable()
    }
    
    /**
     * リトライ処理
     */
    suspend fun <T> retryOperation(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(operation())
            } catch (e: Exception) {
                lastException = e
                val networkError = handleException(e)
                
                if (!shouldRetry(networkError) || attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                
                // リトライ前の待機
                kotlinx.coroutines.delay(delayMs * (attempt + 1))
            }
        }
        
        return Result.failure(lastException ?: Exception("Retry failed"))
    }
}