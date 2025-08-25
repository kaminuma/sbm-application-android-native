package com.sbm.application.domain.model

import com.google.gson.annotations.SerializedName

/**
 * AI利用状況情報を表すデータクラス
 * サーバーからの/api/v1/ai/usage APIレスポンス用
 */
data class AIUsageInfo(
    @SerializedName("dailyUsed")
    val dailyUsed: Int,
    
    @SerializedName("dailyLimit")
    val dailyLimit: Int,
    
    @SerializedName("dailyRemaining")
    val dailyRemaining: Int,
    
    @SerializedName("monthlyUsed")
    val monthlyUsed: Int,
    
    @SerializedName("monthlyLimit")
    val monthlyLimit: Int,
    
    @SerializedName("monthlyRemaining")
    val monthlyRemaining: Int,
    
    @SerializedName("provider")
    val provider: String,
    
    @SerializedName("canUseToday")
    val canUseToday: Boolean,
    
    @SerializedName("nextResetDate")
    val nextResetDate: String,
    
    @SerializedName("debugMode")
    val debugMode: Boolean = false,
    
    @SerializedName("isDebugUser")
    val isDebugUser: Boolean = false,
    
    @SerializedName("limitsEnabled")
    val limitsEnabled: Boolean = true
) {
    /**
     * デバッグまたは制限無効化により無制限利用可能かどうか
     */
    val isUnlimited: Boolean
        get() = debugMode || isDebugUser || !limitsEnabled

    /**
     * 制限に近づいている場合の警告が必要かどうか
     */
    val needsWarning: Boolean
        get() = !isUnlimited && dailyRemaining <= 1

    /**
     * 利用状況の表示用文字列
     */
    val usageDisplayText: String
        get() = when {
            debugMode -> "🔧 デバッグモード: 無制限利用可能"
            isDebugUser -> "🔧 デバッグユーザー: 無制限利用可能"
            !limitsEnabled -> "制限機能無効: 無制限利用可能"
            else -> "今日のAI分析回数: ${dailyUsed}回 残り${dailyRemaining}回の分析が可能"
        }

    /**
     * プログレスバーの進捗率（0.0 - 1.0）
     */
    val progressRatio: Float
        get() = if (isUnlimited || dailyLimit == 0) 0f else (dailyUsed.toFloat() / dailyLimit.toFloat())
}

/**
 * AI利用制限到達例外
 */
class RateLimitExceededException(
    message: String,
    val usageInfo: AIUsageInfo? = null
) : Exception(message)

/**
 * AI利用状況取得エラー
 */
sealed class AIUsageError : Exception() {
    object NetworkError : AIUsageError()
    object AuthenticationError : AIUsageError()
    data class ApiError(val code: Int, override val message: String) : AIUsageError()
    data class UnknownError(override val message: String) : AIUsageError()
}