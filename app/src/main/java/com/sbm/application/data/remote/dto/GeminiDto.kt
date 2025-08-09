package com.sbm.application.data.remote.dto

import com.google.gson.annotations.SerializedName

// Gemini API Request
data class GeminiRequest(
    val contents: List<Content>,
    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null,
    @SerializedName("safetySettings")
    val safetySettings: List<SafetySetting>? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    @SerializedName("topK")
    val topK: Int = 40,
    @SerializedName("topP")
    val topP: Float = 0.8f,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 2048,
    @SerializedName("stopSequences")
    val stopSequences: List<String> = emptyList()
)

data class SafetySetting(
    val category: String,
    val threshold: String
)

// Gemini API Response
data class GeminiResponse(
    val candidates: List<Candidate>,
    @SerializedName("usageMetadata")
    val usageMetadata: UsageMetadata?,
    @SerializedName("promptFeedback")
    val promptFeedback: PromptFeedback?
)

data class Candidate(
    val content: Content,
    @SerializedName("finishReason")
    val finishReason: String?,
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>?
)

data class UsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int,
    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int,
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int
)

data class PromptFeedback(
    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    val category: String,
    val probability: String
)

// AI分析結果のDTO
data class GeminiInsightDto(
    val summary: String,
    val moodAnalysis: String,
    val activityAnalysis: String,
    val recommendations: List<String>,
    val highlights: List<String>,
    val motivationalMessage: String
)