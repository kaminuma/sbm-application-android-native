package com.sbm.application.util

import android.content.Context
import com.sbm.application.data.metrics.AIAnalysisMetrics
import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.AIAnalysisRequestDto
import com.sbm.application.domain.model.*
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import com.sbm.application.domain.model.PeriodSummary

/**
 * テスト用のユーティリティクラス
 * 実際のバックエンドAPIとの統合テストをサポート
 */
object TestUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * テスト用のAI分析リクエストを作成
     */
    fun createTestAnalysisRequest(): AIAnalysisRequest {
        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7) // 7日前
        }
        val startStr = dateFormat.format(startDate.time)
        val endStr = dateFormat.format(endDate.time)
        val activities = createTestActivities()
        val moods = createTestMoodRecords()
        val summary = PeriodSummary.create(
            startDate = startStr,
            endDate = endStr,
            moodRecords = moods,
            activities = activities
        )

        return AIAnalysisRequest(
            startDate = startStr,
            endDate = endStr,
            activities = activities,
            moodRecords = moods,
            periodSummary = summary
        )
    }

    /**
     * テスト用の活動データを作成
     */
    private fun createTestActivities(): List<Activity> {
        return listOf(
            Activity(
                activityId = 1L,
                userId = 12345L,
                title = "朝の運動",
                contents = "公園でランニング30分",
                start = "07:00",
                end = "07:30",
                date = dateFormat.format(Calendar.getInstance().time),
                category = "運動",
                categorySub = "有酸素運動"
            ),
            Activity(
                activityId = 2L,
                userId = 12345L,
                title = "仕事",
                contents = "プロジェクト資料作成",
                start = "09:00",
                end = "12:00",
                date = dateFormat.format(Calendar.getInstance().time),
                category = "仕事",
                categorySub = "デスクワーク"
            ),
            Activity(
                activityId = 3L,
                userId = 12345L,
                title = "読書",
                contents = "技術書の勉強",
                start = "20:00",
                end = "21:00",
                date = dateFormat.format(Calendar.getInstance().time),
                category = "学習",
                categorySub = "読書"
            )
        )
    }

    /**
     * テスト用の気分データを作成
     */
    private fun createTestMoodRecords(): List<MoodRecord> {
        return listOf(
            MoodRecord(
                id = 1L,
                userId = 12345L,
                date = dateFormat.format(Calendar.getInstance().time),
                mood = 4,
                note = "今日は調子がいい",
                createdAt = "2024-08-10T10:00:00Z",
                updatedAt = "2024-08-10T10:00:00Z"
            ),
            MoodRecord(
                id = 2L,
                userId = 12345L,
                date = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time),
                mood = 3,
                note = "普通の日",
                createdAt = "2024-08-09T10:00:00Z",
                updatedAt = "2024-08-09T10:00:00Z"
            ),
            MoodRecord(
                id = 3L,
                userId = 12345L,
                date = dateFormat.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }.time),
                mood = 5,
                note = "素晴らしい一日だった",
                createdAt = "2024-08-08T10:00:00Z",
                updatedAt = "2024-08-08T10:00:00Z"
            )
        )
    }

    /**
     * バックエンドAPIの接続テストを実行
     * @param apiService APIサービスインスタンス
     * @param authRepository 認証リポジトリ
     * @return テスト結果のサマリー
     */
    suspend fun testBackendConnection(
        apiService: ApiService,
        authRepository: AuthRepository
    ): ConnectionTestResult {
        val results = mutableListOf<TestStepResult>()

        // 1. 認証チェック
        results.add(testAuthentication(authRepository))

        // 2. AI分析APIテスト
        if (results.last().success) {
            results.add(testAIAnalysisAPI(apiService, authRepository))
        }

        val allSuccess = results.all { it.success }
        val totalTime = results.sumOf { it.executionTimeMs }

        return ConnectionTestResult(
            success = allSuccess,
            totalExecutionTimeMs = totalTime,
            steps = results
        )
    }

    private suspend fun testAuthentication(authRepository: AuthRepository): TestStepResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val isLoggedIn = authRepository.isLoggedIn()
            val token = authRepository.getStoredToken()
            
            val success = isLoggedIn && !token.isNullOrEmpty()
            val message = if (success) {
                "認証OK - トークンあり"
            } else {
                "認証NG - ログインが必要"
            }
            
            TestStepResult(
                stepName = "認証チェック",
                success = success,
                message = message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestStepResult(
                stepName = "認証チェック",
                success = false,
                message = "認証エラー: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun testAIAnalysisAPI(
        apiService: ApiService,
        authRepository: AuthRepository
    ): TestStepResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val token = authRepository.getStoredToken()!!
            val testRequest = AIAnalysisRequestDto(
                startDate = "2024-08-03",
                endDate = "2024-08-10",
                analysisFocus = "BALANCED",
                detailLevel = "STANDARD",
                responseStyle = "FRIENDLY"
            )
            
            val response = apiService.generateAIAnalysis("Bearer $token", testRequest)
            
            val success = response.isSuccessful
            val message = if (success) {
                val body = response.body()
                "API呼び出し成功 - success: ${body?.success}, hasData: ${body?.data != null}"
            } else {
                "API呼び出し失敗 - HTTP ${response.code()}: ${response.message()}"
            }
            
            TestStepResult(
                stepName = "AI分析API",
                success = success,
                message = message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestStepResult(
                stepName = "AI分析API",
                success = false,
                message = "API例外: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * パフォーマンステストを実行
     */
    suspend fun runPerformanceTest(
        apiService: ApiService,
        authRepository: AuthRepository,
        iterations: Int = 5
    ): PerformanceTestResult {
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        
        repeat(iterations) { i ->
            val startTime = System.currentTimeMillis()
            
            try {
                val token = authRepository.getStoredToken()!!
                val testRequest = AIAnalysisRequestDto(
                    startDate = "2024-08-03",
                    endDate = "2024-08-10",
                    analysisFocus = "BALANCED",
                    detailLevel = "STANDARD",
                    responseStyle = "FRIENDLY"
                )
                
                val response = apiService.generateAIAnalysis("Bearer $token", testRequest)
                val executionTime = System.currentTimeMillis() - startTime
                
                if (response.isSuccessful) {
                    results.add(executionTime)
                } else {
                    errors.add("Iteration $i: HTTP ${response.code()}")
                }
                
            } catch (e: Exception) {
                errors.add("Iteration $i: ${e.message}")
            }
        }
        
        return PerformanceTestResult(
            totalIterations = iterations,
            successfulIterations = results.size,
            failedIterations = errors.size,
            averageResponseTimeMs = if (results.isNotEmpty()) results.average() else 0.0,
            minResponseTimeMs = results.minOrNull() ?: 0L,
            maxResponseTimeMs = results.maxOrNull() ?: 0L,
            errors = errors
        )
    }
}

// テスト結果用のデータクラス定義
data class TestStepResult(
    val stepName: String,
    val success: Boolean,
    val message: String,
    val executionTimeMs: Long
)

data class ConnectionTestResult(
    val success: Boolean,
    val totalExecutionTimeMs: Long,
    val steps: List<TestStepResult>
)

data class PerformanceTestResult(
    val totalIterations: Int,
    val successfulIterations: Int,
    val failedIterations: Int,
    val averageResponseTimeMs: Double,
    val minResponseTimeMs: Long,
    val maxResponseTimeMs: Long,
    val errors: List<String>
)