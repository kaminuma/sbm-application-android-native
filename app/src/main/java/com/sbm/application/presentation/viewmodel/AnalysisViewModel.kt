package com.sbm.application.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.domain.repository.ActivityRepository
import com.sbm.application.domain.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CategoryData(
    val category: String,
    val totalHours: Float,
    val activityCount: Int
)

data class MoodTrendData(
    val date: String,
    val mood: Float
)

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val activities: List<Activity> = emptyList(),
    val moodRecords: List<MoodRecord> = emptyList(),
    val categoryData: List<CategoryData> = emptyList(),
    val moodTrendData: List<MoodTrendData> = emptyList(),
    val totalActivities: Int = 0,
    val averageMood: Float = 0f,
    val mostActiveCategory: String = ""
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val moodRepository: MoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun loadAnalysisData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load activities
                val activitiesResult = activityRepository.getActivities()
                val moodResult = moodRepository.getMoodRecords()
                
                var activities = emptyList<Activity>()
                var moodRecords = emptyList<MoodRecord>()
                
                activitiesResult.onSuccess { 
                    activities = it
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load activities"
                    )
                    return@launch
                }
                
                moodResult.onSuccess {
                    moodRecords = it
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load mood records"
                    )
                    return@launch
                }
                
                // Process data for analysis
                val categoryData = processCategoryData(activities)
                val moodTrendData = processMoodTrendData(moodRecords)
                val averageMood = moodRecords.map { it.mood }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
                val mostActiveCategory = categoryData.maxByOrNull { it.totalHours }?.category ?: ""
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activities = activities,
                    moodRecords = moodRecords,
                    categoryData = categoryData,
                    moodTrendData = moodTrendData,
                    totalActivities = activities.size,
                    averageMood = averageMood,
                    mostActiveCategory = mostActiveCategory
                )
                
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun loadAnalysisData(startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load activities
                val activitiesResult = activityRepository.getActivities()
                val moodResult = moodRepository.getMoodRecords()
                
                var activities = emptyList<Activity>()
                var moodRecords = emptyList<MoodRecord>()
                
                activitiesResult.onSuccess { allActivities ->
                    // Filter activities by date range
                    activities = allActivities.filter { activity ->
                        activity.date >= startDate && activity.date <= endDate
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load activities"
                    )
                    return@launch
                }
                
                moodResult.onSuccess { allMoodRecords ->
                    // Filter mood records by date range
                    moodRecords = allMoodRecords.filter { mood ->
                        mood.date >= startDate && mood.date <= endDate
                    }
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load mood records"
                    )
                    return@launch
                }
                
                // Process data for analysis
                val categoryData = processCategoryData(activities)
                val moodTrendData = processMoodTrendData(moodRecords)
                val averageMood = moodRecords.map { it.mood }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
                val mostActiveCategory = categoryData.maxByOrNull { it.totalHours }?.category ?: ""
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    activities = activities,
                    moodRecords = moodRecords,
                    categoryData = categoryData,
                    moodTrendData = moodTrendData,
                    totalActivities = activities.size,
                    averageMood = averageMood,
                    mostActiveCategory = mostActiveCategory
                )
                
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private fun processCategoryData(activities: List<Activity>): List<CategoryData> {
        return activities
            .groupBy { it.category }
            .map { (category, categoryActivities) ->
                val totalHours = categoryActivities.sumOf { activity ->
                    calculateActivityDuration(activity.start, activity.end)
                }.toFloat()
                CategoryData(
                    category = category,
                    totalHours = totalHours,
                    activityCount = categoryActivities.size
                )
            }
            .sortedByDescending { it.totalHours }
    }
    
    private fun processMoodTrendData(moodRecords: List<MoodRecord>): List<MoodTrendData> {
        return moodRecords
            .sortedBy { it.date }
            .takeLast(30) // Last 30 days
            .map { record ->
                MoodTrendData(
                    date = record.date,
                    mood = record.mood.toFloat()
                )
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
                // Handle case where activity spans midnight
                (24 * 60 - startMinutes) + endMinutes
            }
            
            durationMinutes / 60.0 // Convert to hours
        } catch (e: Exception) {
            0.0
        }
    }
}