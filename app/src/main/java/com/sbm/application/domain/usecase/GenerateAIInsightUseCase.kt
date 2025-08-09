package com.sbm.application.domain.usecase

import com.sbm.application.domain.model.AIAnalysisConfig
import com.sbm.application.domain.model.AIAnalysisRequest
import com.sbm.application.domain.model.AIInsightResponse
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.domain.model.PeriodSummary
import com.sbm.application.domain.model.AIAnalysisError
import com.sbm.application.domain.repository.AIAnalysisRepository
import javax.inject.Inject

class GenerateAIInsightUseCase @Inject constructor(
    private val aiRepository: AIAnalysisRepository
) {
    suspend fun execute(
        startDate: String,
        endDate: String,
        activities: List<Activity>,
        moodRecords: List<MoodRecord>
    ): Result<AIInsightResponse> {
        return execute(startDate, endDate, activities, moodRecords, AIAnalysisConfig())
    }
    
    suspend fun execute(
        startDate: String,
        endDate: String,
        activities: List<Activity>,
        moodRecords: List<MoodRecord>,
        config: AIAnalysisConfig
    ): Result<AIInsightResponse> {
        try {
            // データ不足チェック
            if (activities.isEmpty() && moodRecords.isEmpty()) {
                return Result.failure(AIAnalysisError.InsufficientData)
            }
            
            // 設定チェック
            if (!aiRepository.isConfigured()) {
                return Result.failure(AIAnalysisError.ApiKeyNotSet)
            }
            
            // 期間サマリー生成
            val periodSummary = PeriodSummary.create(
                startDate = startDate,
                endDate = endDate,
                moodRecords = moodRecords,
                activities = activities
            )
            
            // AI分析リクエスト作成
            val request = AIAnalysisRequest(
                startDate = startDate,
                endDate = endDate,
                moodRecords = moodRecords,
                activities = activities,
                periodSummary = periodSummary
            )
            
            return aiRepository.generateInsight(request, config)
            
        } catch (e: Exception) {
            return Result.failure(AIAnalysisError.fromThrowable(e))
        }
    }
    
    suspend fun checkConfiguration(): Result<String> {
        return try {
            val status = aiRepository.getConfigurationStatus()
            if (status.isConfigured && status.hasValidCredentials) {
                Result.success("AI分析の設定が完了しています（${status.mode}）")
            } else {
                Result.failure(Exception(status.errorMessage ?: "AI設定が不完全です"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}