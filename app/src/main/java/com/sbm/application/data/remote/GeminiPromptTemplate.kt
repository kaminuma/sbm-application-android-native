package com.sbm.application.data.remote

import com.sbm.application.domain.model.AIAnalysisRequest
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.model.MoodRecord

object GeminiPromptTemplate {
    
    fun createLifeAnalysisPrompt(request: AIAnalysisRequest): String {
        val periodSummary = request.periodSummary
        val moodSummary = summarizeMoodData(request.moodRecords)
        val activitySummary = summarizeActivityData(request.activities)
        
        return """
        あなたは親しみやすくて優秀なライフコーチです。ユーザーの${periodSummary.dayCount}日間の生活データを分析し、
        建設的で励ましのあるアドバイスを日本語で提供してください。

        ## 📊 分析期間
        ${request.startDate} ～ ${request.endDate} (${periodSummary.dayCount}日間)

        ## 📈 データ概要
        ${if (request.moodRecords.isNotEmpty()) "気分記録: $moodSummary" else "気分記録: データなし"}
        ${if (request.activities.isNotEmpty()) "活動記録: $activitySummary" else "活動記録: データなし"}

        ## 📋 詳細データ
        ${if (request.moodRecords.isNotEmpty()) formatMoodRecords(request.moodRecords) else ""}
        ${if (request.activities.isNotEmpty()) formatActivities(request.activities) else ""}

        ## 🎯 出力指定
        以下の形式の**有効なJSON**で回答してください。改行や余分な文字、コメントは含めないでください：

        {
          "summary": "期間全体の総評（100文字以内、親しみやすく）",
          "moodAnalysis": "気分の傾向分析（150文字以内）", 
          "activityAnalysis": "活動パターン分析（150文字以内）",
          "recommendations": ["具体的で実行しやすいアドバイス1", "アドバイス2", "アドバイス3"],
          "highlights": ["良かった点1", "良かった点2"],
          "motivationalMessage": "前向きな励ましメッセージ（80文字以内）"
        }

        ## ✨ 回答方針
        - 優しく親しみやすい口調（です・ます調）
        - 具体的で実行しやすい提案
        - ポジティブな面を強調
        - データが少ない場合は「データが限られているため推測になります」と伝える
        - JSON形式を厳密に守る（ダブルクォート使用、エスケープ処理）
        - 改行文字や特殊文字を避ける
        """.trimIndent()
    }
    
    private fun summarizeMoodData(moodRecords: List<MoodRecord>): String {
        if (moodRecords.isEmpty()) return "なし"
        
        val avgMood = moodRecords.map { it.mood }.average()
        val recordCount = moodRecords.size
        val moodRange = "${moodRecords.minOf { it.mood }}～${moodRecords.maxOf { it.mood }}点"
        
        return "${recordCount}件の記録、平均${String.format("%.1f", avgMood)}点、範囲: $moodRange"
    }
    
    private fun summarizeActivityData(activities: List<Activity>): String {
        if (activities.isEmpty()) return "なし"
        
        val topCategory = activities.groupBy { it.category }
            .maxByOrNull { it.value.size }?.key ?: "不明"
        val totalCount = activities.size
        val uniqueCategories = activities.map { it.category }.distinct().size
        
        return "${totalCount}件の活動、${uniqueCategories}カテゴリ、最多: $topCategory"
    }
    
    private fun formatMoodRecords(moodRecords: List<MoodRecord>): String {
        if (moodRecords.isEmpty()) return ""
        
        val recentMoods = moodRecords.takeLast(7) // 最新7件まで
        val formattedMoods = recentMoods.joinToString(", ") { mood ->
            "${mood.date}: ${mood.mood}点${if (mood.note.isNotBlank()) " (${mood.note.take(20)})" else ""}"
        }
        
        return "### 気分記録詳細\n$formattedMoods\n"
    }
    
    private fun formatActivities(activities: List<Activity>): String {
        if (activities.isEmpty()) return ""
        
        val categoryGroups = activities.groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
            .take(5) // 上位5カテゴリ
        
        val formattedCategories = categoryGroups.joinToString("\n") { (category, acts) ->
            "- $category: ${acts.size}件"
        }
        
        return "### 活動記録詳細\n$formattedCategories\n"
    }
}