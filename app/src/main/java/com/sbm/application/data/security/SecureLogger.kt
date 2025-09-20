package com.sbm.application.data.security

import android.util.Log

/**
 * セキュアなログ出力管理クラス
 */
object SecureLogger {
    
    /**
     * デバッグログの出力（デバッグビルドのみ）
     */
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        if (com.sbm.application.BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    /**
     * エラーログの出力（本番環境では匿名化）
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (com.sbm.application.BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } else {
            // 本番環境では匿名化されたエラーのみ記録
            val anonymizedMessage = anonymizeErrorMessage(message)
            Log.e(tag, anonymizedMessage)
        }
    }
    
    /**
     * 情報ログの出力（デバッグビルドのみ）
     */
    fun info(tag: String, message: String) {
        if (com.sbm.application.BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }
    
    /**
     * 警告ログの出力（本番環境では匿名化）
     */
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (com.sbm.application.BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        } else {
            // 本番環境では匿名化されたメッセージのみ
            val anonymizedMessage = anonymizeErrorMessage(message)
            Log.w(tag, anonymizedMessage)
        }
    }
    
    /**
     * エラーメッセージの匿名化
     */
    private fun anonymizeErrorMessage(message: String): String {
        return message
            .replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "[IP_ADDRESS]") // IPアドレス
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[EMAIL]") // メールアドレス
            .replace(Regex("Bearer\\s+[A-Za-z0-9._-]+"), "Bearer [TOKEN]") // Bearerトークン
            .replace(Regex("password[\"']?\\s*[:=]\\s*[\"'][^\"']*[\"']"), "password: [REDACTED]") // パスワード
            .replace(Regex("token[\"']?\\s*[:=]\\s*[\"'][^\"']*[\"']"), "token: [REDACTED]") // 一般的なトークン
            .replace(Regex("key[\"']?\\s*[:=]\\s*[\"'][^\"']*[\"']"), "key: [REDACTED]") // APIキー等
    }
}