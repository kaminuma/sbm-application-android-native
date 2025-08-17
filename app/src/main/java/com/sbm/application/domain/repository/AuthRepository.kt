package com.sbm.application.domain.repository

import com.sbm.application.data.remote.dto.LoginResponse
import retrofit2.Response

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Pair<String, String>> // (token, userId)
    suspend fun register(username: String, email: String, password: String): Result<Pair<String, String>> // (token, userId)
    suspend fun logout(token: String): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun getStoredToken(): String?
    suspend fun getStoredUserId(): String?

    suspend fun getStoredRefreshToken(): String?
    suspend fun refreshToken(): Result<Pair<String, String>> // (token, refreshToken)
    suspend fun clearAuth(): Unit
    
    // Google OAuth2
    suspend fun getOAuth2Session(sessionId: String): Response<LoginResponse>
    suspend fun saveToken(token: String)
    suspend fun saveUserId(userId: String)
}