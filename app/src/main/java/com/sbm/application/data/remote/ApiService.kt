package com.sbm.application.data.remote

import com.sbm.application.data.remote.dto.AuthDto
import com.sbm.application.data.remote.dto.LoginResponse
import com.sbm.application.data.remote.dto.ActivityDto
import com.sbm.application.data.remote.dto.MoodDto
import com.sbm.application.data.remote.dto.AIAnalysisRequestDto
import com.sbm.application.data.remote.dto.AIAnalysisResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication
    @POST("auth/login")
    suspend fun login(@Body loginRequest: AuthDto.LoginRequest): Response<AuthDto.LoginResponse>
    
    @POST("auth/register")
    suspend fun register(@Body registerRequest: AuthDto.RegisterRequest): Response<ResponseBody>
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body refreshRequest: AuthDto.RefreshTokenRequest): Response<AuthDto.RefreshTokenResponse>
    
    @FormUrlEncoded
    @POST("auth/oauth2/session")
    suspend fun getOAuth2Session(
        @Field("sessionId") sessionId: String
    ): Response<LoginResponse>
    
    // Activities
    @GET("activities")
    suspend fun getActivities(
        @Header("Authorization") token: String,
        @Query("userId") userId: Int
    ): Response<List<ActivityDto.ActivityResponse>>
    
    @POST("activities")
    suspend fun createActivity(
        @Header("Authorization") token: String,
        @Body activity: ActivityDto.CreateRequest
    ): Response<ResponseBody>
    
    @PUT("activities/{activityId}")
    suspend fun updateActivity(
        @Header("Authorization") token: String,
        @Path("activityId") activityId: Long,
        @Body activity: ActivityDto.UpdateRequest
    ): Response<ResponseBody>
    
    @DELETE("activities/{id}")
    suspend fun deleteActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ResponseBody>
    
    // Mood Records
    @GET("mood")
    suspend fun getMoodRecords(
        @Header("Authorization") token: String,
        @Query("userId") userId: Int
    ): Response<MoodDto.MoodResponse>
    
    @POST("mood")
    suspend fun createMoodRecord(
        @Header("Authorization") token: String,
        @Body mood: MoodDto.CreateRequest
    ): Response<MoodDto.OperationResponse>
    
    @PUT("mood/{date}")
    suspend fun updateMoodRecord(
        @Header("Authorization") token: String,
        @Path("date") date: String,
        @Body mood: MoodDto.UpdateRequest
    ): Response<MoodDto.OperationResponse>
    
    @DELETE("mood/{date}")
    suspend fun deleteMoodRecord(
        @Header("Authorization") token: String,
        @Path("date") date: String,
        @Query("userId") userId: Long
    ): Response<MoodDto.OperationResponse>
    
    // AI Analysis (Proxy to Backend API)
    @POST("ai/analysis")
    suspend fun generateAIAnalysis(
        @Header("Authorization") token: String,
        @Body request: AIAnalysisRequestDto
    ): Response<AIAnalysisResponseDto>
}