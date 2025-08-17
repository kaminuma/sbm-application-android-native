package com.sbm.application.data.metrics

import android.content.Context
import android.content.SharedPreferences
// import android.util.Log // Removed for production
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sbm.application.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI分析のパフォーマンスメトリクスを収集・分析するクラス
 * プロキシ版と直接版の比較、パフォーマンス監視を行う
 */
@Singleton
class AIAnalysisMetrics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "AIAnalysisMetrics"
        private const val PREF_NAME = "ai_analysis_metrics"
        private const val MAX_STORED_METRICS = 100 // 最大保存件数
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val _currentMetrics = MutableStateFlow<List<AnalysisMetric>>(emptyList())
    val currentMetrics: StateFlow<List<AnalysisMetric>> = _currentMetrics.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadStoredMetrics()
    }

    /**
     * AI分析の実行時間とメタデータを記録
     */
    fun recordAnalysis(
        implementationType: ImplementationType,
        requestData: AnalysisRequestData,
        result: AnalysisResult
    ) {


        val metric = AnalysisMetric(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            implementationType = implementationType,
            requestData = requestData,
            result = result
        )

        // メモリに追加
        val currentList = _currentMetrics.value.toMutableList()
        currentList.add(0, metric) // 最新を先頭に追加
        
        // 最大件数を超えた場合は古いものを削除
        if (currentList.size > MAX_STORED_METRICS) {
            currentList.subList(MAX_STORED_METRICS, currentList.size).clear()
        }
        
        _currentMetrics.value = currentList

        // 永続化
        saveMetricsToStorage(currentList)

            }

    /**
     * 成功した分析のメトリクスを記録
     */
    fun recordSuccess(
        implementationType: ImplementationType,
        startDate: String,
        endDate: String,
        processingTimeMs: Long,
        dataSize: DataSize,
        responseSize: Int = 0,
        tokenUsed: Int? = null
    ) {
        recordAnalysis(
            implementationType = implementationType,
            requestData = AnalysisRequestData(
                startDate = startDate,
                endDate = endDate,
                dataSize = dataSize
            ),
            result = AnalysisResult.Success(
                processingTimeMs = processingTimeMs,
                responseSize = responseSize,
                tokensUsed = tokenUsed
            )
        )
    }

    /**
     * 失敗した分析のメトリクスを記録
     */
    fun recordFailure(
        implementationType: ImplementationType,
        startDate: String,
        endDate: String,
        processingTimeMs: Long,
        dataSize: DataSize,
        errorType: String,
        errorMessage: String
    ) {
        recordAnalysis(
            implementationType = implementationType,
            requestData = AnalysisRequestData(
                startDate = startDate,
                endDate = endDate,
                dataSize = dataSize
            ),
            result = AnalysisResult.Failure(
                processingTimeMs = processingTimeMs,
                errorType = errorType,
                errorMessage = errorMessage
            )
        )
    }

    /**
     * パフォーマンス統計を取得
     */
    fun getPerformanceStats(): PerformanceStats {
        val metrics = _currentMetrics.value
        
        if (metrics.isEmpty()) {
            return PerformanceStats.empty()
        }

        val proxyMetrics = metrics.filter { it.implementationType == ImplementationType.BACKEND_PROXY }
        val directMetrics = metrics.filter { it.implementationType == ImplementationType.GEMINI_DIRECT }

        val proxySuccessful = proxyMetrics.filter { it.result is AnalysisResult.Success }
        val directSuccessful = directMetrics.filter { it.result is AnalysisResult.Success }

        return PerformanceStats(
            totalAnalyses = metrics.size,
            proxyAnalyses = proxyMetrics.size,
            directAnalyses = directMetrics.size,
            proxySuccessRate = if (proxyMetrics.isNotEmpty()) {
                proxySuccessful.size.toFloat() / proxyMetrics.size
            } else 0f,
            directSuccessRate = if (directMetrics.isNotEmpty()) {
                directSuccessful.size.toFloat() / directMetrics.size
            } else 0f,
            proxyAvgResponseTime = proxySuccessful.map { 
                (it.result as AnalysisResult.Success).processingTimeMs 
            }.average().takeIf { !it.isNaN() } ?: 0.0,
            directAvgResponseTime = directSuccessful.map { 
                (it.result as AnalysisResult.Success).processingTimeMs 
            }.average().takeIf { !it.isNaN() } ?: 0.0,
            commonErrors = getCommonErrors(metrics)
        )
    }

    /**
     * メトリクスをクリア
     */
    fun clearMetrics() {
        _currentMetrics.value = emptyList()
        preferences.edit().clear().apply()
        
            }

    /**
     * CSVエクスポート用のデータを取得
     */
    fun exportToCsv(): String {
        val metrics = _currentMetrics.value
        val sb = StringBuilder()
        
        // ヘッダー
        sb.appendLine("Timestamp,Implementation,StartDate,EndDate,DataSize,Success,ProcessingTime,ErrorType,ErrorMessage")
        
        // データ
        metrics.forEach { metric ->
            val timestamp = dateFormat.format(Date(metric.timestamp))
            val success = metric.result is AnalysisResult.Success
            val processingTime = when (metric.result) {
                is AnalysisResult.Success -> metric.result.processingTimeMs
                is AnalysisResult.Failure -> metric.result.processingTimeMs
            }
            val errorType = if (metric.result is AnalysisResult.Failure) metric.result.errorType else ""
            val errorMessage = if (metric.result is AnalysisResult.Failure) {
                metric.result.errorMessage.replace("\"", "\"\"") // CSV エスケープ
            } else ""
            
            sb.appendLine("$timestamp,${metric.implementationType.name},${metric.requestData.startDate},${metric.requestData.endDate},${metric.requestData.dataSize.totalItems},$success,$processingTime,$errorType,\"$errorMessage\"")
        }
        
        return sb.toString()
    }

    private fun loadStoredMetrics() {
        try {
            val json = preferences.getString("metrics", null) ?: return
            val type = object : TypeToken<List<AnalysisMetric>>() {}.type
            val metrics = gson.fromJson<List<AnalysisMetric>>(json, type) ?: emptyList()
            _currentMetrics.value = metrics.take(MAX_STORED_METRICS)
        } catch (e: Exception) {
                    }
    }

    private fun saveMetricsToStorage(metrics: List<AnalysisMetric>) {
        try {
            val json = gson.toJson(metrics)
            preferences.edit()
                .putString("metrics", json)
                .apply()
        } catch (e: Exception) {
                    }
    }

    // logMetric function removed for production

    private fun getCommonErrors(metrics: List<AnalysisMetric>): List<ErrorCount> {
        return metrics
            .mapNotNull { metric ->
                when (metric.result) {
                    is AnalysisResult.Failure -> metric.result.errorType
                    else -> null
                }
            }
            .groupBy { it }
            .map { (errorType, occurrences) ->
                ErrorCount(errorType, occurrences.size)
            }
            .sortedByDescending { it.count }
            .take(5) // 上位5つ
    }
}

// データクラス定義
data class AnalysisMetric(
    val id: String,
    val timestamp: Long,
    val implementationType: ImplementationType,
    val requestData: AnalysisRequestData,
    val result: AnalysisResult
)

data class AnalysisRequestData(
    val startDate: String,
    val endDate: String,
    val dataSize: DataSize
)

data class DataSize(
    val activitiesCount: Int,
    val moodRecordsCount: Int
) {
    val totalItems: Int get() = activitiesCount + moodRecordsCount
}

sealed class AnalysisResult {
    data class Success(
        val processingTimeMs: Long,
        val responseSize: Int,
        val tokensUsed: Int? = null
    ) : AnalysisResult()
    
    data class Failure(
        val processingTimeMs: Long,
        val errorType: String,
        val errorMessage: String
    ) : AnalysisResult()
}

enum class ImplementationType {
    BACKEND_PROXY,    // バックエンドAPI経由
    GEMINI_DIRECT     // 直接Gemini API
}

data class PerformanceStats(
    val totalAnalyses: Int,
    val proxyAnalyses: Int,
    val directAnalyses: Int,
    val proxySuccessRate: Float,
    val directSuccessRate: Float,
    val proxyAvgResponseTime: Double,
    val directAvgResponseTime: Double,
    val commonErrors: List<ErrorCount>
) {
    companion object {
        fun empty() = PerformanceStats(
            totalAnalyses = 0,
            proxyAnalyses = 0,
            directAnalyses = 0,
            proxySuccessRate = 0f,
            directSuccessRate = 0f,
            proxyAvgResponseTime = 0.0,
            directAvgResponseTime = 0.0,
            commonErrors = emptyList()
        )
    }
}

data class ErrorCount(
    val errorType: String,
    val count: Int
)