package com.sbm.application.domain.model

data class AIInsight(
    val id: Long,
    val userId: Long,
    val startDate: String,
    val endDate: String,
    val summary: String,
    val moodAnalysis: String,
    val activityAnalysis: String,
    val recommendations: List<String>,
    val highlights: List<String>,
    val motivationalMessage: String,
    val createdAt: String
) {
    companion object {
        fun createDefault(): AIInsight {
            return AIInsight(
                id = 0L,
                userId = 0L,
                startDate = "",
                endDate = "",
                summary = "",
                moodAnalysis = "",
                activityAnalysis = "",
                recommendations = emptyList(),
                highlights = emptyList(),
                motivationalMessage = "",
                createdAt = System.currentTimeMillis().toString()
            )
        }
    }
}