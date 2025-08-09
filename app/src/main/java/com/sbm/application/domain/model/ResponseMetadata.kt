package com.sbm.application.domain.model

data class ResponseMetadata(
    val apiVersion: String,
    val processingTimeMs: Long,
    val tokensUsed: Int? = null,
    val remainingQuota: Int? = null,
    val requestId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)