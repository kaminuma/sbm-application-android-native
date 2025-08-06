package com.sbm.application.domain.model

data class Activity(
    val activityId: Long,
    val userId: Long,
    val title: String,
    val contents: String?,
    val start: String, // 時刻 "HH:mm"
    val end: String,   // 時刻 "HH:mm"
    val date: String,  // 日付 "YYYY-MM-DD" 
    val category: String,
    val categorySub: String?
) {
    companion object {
        fun createDefault(): Activity {
            return Activity(
                activityId = 0L,
                userId = 0L,
                title = "",
                contents = null,
                start = "00:00",
                end = "00:00",
                date = "2024-01-01",
                category = "その他",
                categorySub = null
            )
        }
    }
}