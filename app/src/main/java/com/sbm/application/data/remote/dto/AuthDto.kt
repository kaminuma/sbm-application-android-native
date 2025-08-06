package com.sbm.application.data.remote.dto

import com.google.gson.annotations.SerializedName

object AuthDto {
    
    data class LoginRequest(
        @SerializedName("username")
        val username: String,
        @SerializedName("password")
        val password: String
    )
    
    data class LoginResponse(
        @SerializedName("token")
        val token: String?,
        @SerializedName("userId")
        val userId: String?,
        @SerializedName("refreshToken")
        val refreshToken: String? = null  // バックエンド対応時に使用
    )
    
    data class RegisterRequest(
        @SerializedName("username")
        val username: String,
        @SerializedName("email")
        val email: String,
        @SerializedName("password")
        val password: String
    )
    
    data class RegisterResponse(
        @SerializedName("message")
        val message: String  // 常に値が返されるのでnon-nullable
    )
    
    data class UserDto(
        @SerializedName("userId")
        val userId: String,  // Long → String に変更（API仕様に合わせる）
        @SerializedName("username")
        val username: String,
        @SerializedName("email")
        val email: String
    )
    
    data class RefreshTokenRequest(
        @SerializedName("refreshToken")
        val refreshToken: String
    )
    
    data class RefreshTokenResponse(
        @SerializedName("token")
        val token: String?,
        @SerializedName("refreshToken")
        val refreshToken: String?
    )
}