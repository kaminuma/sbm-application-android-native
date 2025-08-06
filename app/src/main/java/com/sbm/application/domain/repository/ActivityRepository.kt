package com.sbm.application.domain.repository

import com.sbm.application.domain.model.Activity

interface ActivityRepository {
    suspend fun getActivities(): Result<List<Activity>>
    suspend fun createActivity(title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?): Result<Activity>
    suspend fun updateActivity(activityId: Long, title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?): Result<Activity>
    suspend fun deleteActivity(id: Long): Result<Unit>
}