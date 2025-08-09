package com.sbm.application.domain.model

data class AIAnalysisConfig(
    val analysisFocus: AnalysisFocus = AnalysisFocus.BALANCED,
    val detailLevel: DetailLevel = DetailLevel.STANDARD,
    val responseStyle: ResponseStyle = ResponseStyle.FRIENDLY
    // 分析期間は直近1週間に固定（APIの安定性とユーザビリティを重視）
    // TODO: 将来的に比較分析機能を追加予定
    // val comparisonOption: ComparisonOption = ComparisonOption.NONE
)

// 分析期間は直近1週間に固定（コメントアウト - 将来的に選択可能にする場合は復活予定）
// enum class AnalysisPeriod(val displayName: String, val description: String) {
//     LAST_7_DAYS("直近1週間", "過去7日間のデータを分析")
// }

// TODO: 将来的に比較分析機能を追加予定
// enum class ComparisonOption(val displayName: String, val description: String) {
//     NONE("比較なし", "単独期間のみ分析"),
//     PREVIOUS_PERIOD("前回同期間", "前回の同じ期間と比較"),
//     LAST_MONTH("先月", "先月の同期間と比較"),
//     LAST_YEAR("去年同期", "去年の同期間と比較")
// }

enum class AnalysisFocus(val displayName: String, val description: String, val emoji: String) {
    MOOD_FOCUSED("気分重視", "気分の変化や傾向を詳しく分析", "😊"),
    ACTIVITY_FOCUSED("活動重視", "活動パターンや時間配分を詳しく分析", "⚡"),
    BALANCED("バランス", "気分と活動を均等に分析", "⚖️"),
    WELLNESS_FOCUSED("ウェルネス重視", "健康や生活習慣の改善に重点", "🌱")
}

enum class DetailLevel(val displayName: String, val description: String) {
    CONCISE("簡潔", "要点を短くまとめた分析"),
    STANDARD("標準", "バランスの取れた詳細度"),
    DETAILED("詳細", "深い洞察と具体的なアドバイス")
}

enum class ResponseStyle(val displayName: String, val description: String, val emoji: String) {
    FRIENDLY("親しみやすい", "温かく親近感のある口調", "😊"),
    PROFESSIONAL("専門的", "客観的で分析的な口調", "📊"),
    ENCOURAGING("励まし重視", "前向きで応援するような口調", "💪"),
    CASUAL("カジュアル", "気軽で親しみやすい口調", "😎")
}

// AI Provider (将来の拡張用)
enum class AIProvider(val displayName: String, val description: String) {
    GEMINI("Gemini", "Google Gemini AI"),
    CLAUDE("Claude", "Anthropic Claude AI"),
    GPT("ChatGPT", "OpenAI GPT"),
    CUSTOM_API("カスタムAPI", "独自のAI API")
}

// TODO: 将来的に比較分析機能を追加時に実装予定
// data class PromptContext(
//     val config: AIAnalysisConfig,
//     val request: AIAnalysisRequest,
//     val comparisonData: AIAnalysisRequest? = null
// )