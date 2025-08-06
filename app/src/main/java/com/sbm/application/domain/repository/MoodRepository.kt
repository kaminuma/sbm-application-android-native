package com.sbm.application.domain.repository

import com.sbm.application.domain.model.MoodRecord

interface MoodRepository {
    suspend fun getMoodRecords(): Result<List<MoodRecord>>
    suspend fun createMoodRecord(date: String, mood: Int, note: String?): Result<MoodRecord>
    suspend fun updateMoodRecord(date: String, mood: Int, note: String?): Result<MoodRecord>
    suspend fun deleteMoodRecord(date: String): Result<Unit>
}