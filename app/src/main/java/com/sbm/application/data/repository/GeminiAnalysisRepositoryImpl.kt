package com.sbm.application.data.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.sbm.application.data.local.ConfigManager
import com.sbm.application.data.remote.GeminiApiService
import com.sbm.application.data.remote.GeminiPromptTemplate
import com.sbm.application.data.remote.dto.GeminiInsightDto
import com.sbm.application.data.remote.dto.GeminiRequest
import com.sbm.application.data.remote.dto.Content
import com.sbm.application.data.remote.dto.Part
import com.sbm.application.data.remote.dto.GenerationConfig
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AIAnalysisRepository
import com.sbm.application.domain.service.AIPromptGenerator
import javax.inject.Inject

class GeminiAnalysisRepositoryImpl @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val configManager: ConfigManager,
    private val gson: Gson,
    private val promptGenerator: AIPromptGenerator
) : AIAnalysisRepository {
    
    override suspend fun generateInsight(request: AIAnalysisRequest): Result<AIInsightResponse> {
        return generateInsight(request, AIAnalysisConfig())
    }
    
    override suspend fun generateInsight(request: AIAnalysisRequest, config: AIAnalysisConfig): Result<AIInsightResponse> {
        return try {
            val apiKey = configManager.getGeminiApiKey()
                ?: return Result.failure(AIAnalysisError.ApiKeyNotSet)
            
            val prompt = promptGenerator.generateLifeAnalysisPrompt(request, config)
            val geminiRequest = GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 2048
                )
            )
            
            val startTime = System.currentTimeMillis()
            val promptSizeBytes = prompt.toByteArray(Charsets.UTF_8).size
            
            val response = geminiApiService.generateContent(apiKey, geminiRequest)
            val processingTime = System.currentTimeMillis() - startTime
            
            val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return Result.failure(AIAnalysisError.ApiResponseError)
            
            val insight = parseGeminiResponse(content, request.startDate, request.endDate)
            
            val responseData = AIInsightResponse(
                success = true,
                data = insight,
                error = null,
                metadata = ResponseMetadata(
                    apiVersion = "gemini-1.5-flash",
                    processingTimeMs = processingTime,
                    tokensUsed = response.usageMetadata?.totalTokenCount,
                    remainingQuota = null
                )
            )
            
            
            Result.success(responseData)
            
        } catch (e: retrofit2.HttpException) {
            val errorType = when (e.code()) {
                401 -> "InvalidApiKey"
                429 -> "RateLimitExceeded"
                else -> "HttpError_${e.code()}"
            }
            
            when (e.code()) {
                401 -> Result.failure(AIAnalysisError.InvalidApiKey)
                429 -> Result.failure(AIAnalysisError.RateLimitExceeded)
                else -> Result.failure(AIAnalysisError.fromThrowable(e))
            }
        } catch (e: Exception) {
            Result.failure(AIAnalysisError.fromThrowable(e))
        }
    }
    
    override suspend fun isConfigured(): Boolean {
        return configManager.getGeminiApiKey()?.isNotEmpty() == true
    }
    
    override suspend fun getConfigurationStatus(): ConfigurationStatus {
        val apiKey = configManager.getGeminiApiKey()
        
        return when {
            apiKey.isNullOrEmpty() -> ConfigurationStatus.notConfigured("Gemini APIキーが設定されていません")
            apiKey.length < 10 -> ConfigurationStatus.invalidCredentials("gemini", "APIキーが無効です")
            else -> ConfigurationStatus.configured("gemini")
        }
    }
    
    private fun parseGeminiResponse(jsonString: String, startDate: String, endDate: String): AIInsight {
        return try {
            // Remove any markdown formatting if present - more robust cleaning
            val cleanJson = jsonString
                .removePrefix("```json")
                .removeSuffix("```")
                .replace(Regex("^```json\n?"), "") // Remove ```json at start
                .replace(Regex("\n?```$"), "")     // Remove ``` at end
                .trim()
            
            val dto = gson.fromJson(cleanJson, GeminiInsightDto::class.java)
            
            AIInsight(
                id = 0L,
                userId = 0L,
                startDate = startDate,
                endDate = endDate,
                summary = dto.summary,
                moodAnalysis = dto.moodAnalysis,
                activityAnalysis = dto.activityAnalysis,
                recommendations = dto.recommendations,
                highlights = dto.highlights,
                motivationalMessage = dto.motivationalMessage,
                createdAt = System.currentTimeMillis().toString()
            )
        } catch (e: JsonSyntaxException) {
            throw AIAnalysisError.ApiResponseError
        } catch (e: Exception) {
            throw AIAnalysisError.UnknownError("AI応答の解析に失敗: ${e.message}")
        }
    }
}