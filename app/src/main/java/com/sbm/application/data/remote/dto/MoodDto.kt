package com.sbm.application.data.remote.dto

import com.google.gson.annotations.SerializedName

object MoodDto {
    
    // 気分記録作成リクエスト
    data class CreateRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("date")
        val date: String,  // yyyy-MM-dd形式
        @SerializedName("mood")
        val mood: Int,
        @SerializedName("note")
        val note: String
    )
    
    // 気分記録更新リクエスト
    data class UpdateRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("mood")
        val mood: Int,
        @SerializedName("note")
        val note: String
    )
    
    // 気分記録レスポンス
    data class MoodRecord(
        @SerializedName("id")
        val id: Long,
        @SerializedName("userId")
        val userId: Long,
        @SerializedName("date")
        val date: String,
        @SerializedName("mood")
        val mood: Int,
        @SerializedName("note")
        val note: String,
        @SerializedName("createdAt")
        val createdAt: String?,
        @SerializedName("updatedAt")
        val updatedAt: String?
    )
    
    // 気分記録一覧レスポンス
    data class MoodResponse(
        @SerializedName("moodRecords")
        val moodRecords: List<MoodRecord>
    )
    
    // 気分記録操作結果レスポンス
    data class OperationResponse(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("message")
        val message: String
    )
}