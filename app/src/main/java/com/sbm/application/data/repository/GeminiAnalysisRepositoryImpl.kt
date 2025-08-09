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
                    maxOutputTokens = 2048,
                    responseMimeType = "application/json"
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
            handleHttpException(e)
        } catch (e: java.net.UnknownHostException) {
            // ネットワーク接続なし
            Result.failure(AIAnalysisError.NetworkError("インターネット接続を確認してください"))
        } catch (e: java.net.SocketTimeoutException) {
            // タイムアウト
            Result.failure(AIAnalysisError.NetworkError("通信タイムアウトが発生しました。しばらく待ってから再試行してください"))
        } catch (e: java.io.IOException) {
            // その他のネットワークエラー
            Result.failure(AIAnalysisError.NetworkError("ネットワークエラーが発生しました: ${e.message}"))
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
            // Robust JSON extraction from markdown code blocks
            val cleanJson = extractJsonFromMarkdown(jsonString)
            
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
    
    /**
     * Markdownコードブロックから堅牢にJSONを抽出する
     * 複数のregex置換の代わりに、一度の解析でJSONを特定
     */
    private fun extractJsonFromMarkdown(text: String): String {
        val trimmedText = text.trim()
        
        // Case 1: ```json ... ``` パターン
        val jsonBlockPattern = """```(?:json)?\s*\n?(.*?)\n?```"""
        val jsonBlockRegex = Regex(jsonBlockPattern, RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonBlockRegex.find(trimmedText)
        if (jsonMatch != null) {
            return jsonMatch.groupValues[1].trim()
        }
        
        // Case 2: { ... } の単純JSONパターン
        val jsonStartIndex = trimmedText.indexOfFirst { it == '{' }
        val jsonEndIndex = trimmedText.indexOfLast { it == '}' }
        
        if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex <= jsonEndIndex) {
            return trimmedText.substring(jsonStartIndex, jsonEndIndex + 1).trim()
        }
        
        // Case 3: そのまま返す（フォールバック）
        return trimmedText
    }
    
    /**
     * HTTP例外を適切なAIAnalysisErrorに変換する
     */
    private fun handleHttpException(e: retrofit2.HttpException): Result<AIInsightResponse> {
        return when (e.code()) {
            401 -> Result.failure(AIAnalysisError.InvalidApiKey)
            429 -> Result.failure(AIAnalysisError.RateLimitExceeded) 
            400 -> {
                // リクエスト形式エラー（データ不足の可能性）
                val errorMessage = if (e.message()?.contains("insufficient data", ignoreCase = true) == true) {
                    "分析に必要なデータが不足しています。もう少しデータを追加してから再試行してください"
                } else {
                    "リクエスト形式に問題があります"
                }
                Result.failure(AIAnalysisError.ApiRequestError(errorMessage))
            }
            403 -> Result.failure(AIAnalysisError.ApiRequestError("API利用権限がありません"))
            404 -> Result.failure(AIAnalysisError.ApiRequestError("APIエンドポイントが見つかりません"))
            500, 502, 503 -> Result.failure(AIAnalysisError.NetworkError("サーバーに一時的な問題が発生しています。しばらく待ってから再試行してください"))
            else -> Result.failure(AIAnalysisError.fromThrowable(e))
        }
    }
}