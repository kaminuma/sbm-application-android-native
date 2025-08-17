package com.sbm.application.data.repository

// import android.util.Log // Removed for production
import com.sbm.application.BuildConfig
import com.sbm.application.data.metrics.AIAnalysisMetrics
import com.sbm.application.data.metrics.DataSize
import com.sbm.application.data.metrics.ImplementationType
import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AIAnalysisRequestDto
import com.sbm.application.data.remote.dto.AnalysisFocus
import com.sbm.application.data.remote.dto.DetailLevel
import com.sbm.application.data.remote.dto.ResponseStyle
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AIAnalysisRepository
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import javax.inject.Inject

/**
 * バックエンドAPI経由でAI分析を行うRepository実装
 * 既存のGeminiAnalysisRepositoryImplと並存し、将来的にこちらに切り替える
 */
class ProxyAIAnalysisRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val metrics: AIAnalysisMetrics
) : AIAnalysisRepository {

    companion object {
        private const val TAG = "ProxyAIAnalysisRepository"
    }

    override suspend fun generateInsight(request: AIAnalysisRequest): Result<AIInsightResponse> {
        return generateInsight(request, AIAnalysisConfig())
    }

    override suspend fun generateInsight(
        request: AIAnalysisRequest, 
        config: AIAnalysisConfig
    ): Result<AIInsightResponse> = withContext(Dispatchers.IO) {
        
        val dataSize = DataSize(
            activitiesCount = request.activities.size,
            moodRecordsCount = request.moodRecords.size
        )
        val startTime = System.currentTimeMillis()
        
        try {
            // 認証トークンを取得
            val token = authRepository.getStoredToken()
            if (token == null) {
                val processingTime = System.currentTimeMillis() - startTime
                metrics.recordFailure(
                    implementationType = ImplementationType.BACKEND_PROXY,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    processingTimeMs = processingTime,
                    dataSize = dataSize,
                    errorType = "AuthenticationError",
                    errorMessage = "認証情報が見つかりません"
                )
                return@withContext Result.failure(
                    AIAnalysisError.ApiRequestError("認証情報が見つかりません。再ログインしてください。")
                )
            }

            
            // リクエストDTO作成
            val requestDto = AIAnalysisRequestDto(
                startDate = request.startDate,
                endDate = request.endDate,
                analysisFocus = mapAnalysisFocus(config.analysisFocus),
                detailLevel = mapDetailLevel(config.detailLevel),
                responseStyle = mapResponseStyle(config.responseStyle)
            )

            // バックエンドAPI呼び出し
            val requestStart = System.currentTimeMillis()
            val response = apiService.generateAIAnalysis("Bearer $token", requestDto)
            val processingTime = System.currentTimeMillis() - requestStart

            if (response.isSuccessful) {
                val responseDto = response.body()
                
                if (responseDto?.success == true && responseDto.data != null) {
                    // 成功レスポンスの処理
                    val insight = AIInsight(
                        id = 0L,
                        userId = authRepository.getStoredUserId()?.toLong() ?: 0L,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        summary = responseDto.data.overallSummary,
                        moodAnalysis = responseDto.data.moodInsights,
                        activityAnalysis = responseDto.data.activityInsights,
                        recommendations = parseRecommendations(responseDto.data.recommendations),
                        highlights = extractHighlights(responseDto.data.overallSummary),
                        motivationalMessage = generateMotivationalMessage(responseDto.data.recommendations),
                        createdAt = System.currentTimeMillis().toString()
                    )

                    val responseData = AIInsightResponse(
                        success = true,
                        data = insight,
                        error = null,
                        metadata = ResponseMetadata(
                            apiVersion = "backend-api-v1",
                            processingTimeMs = processingTime,
                            tokensUsed = null, // バックエンドから提供されていない
                            remainingQuota = null
                        )
                    )

                    // メトリクス記録（成功）
                    metrics.recordSuccess(
                        implementationType = ImplementationType.BACKEND_PROXY,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        processingTimeMs = processingTime,
                        dataSize = dataSize,
                        responseSize = responseDto.data.overallSummary.length +
                                     responseDto.data.moodInsights.length +
                                     responseDto.data.activityInsights.length +
                                     responseDto.data.recommendations.length
                    )

                    
                    Result.success(responseData)
                } else {
                    // エラーレスポンスの処理
                    val errorMessage = responseDto?.error ?: "分析結果の取得に失敗しました"
                    
                    // メトリクス記録（APIエラーレスポンス）
                    metrics.recordFailure(
                        implementationType = ImplementationType.BACKEND_PROXY,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        processingTimeMs = processingTime,
                        dataSize = dataSize,
                        errorType = "ApiErrorResponse",
                        errorMessage = errorMessage
                    )
                    
                                        
                    Result.success(
                        AIInsightResponse(
                            success = false,
                            data = null,
                            error = errorMessage
                        )
                    )
                }
            } else {
                // HTTP エラーレスポンスの処理
                handleHttpError(response.code(), response.message(), startTime, request, dataSize)
            }

        } catch (e: HttpException) {
            val elapsedMs = System.currentTimeMillis() - startTime
            recordFailureMetrics(e, "HttpException", elapsedMs, request, dataSize)
            handleHttpError(e.code(), e.message())
        } catch (e: UnknownHostException) {
            val elapsedMs = System.currentTimeMillis() - startTime
            recordFailureMetrics(e, "UnknownHostException", elapsedMs, request, dataSize)
                        Result.failure(AIAnalysisError.NetworkError("インターネット接続を確認してください"))
        } catch (e: SocketTimeoutException) {
            val elapsedMs = System.currentTimeMillis() - startTime
            recordFailureMetrics(e, "SocketTimeoutException", elapsedMs, request, dataSize)
                        Result.failure(AIAnalysisError.NetworkError("通信タイムアウトが発生しました。しばらく待ってから再試行してください"))
        } catch (e: IOException) {
            val elapsedMs = System.currentTimeMillis() - startTime
            recordFailureMetrics(e, "IOException", elapsedMs, request, dataSize)
                        Result.failure(AIAnalysisError.NetworkError("ネットワークエラーが発生しました: ${e.message}"))
        } catch (e: Exception) {
            val elapsedMs = System.currentTimeMillis() - startTime
            recordFailureMetrics(e, "UnexpectedException", elapsedMs, request, dataSize)
                        Result.failure(AIAnalysisError.fromThrowable(e))
        }
    }

    override suspend fun isConfigured(): Boolean {
        // バックエンドAPI経由の場合、認証情報があれば設定完了とみなす
        return authRepository.getStoredToken()?.isNotEmpty() == true
    }

    override suspend fun getConfigurationStatus(): ConfigurationStatus {
        val token = authRepository.getStoredToken()
        
        return when {
            token.isNullOrEmpty() -> ConfigurationStatus.notConfigured("認証情報が設定されていません")
            else -> ConfigurationStatus.configured("backend-api")
        }
    }

    /**
     * ドメインモデルの設定値をDTO用の値にマッピング
     */
    private fun mapAnalysisFocus(focus: com.sbm.application.domain.model.AnalysisFocus): String {
        return when (focus) {
            com.sbm.application.domain.model.AnalysisFocus.MOOD_FOCUSED -> AnalysisFocus.MOOD_FOCUSED.value
            com.sbm.application.domain.model.AnalysisFocus.ACTIVITY_FOCUSED -> AnalysisFocus.ACTIVITY_FOCUSED.value
            com.sbm.application.domain.model.AnalysisFocus.BALANCED -> AnalysisFocus.BALANCED.value
            com.sbm.application.domain.model.AnalysisFocus.WELLNESS_FOCUSED -> AnalysisFocus.WELLNESS_FOCUSED.value
        }
    }

    private fun mapDetailLevel(level: com.sbm.application.domain.model.DetailLevel): String {
        return when (level) {
            com.sbm.application.domain.model.DetailLevel.CONCISE -> DetailLevel.CONCISE.value
            com.sbm.application.domain.model.DetailLevel.STANDARD -> DetailLevel.STANDARD.value
            com.sbm.application.domain.model.DetailLevel.DETAILED -> DetailLevel.DETAILED.value
        }
    }

    private fun mapResponseStyle(style: com.sbm.application.domain.model.ResponseStyle): String {
        return when (style) {
            com.sbm.application.domain.model.ResponseStyle.FRIENDLY -> ResponseStyle.FRIENDLY.value
            com.sbm.application.domain.model.ResponseStyle.PROFESSIONAL -> ResponseStyle.PROFESSIONAL.value
            com.sbm.application.domain.model.ResponseStyle.ENCOURAGING -> ResponseStyle.ENCOURAGING.value
            com.sbm.application.domain.model.ResponseStyle.CASUAL -> ResponseStyle.CASUAL.value
        }
    }

    /**
     * HTTPエラーを適切なAIAnalysisErrorに変換
     */
    private fun handleHttpError(
        code: Int, 
        message: String, 
        startTime: Long, 
        request: AIAnalysisRequest, 
        dataSize: DataSize
    ): Result<AIInsightResponse> {
        val processingTime = System.currentTimeMillis() - startTime
        val errorType = "HTTP_$code"
        
        // メトリクス記録
        metrics.recordFailure(
            implementationType = ImplementationType.BACKEND_PROXY,
            startDate = request.startDate,
            endDate = request.endDate,
            processingTimeMs = processingTime,
            dataSize = dataSize,
            errorType = errorType,
            errorMessage = message
        )
        
        return handleHttpError(code, message)
    }
    
    private fun handleHttpError(code: Int, message: String): Result<AIInsightResponse> {
        val error = when (code) {
            400 -> AIAnalysisError.ApiRequestError("リクエスト形式に問題があります: $message")
            401 -> AIAnalysisError.InvalidApiKey
            403 -> AIAnalysisError.ApiRequestError("API利用権限がありません")
            404 -> AIAnalysisError.ApiRequestError("指定期間のデータが見つかりません")
            429 -> AIAnalysisError.RateLimitExceeded
            500, 502, 503 -> AIAnalysisError.NetworkError("サーバーに一時的な問題が発生しています。しばらく待ってから再試行してください")
            else -> AIAnalysisError.ApiRequestError("API呼び出しエラー: $message ($code)")
        }

        
        return Result.failure(error)
    }

    /**
     * 失敗メトリクスを記録するヘルパーメソッド
     */
    private fun recordFailureMetrics(
        exception: Exception,
        errorType: String,
        processingTime: Long,
        request: AIAnalysisRequest,
        dataSize: DataSize
    ) {
        metrics.recordFailure(
            implementationType = ImplementationType.BACKEND_PROXY,
            startDate = request.startDate,
            endDate = request.endDate,
            processingTimeMs = processingTime,
            dataSize = dataSize,
            errorType = errorType,
            errorMessage = exception.message ?: "Unknown error"
        )
    }

    /**
     * recommendations文字列をリストに分割
     * バックエンドAPIからは文字列で返されるため、適切にパース
     */
    private fun parseRecommendations(recommendations: String): List<String> {
        return recommendations.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { 
                // 番号や記号を取り除いて整形
                it.removePrefix("・")
                  .removePrefix("-")
                  .removePrefix("*")
                  .replaceFirst(Regex("^\\d+\\.\\s*"), "")
                  .trim()
            }
            .filter { it.isNotEmpty() }
            .take(5) // 最大5つに制限
    }

    /**
     * サマリーからハイライトを抽出
     */
    private fun extractHighlights(summary: String): List<String> {
        // 簡単な実装：句点で分割して重要そうなキーワードを含む文を抽出
        val keywords = listOf("改善", "向上", "良い", "注意", "重要", "おすすめ", "効果的")
        
        return summary.split("。")
            .filter { sentence ->
                keywords.any { keyword -> sentence.contains(keyword) }
            }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3) // 最大3つ
    }

    /**
     * 推奨事項から励ましのメッセージを生成
     */
    private fun generateMotivationalMessage(recommendations: String): String {
        val positiveKeywords = listOf("続ける", "維持", "良い", "素晴らしい", "順調")
        val hasPositive = positiveKeywords.any { recommendations.contains(it) }
        
        return if (hasPositive) {
            "素晴らしい取り組みです！この調子で続けていきましょう 💪"
        } else {
            "小さな変化から始めて、一歩ずつ前進していきましょう 🌱"
        }
    }
}