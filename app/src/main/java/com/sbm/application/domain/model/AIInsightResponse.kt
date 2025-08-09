package com.sbm.application.domain.model

data class AIInsightResponse(
    val success: Boolean,
    val data: AIInsight?,
    val error: String?,
    val metadata: ResponseMetadata? = null
)

