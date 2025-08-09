package com.sbm.application.domain.model

sealed class AIAnalysisError : Exception() {
    // NetworkErrorはdata classに変更（詳細メッセージ対応のため）
    
    object ApiKeyNotSet : AIAnalysisError() {
        override val message: String = "AI設定でAPIキーを設定してください"
    }
    
    object InsufficientData : AIAnalysisError() {
        override val message: String = "分析するデータが不足しています"
    }
    
    object RateLimitExceeded : AIAnalysisError() {
        override val message: String = "利用制限に達しました。しばらく後でお試しください"
    }
    
    object InvalidApiKey : AIAnalysisError() {
        override val message: String = "APIキーが無効です。設定を確認してください"
    }
    
    object ApiResponseError : AIAnalysisError() {
        override val message: String = "AI応答の解析に失敗しました"
    }
    
    data class UnknownError(override val message: String) : AIAnalysisError()
    
    data class NetworkError(override val message: String) : AIAnalysisError()
    
    data class ApiRequestError(override val message: String) : AIAnalysisError()
    
    companion object {
        fun fromThrowable(throwable: Throwable): AIAnalysisError {
            return when {
                throwable is java.net.UnknownHostException || 
                throwable is java.net.SocketTimeoutException ||
                throwable is java.net.ConnectException -> NetworkError("ネットワーク接続を確認してください")
                
                throwable.message?.contains("API_KEY_INVALID") == true ||
                throwable.message?.contains("401") == true -> InvalidApiKey
                
                throwable.message?.contains("RATE_LIMIT_EXCEEDED") == true ||
                throwable.message?.contains("429") == true -> RateLimitExceeded
                
                throwable.message?.contains("insufficient") == true -> InsufficientData
                
                else -> UnknownError(throwable.message ?: "不明なエラーが発生しました")
            }
        }
    }
}