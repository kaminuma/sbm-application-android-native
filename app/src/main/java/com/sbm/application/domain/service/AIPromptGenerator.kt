package com.sbm.application.domain.service

import com.sbm.application.domain.model.*

class AIPromptGenerator {
    
    fun generateLifeAnalysisPrompt(
        request: AIAnalysisRequest,
        config: AIAnalysisConfig = AIAnalysisConfig()
    ): String {
        val focusSection = generateFocusSection(config.analysisFocus)
        // TODO: 将来的に比較分析機能を追加予定
        // val comparisonSection = generateComparisonSection(config.comparisonOption)
        val styleSection = generateStyleSection(config.responseStyle)
        val detailSection = generateDetailSection(config.detailLevel)
        
        val moodSummary = summarizeMoodData(request.moodRecords)
        val activitySummary = summarizeActivityData(request.activities)
        
        return """
        ${styleSection}
        
        ## 📊 分析設定
        - 期間: 直近1週間（固定） (${request.startDate} ～ ${request.endDate})
        - 焦点: ${config.analysisFocus.emoji} ${config.analysisFocus.displayName}
        - 詳細度: ${config.detailLevel.displayName}
        - 口調: ${config.responseStyle.emoji} ${config.responseStyle.displayName}
        
        // TODO: 将来的に比較分析機能を追加予定

        ## 📈 データ概要
        ${if (request.moodRecords.isNotEmpty()) "気分記録: $moodSummary" else "気分記録: データなし"}
        ${if (request.activities.isNotEmpty()) "活動記録: $activitySummary" else "活動記録: データなし"}

        ## 📋 詳細データ
        ${if (request.moodRecords.isNotEmpty()) formatMoodRecords(request.moodRecords) else ""}
        ${if (request.activities.isNotEmpty()) formatActivities(request.activities) else ""}

        ${focusSection}
        
        ${detailSection}

        ## 🎯 出力形式
        以下の形式の**有効なJSON**で回答してください：

        {
          "summary": "${getSummaryLength(config.detailLevel)}",
          "moodAnalysis": "${getAnalysisLength(config.detailLevel, config.analysisFocus == AnalysisFocus.MOOD_FOCUSED)}", 
          "activityAnalysis": "${getAnalysisLength(config.detailLevel, config.analysisFocus == AnalysisFocus.ACTIVITY_FOCUSED)}",
          "recommendations": ${getRecommendationCount(config.detailLevel)},
          "highlights": ${getHighlightCount(config.detailLevel)},
          "motivationalMessage": "${getMotivationalLength(config.detailLevel)}"
        }
        """.trimIndent()
    }
    
    // generatePeriodSection は削除（期間固定のため不要）
    
    // TODO: 将来的に比較分析機能を追加時に実装予定
    // private fun generateComparisonSection(comparison: ComparisonOption): String { ... }
    
    private fun generateFocusSection(focus: AnalysisFocus): String {
        return when (focus) {
            AnalysisFocus.MOOD_FOCUSED -> """
                ## 🎯 分析重点: 気分重視
                気分の変化、傾向、パターンを詳しく分析し、気分改善のための具体的なアドバイスを提供してください。
                活動との関連性も重視し、どの活動が気分にどう影響するかを明確にしてください。
            """.trimIndent()
            
            AnalysisFocus.ACTIVITY_FOCUSED -> """
                ## ⚡ 分析重点: 活動重視  
                活動パターン、時間配分、効率性を詳しく分析してください。
                どの活動が最も価値を生んでいるか、時間の使い方で改善できる点を具体的に指摘してください。
            """.trimIndent()
            
            AnalysisFocus.WELLNESS_FOCUSED -> """
                ## 🌱 分析重点: ウェルネス重視
                健康、生活習慣、ウェルビーイングの観点から分析してください。
                長期的な健康と幸福につながる生活パターンの改善提案を重視してください。
            """.trimIndent()
            
            AnalysisFocus.BALANCED -> """
                ## ⚖️ 分析重点: バランス重視
                気分と活動の両方を均等に分析し、全体的な生活バランスの観点からアドバイスしてください。
            """.trimIndent()
        }
    }
    
    private fun generateStyleSection(style: ResponseStyle): String {
        return when (style) {
            ResponseStyle.FRIENDLY -> """
                あなたは親しみやすくて優秀なライフコーチです。温かく親近感のある口調で、
                まるで親しい友人のようにユーザーの生活データを分析し、励ましのあるアドバイスを提供してください。
            """.trimIndent()
            
            ResponseStyle.PROFESSIONAL -> """
                あなたは専門的で客観的なライフアナリストです。データに基づいた冷静な分析と、
                科学的根拠のある実用的なアドバイスを、専門的だが理解しやすい口調で提供してください。
            """.trimIndent()
            
            ResponseStyle.ENCOURAGING -> """
                あなたは前向きで応援上手なメンターです。ユーザーの努力を認め、可能性を信じ、
                どんな状況でも希望と勇気を与えるような、励ましに満ちたアドバイスを提供してください。
            """.trimIndent()
            
            ResponseStyle.CASUAL -> """
                あなたは気軽で親しみやすいライフアドバイザーです。カジュアルで親しみやすい口調で、
                リラックスした雰囲気の中、実用的で取り入れやすいアドバイスを提供してください。
            """.trimIndent()
        }
    }
    
    private fun generateDetailSection(level: DetailLevel): String {
        return when (level) {
            DetailLevel.CONCISE -> """
                ## 📝 詳細レベル: 簡潔
                要点を絞り、短文で分かりやすく伝えてください。具体例は必要最小限に留めてください。
            """.trimIndent()
            
            DetailLevel.STANDARD -> """
                ## 📝 詳細レベル: 標準
                適度な詳細度で、理解しやすい説明と具体例を含めてください。
            """.trimIndent()
            
            DetailLevel.DETAILED -> """
                ## 📝 詳細レベル: 詳細
                深い洞察と豊富な具体例、ステップバイステップの詳しいアドバイスを提供してください。
                背景理由も含めて詳しく説明してください。
            """.trimIndent()
        }
    }
    
    private fun getSummaryLength(level: DetailLevel): String {
        return when (level) {
            DetailLevel.CONCISE -> "期間全体の総評（50文字以内）"
            DetailLevel.STANDARD -> "期間全体の総評（100文字以内）"
            DetailLevel.DETAILED -> "期間全体の総評（150文字以内）"
        }
    }
    
    private fun getAnalysisLength(level: DetailLevel, isFocused: Boolean): String {
        val baseLength = when (level) {
            DetailLevel.CONCISE -> if (isFocused) 100 else 80
            DetailLevel.STANDARD -> if (isFocused) 200 else 150
            DetailLevel.DETAILED -> if (isFocused) 300 else 200
        }
        return "分析内容（${baseLength}文字以内）"
    }
    
    private fun getRecommendationCount(level: DetailLevel): String {
        return when (level) {
            DetailLevel.CONCISE -> "[\"アドバイス1\", \"アドバイス2\"]"
            DetailLevel.STANDARD -> "[\"アドバイス1\", \"アドバイス2\", \"アドバイス3\"]"
            DetailLevel.DETAILED -> "[\"アドバイス1\", \"アドバイス2\", \"アドバイス3\", \"アドバイス4\", \"アドバイス5\"]"
        }
    }
    
    private fun getHighlightCount(level: DetailLevel): String {
        return when (level) {
            DetailLevel.CONCISE -> "[\"良かった点1\"]"
            DetailLevel.STANDARD -> "[\"良かった点1\", \"良かった点2\"]"
            DetailLevel.DETAILED -> "[\"良かった点1\", \"良かった点2\", \"良かった点3\"]"
        }
    }
    
    private fun getMotivationalLength(level: DetailLevel): String {
        return when (level) {
            DetailLevel.CONCISE -> "励ましメッセージ（40文字以内）"
            DetailLevel.STANDARD -> "励ましメッセージ（80文字以内）"
            DetailLevel.DETAILED -> "励ましメッセージ（120文字以内）"
        }
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
        
        val weekMoods = moodRecords // 過去1週間分の気分記録を全て使用（期間フィルタ済み）
        val formattedMoods = weekMoods.joinToString(", ") { mood ->
            "${mood.date}: ${mood.mood}点${if (mood.note.isNotBlank()) " (${mood.note.take(20)})" else ""}"
        }
        
        return "### 気分記録詳細\n$formattedMoods\n"
    }
    
    private fun formatActivities(activities: List<Activity>): String {
        if (activities.isEmpty()) return ""
        
        // 過去1週間分の活動を全て詳細表示（期間フィルタ済みのデータを全て使用）
        val weekActivities = activities
        val formattedActivities = weekActivities.joinToString("\n") { activity ->
            val timeRange = "${activity.start}-${activity.end}"
            val categoryInfo = if (activity.categorySub.isNullOrBlank()) {
                activity.category
            } else {
                "${activity.category}(${activity.categorySub})"
            }
            val contentInfo = if (activity.contents.isNullOrBlank()) {
                ""
            } else {
                " - ${activity.contents.take(30)}" // 内容を30文字まで
            }
            
            "${activity.date} ${timeRange}: ${activity.title} [$categoryInfo]$contentInfo"
        }
        
        // カテゴリ別サマリーも追加
        val categoryGroups = activities.groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
            .take(3) // 上位3カテゴリ
        
        val categorySummary = categoryGroups.joinToString(", ") { (category, acts) ->
            "$category: ${acts.size}件"
        }
        
        return """
### 活動記録詳細
$formattedActivities

### カテゴリ別概要
$categorySummary
"""
    }
}