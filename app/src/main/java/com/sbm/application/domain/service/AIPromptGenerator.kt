package com.sbm.application.domain.service

import com.sbm.application.domain.model.*

class AIPromptGenerator {
    
    fun generateLifeAnalysisPrompt(
        request: AIAnalysisRequest,
        config: AIAnalysisConfig = AIAnalysisConfig()
    ): String {
        val periodSection = generatePeriodSection(request, config.analysisPeriod)
        val comparisonSection = generateComparisonSection(config.comparisonOption)
        val focusSection = generateFocusSection(config.analysisFocus)
        val styleSection = generateStyleSection(config.responseStyle)
        val detailSection = generateDetailSection(config.detailLevel)
        
        val moodSummary = summarizeMoodData(request.moodRecords)
        val activitySummary = summarizeActivityData(request.activities)
        
        return """
        ${styleSection}
        
        ## 📊 分析設定
        - 期間: ${config.analysisPeriod.displayName} (${request.startDate} ～ ${request.endDate})
        - 焦点: ${config.analysisFocus.emoji} ${config.analysisFocus.displayName}
        - 詳細度: ${config.detailLevel.displayName}
        - 口調: ${config.responseStyle.emoji} ${config.responseStyle.displayName}
        ${comparisonSection}

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
    
    private fun generatePeriodSection(request: AIAnalysisRequest, period: AnalysisPeriod): String {
        return when (period) {
            AnalysisPeriod.CUSTOM -> "指定された期間"
            AnalysisPeriod.LAST_7_DAYS -> "直近1週間"
            AnalysisPeriod.LAST_30_DAYS -> "直近1ヶ月"
            AnalysisPeriod.LAST_90_DAYS -> "直近3ヶ月"
        }
    }
    
    private fun generateComparisonSection(comparison: ComparisonOption): String {
        return when (comparison) {
            ComparisonOption.NONE -> ""
            ComparisonOption.PREVIOUS_PERIOD -> "- 比較: 前回同期間との変化を重視して分析"
            ComparisonOption.LAST_MONTH -> "- 比較: 先月との変化を重視して分析"
            ComparisonOption.LAST_YEAR -> "- 比較: 去年同期との変化を重視して分析"
        }
    }
    
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