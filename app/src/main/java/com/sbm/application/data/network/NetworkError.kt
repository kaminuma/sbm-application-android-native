package com.sbm.application.data.network

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * ネットワークエラーの種類を分類するためのsealed class
 */
sealed class NetworkError(
    message: String, 
    cause: Throwable? = null
) : Exception(message, cause) {
    
    // エラーメッセージを取得するためのプロパティ
    val errorMessage: String = message
    
    // ネットワーク接続エラー（インターネット接続なし）
    data class NoConnection(val error: Throwable) : NetworkError(
        "インターネット接続がありません。ネットワーク設定を確認してください。",
        error
    )
    
    // タイムアウトエラー
    data class Timeout(val error: Throwable) : NetworkError(
        "リクエストがタイムアウトしました。時間をおいて再試行してください。",
        error
    )
    
    // サーバーエラー（500系）
    data class ServerError(val code: Int, val error: Throwable) : NetworkError(
        "サーバーエラーが発生しました。しばらく時間をおいて再試行してください。(エラーコード: $code)",
        error
    )
    
    // クライアントエラー（400系、403は除く）
    data class ClientError(val code: Int, val error: Throwable) : NetworkError(
        "リクエストエラーが発生しました。(エラーコード: $code)",
        error
    )
    
    // 認証エラー（401, 403）
    data class AuthError(val code: Int, val error: Throwable) : NetworkError(
        "認証エラーが発生しました。再度ログインしてください。",
        error
    )
    
    // 未知のエラー
    data class Unknown(val error: Throwable) : NetworkError(
        "予期しないエラーが発生しました。",
        error
    )
    
    companion object {
        /**
         * 例外をNetworkErrorに変換する
         */
        fun fromThrowable(throwable: Throwable): NetworkError {
            return when (throwable) {
                is UnknownHostException -> NoConnection(throwable)
                is SocketTimeoutException -> Timeout(throwable)
                is IOException -> {
                    if (throwable.message?.contains("timeout", ignoreCase = true) == true) {
                        Timeout(throwable)
                    } else {
                        NoConnection(throwable)
                    }
                }
                is HttpException -> {
                    when (throwable.code()) {
                        in 401..403 -> AuthError(throwable.code(), throwable)
                        in 400..499 -> ClientError(throwable.code(), throwable)
                        in 500..599 -> ServerError(throwable.code(), throwable)
                        else -> Unknown(throwable)
                    }
                }
                else -> Unknown(throwable)
            }
        }
    }
}
