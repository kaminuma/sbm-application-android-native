package com.sbm.application.data.remote.dto

import com.google.gson.annotations.SerializedName

object ActivityDto {
    
    // 活動記録作成リクエスト
    data class CreateRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("title")
        val title: String,
        @SerializedName("contents")
        val contents: String?,
        @SerializedName("start")
        val start: String,  // HH:mm形式
        @SerializedName("end")
        val end: String,    // HH:mm形式
        @SerializedName("date")
        val date: String,   // yyyy-MM-dd形式
        @SerializedName("category")
        val category: String,
        @SerializedName("categorySub")
        val categorySub: String?
    )
    
    // 活動記録更新リクエスト
    data class UpdateRequest(
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("title")
        val title: String,
        @SerializedName("contents")
        val contents: String?,
        @SerializedName("start")
        val start: String,  // HH:mm形式
        @SerializedName("end")
        val end: String,    // HH:mm形式
        @SerializedName("date")
        val date: String,   // yyyy-MM-dd形式
        @SerializedName("category")
        val category: String,
        @SerializedName("categorySub")
        val categorySub: String?
    )
    
    // 活動記録レスポンス
    data class ActivityResponse(
        @SerializedName("activityId")
        val activityId: Long,
        @SerializedName("userId")
        val userId: Int,
        @SerializedName("title")
        val title: String,
        @SerializedName("contents")
        val contents: String?,
        @SerializedName("start")
        val start: String,  // HH:mm形式
        @SerializedName("end")
        val end: String,    // HH:mm形式
        @SerializedName("date")
        val date: String,   // yyyy-MM-dd形式
        @SerializedName("category")
        val category: String,
        @SerializedName("categorySub")
        val categorySub: String?
    )
    
    // 活動記録操作結果レスポンス
    data class OperationResponse(
        @SerializedName("success")
        val success: Boolean,
        @SerializedName("message")
        val message: String
    )
}