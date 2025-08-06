package com.sbm.application.domain.model

data class MoodRecord(
    val id: Long,
    val userId: Long,
    val date: String,
    val mood: Int,
    val note: String,
    val createdAt: String?,
    val updatedAt: String?
)