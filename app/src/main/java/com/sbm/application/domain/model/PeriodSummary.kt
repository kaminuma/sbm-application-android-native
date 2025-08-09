package com.sbm.application.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PeriodSummary(
    val startDate: String,
    val endDate: String,
    val dayCount: Int,
    val averageMood: Float,
    val moodRecordCount: Int,
    val totalActivities: Int,
    val topCategory: String,
    val activityHours: Float
) {
    companion object {
        fun create(
            startDate: String,
            endDate: String,
            moodRecords: List<MoodRecord>,
            activities: List<Activity>
        ): PeriodSummary {
            val dayCount = calculateDaysBetween(startDate, endDate)
            
            val averageMood = if (moodRecords.isNotEmpty()) {
                moodRecords.map { it.mood }.average().toFloat()
            } else 0f
            
            val topCategory = activities
                .groupBy { it.category }
                .maxByOrNull { it.value.size }?.key ?: ""
            
            val totalHours = activities.sumOf { activity ->
                calculateActivityDuration(activity.start, activity.end)
            }.toFloat()
            
            return PeriodSummary(
                startDate = startDate,
                endDate = endDate,
                dayCount = dayCount,
                averageMood = averageMood,
                moodRecordCount = moodRecords.size,
                totalActivities = activities.size,
                topCategory = topCategory,
                activityHours = totalHours
            )
        }
        
        private fun calculateDaysBetween(startDate: String, endDate: String): Int {
            return try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val start = LocalDate.parse(startDate, formatter)
                val end = LocalDate.parse(endDate, formatter)
                ChronoUnit.DAYS.between(start, end).toInt() + 1
            } catch (e: Exception) {
                1
            }
        }
        
        private fun calculateActivityDuration(start: String, end: String): Double {
            return try {
                val startParts = start.split(":")
                val endParts = end.split(":")
                
                val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
                
                val durationMinutes = if (endMinutes >= startMinutes) {
                    endMinutes - startMinutes
                } else {
                    (24 * 60 - startMinutes) + endMinutes
                }
                
                durationMinutes / 60.0
            } catch (e: Exception) {
                0.0
            }
        }
    }
}