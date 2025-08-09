package com.sbm.application.domain.repository

import com.sbm.application.domain.model.AIAnalysisConfig
import com.sbm.application.domain.model.AIAnalysisRequest
import com.sbm.application.domain.model.AIInsightResponse
import com.sbm.application.domain.model.ConfigurationStatus

interface AIAnalysisRepository {
    suspend fun generateInsight(request: AIAnalysisRequest): Result<AIInsightResponse>
    suspend fun generateInsight(request: AIAnalysisRequest, config: AIAnalysisConfig): Result<AIInsightResponse>
    suspend fun isConfigured(): Boolean
    suspend fun getConfigurationStatus(): ConfigurationStatus
}