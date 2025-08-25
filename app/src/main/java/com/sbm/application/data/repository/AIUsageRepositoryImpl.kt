package com.sbm.application.data.repository

import com.sbm.application.data.remote.ApiService
import com.sbm.application.domain.model.AIUsageInfo
import com.sbm.application.domain.model.AIUsageError
import com.sbm.application.domain.repository.AIUsageRepository
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException
import javax.inject.Inject

/**
 * AI利用状況Repository実装
 */
class AIUsageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) : AIUsageRepository {

    override suspend fun getUsageInfo(): Result<AIUsageInfo> = withContext(Dispatchers.IO) {
        try {
            val token = authRepository.getStoredToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(AIUsageError.AuthenticationError)
            }
            
            val response = apiService.getAIUsage("Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.let { usageInfo ->
                    Result.success(usageInfo)
                } ?: Result.failure(AIUsageError.UnknownError("レスポンスが空です"))
            } else {
                val error = when (response.code()) {
                    401 -> AIUsageError.AuthenticationError
                    else -> AIUsageError.ApiError(response.code(), response.message())
                }
                Result.failure(error)
            }
        } catch (e: UnknownHostException) {
            Result.failure(AIUsageError.NetworkError)
        } catch (e: SocketTimeoutException) {
            Result.failure(AIUsageError.NetworkError)
        } catch (e: IOException) {
            Result.failure(AIUsageError.NetworkError)
        } catch (e: HttpException) {
            val error = when (e.code()) {
                401 -> AIUsageError.AuthenticationError
                else -> AIUsageError.ApiError(e.code(), e.message())
            }
            Result.failure(error)
        } catch (e: Exception) {
            Result.failure(AIUsageError.UnknownError(e.message ?: "不明なエラー"))
        }
    }
    
    override suspend fun getDebugLimits(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val token = authRepository.getStoredToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(AIUsageError.AuthenticationError)
            }
            
            val response = apiService.getDebugLimits("Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.let { debugInfo ->
                    Result.success(debugInfo)
                } ?: Result.failure(AIUsageError.UnknownError("レスポンスが空です"))
            } else {
                Result.failure(AIUsageError.ApiError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Result.failure(AIUsageError.UnknownError(e.message ?: "デバッグ情報取得エラー"))
        }
    }
    
    override suspend fun resetUserLimits(userId: Int): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val token = authRepository.getStoredToken()
            if (token.isNullOrEmpty()) {
                return@withContext Result.failure(AIUsageError.AuthenticationError)
            }
            
            val response = apiService.resetUserLimits(userId, "Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.let { result ->
                    Result.success(result)
                } ?: Result.failure(AIUsageError.UnknownError("レスポンスが空です"))
            } else {
                Result.failure(AIUsageError.ApiError(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Result.failure(AIUsageError.UnknownError(e.message ?: "利用回数リセットエラー"))
        }
    }
}