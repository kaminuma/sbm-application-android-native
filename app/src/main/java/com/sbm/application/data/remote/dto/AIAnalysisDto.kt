package com.sbm.application.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sbm.application.domain.model.AIUsageInfo

/**
 * バックエンドAPI経由でのAI分析リクエスト用DTO
 * POST /api/v1/ai/analysis
 */
data class AIAnalysisRequestDto(
    @SerializedName("start_date") 
    val startDate: String,
    
    @SerializedName("end_date") 
    val endDate: String,
    
    @SerializedName("analysis_focus") 
    val analysisFocus: String,
    
    @SerializedName("detail_level") 
    val detailLevel: String,
    
    @SerializedName("response_style") 
    val responseStyle: String
)

/**
 * バックエンドAPI経由でのAI分析レスポンス用DTO
 */
data class AIAnalysisResponseDto(
    val success: Boolean,
    val error: String?,
    val data: AIInsightData?,
    @SerializedName("usage_info")
    val usageInfo: AIUsageInfo? = null
)

/**
 * AI分析結果データ
 */
data class AIInsightData(
    @SerializedName("overall_summary") 
    val overallSummary: String,
    
    @SerializedName("mood_insights") 
    val moodInsights: String,
    
    @SerializedName("activity_insights") 
    val activityInsights: String,
    
    @SerializedName("recommendations") 
    val recommendations: String
)

/**
 * 分析設定の列挙型定義
 */
enum class AnalysisFocus(val value: String) {
    MOOD_FOCUSED("MOOD_FOCUSED"),
    ACTIVITY_FOCUSED("ACTIVITY_FOCUSED"),
    BALANCED("BALANCED"),
    WELLNESS_FOCUSED("WELLNESS_FOCUSED")
}

enum class DetailLevel(val value: String) {
    CONCISE("CONCISE"),
    STANDARD("STANDARD"),
    DETAILED("DETAILED")
}

enum class ResponseStyle(val value: String) {
    FRIENDLY("FRIENDLY"),
    PROFESSIONAL("PROFESSIONAL"),
    ENCOURAGING("ENCOURAGING"),
    CASUAL("CASUAL")
}