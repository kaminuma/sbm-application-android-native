package com.sbm.application.domain.model

data class AIAnalysisRequest(
    val startDate: String,
    val endDate: String,
    val moodRecords: List<MoodRecord>,
    val activities: List<Activity>,
    val periodSummary: PeriodSummary
) {
    init {
        // バリデーション追加
        require(startDate.isNotBlank()) { "Start date cannot be blank" }
        require(endDate.isNotBlank()) { "End date cannot be blank" }
        require(isValidDateFormat(startDate)) { "Invalid start date format: $startDate" }
        require(isValidDateFormat(endDate)) { "Invalid end date format: $endDate" }
        require(isValidDateRange(startDate, endDate)) { "End date must be after or equal to start date" }
        require(moodRecords.isNotEmpty() || activities.isNotEmpty()) { 
            "At least one mood record or activity is required for analysis" 
        }
    }
    
    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            java.time.LocalDate.parse(date, formatter)
            true
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }
    
    private fun isValidDateRange(start: String, end: String): Boolean {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = java.time.LocalDate.parse(start, formatter)
            val endDate = java.time.LocalDate.parse(end, formatter)
            !endDate.isBefore(startDate)
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }
}

