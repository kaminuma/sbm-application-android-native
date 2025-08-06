package com.sbm.application.data.repository

import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.MoodDto
import com.sbm.application.data.network.NetworkError
import com.sbm.application.data.network.NetworkUtil
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.domain.repository.MoodRepository
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val networkUtil: NetworkUtil
) : MoodRepository {
    
    private suspend fun getAuthToken(): String {
        return authRepository.getStoredToken() ?: throw Exception("Not authenticated")
    }
    
    override suspend fun getMoodRecords(): Result<List<MoodRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val userId = authRepository.getStoredUserId()?.toInt() ?: throw Exception("User ID not found")
                val response = apiService.getMoodRecords("Bearer $token", userId)
                
                if (response.isSuccessful) {
                    val moodResponse = response.body()!!
                    val moodRecords = moodResponse.moodRecords.map { dto ->
                        MoodRecord(
                            id = dto.id,
                            userId = dto.userId,
                            date = dto.date,
                            mood = dto.mood,
                            note = dto.note,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )
                    }
                    
                    Result.success(moodRecords)
                } else {
                    Result.failure(Exception("Failed to get mood records: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun createMoodRecord(date: String, mood: Int, note: String?): Result<MoodRecord> {
        return withContext(Dispatchers.IO) {
            try {
                // ネットワークチェックを削除（API後リロード方式ではViewModelで判断）
                
                networkUtil.retryWithBackoff(maxRetries = 3) {
                    val token = getAuthToken()
                    val userId = authRepository.getStoredUserId() ?: throw Exception("User ID not found")
                    
                    val request = MoodDto.CreateRequest(
                        userId = userId.toInt(),
                        date = date,
                        mood = mood,
                        note = note ?: ""
                    )
                    
                    val response = apiService.createMoodRecord("Bearer $token", request)
                    
                    if (response.isSuccessful) {
                        val operationResponse = response.body()!!
                        if (operationResponse.success) {
                            Unit // 作成成功
                        } else {
                            throw Exception("Failed to create mood record: ${operationResponse.message}")
                        }
                    } else {
                        throw Exception("Failed to create mood record: ${response.message()}")
                    }
                }
                
                // API後リロード方式のため、ダミーレスポンスを返却（ViewModelでloadMoodRecords()が呼ばれる）
                val storedUserId = authRepository.getStoredUserId()?.toLong() ?: throw Exception("User ID not found")
                val createdRecord = MoodRecord(
                    id = 0L, // 実際のIDは後でloadMoodRecords()で取得される
                    userId = storedUserId,
                    date = date,
                    mood = mood,
                    note = note ?: "",
                    createdAt = null,
                    updatedAt = null
                )
                
                Result.success(createdRecord)
            } catch (e: Exception) {
                val networkError = NetworkError.fromThrowable(e)
                Result.failure(networkError)
            }
        }
    }
    
    override suspend fun updateMoodRecord(date: String, mood: Int, note: String?): Result<MoodRecord> {
        return withContext(Dispatchers.IO) {
            try {
                // ネットワークチェックを削除（API後リロード方式ではViewModelで判断）
                
                networkUtil.retryWithBackoff(maxRetries = 3) {
                    val token = getAuthToken()
                    val userId = authRepository.getStoredUserId()?.toInt() ?: throw Exception("User ID not found")
                    val request = MoodDto.UpdateRequest(
                        userId = userId,
                        mood = mood, 
                        note = note ?: ""
                    )
                    
                    val response = apiService.updateMoodRecord("Bearer $token", date, request)
                    
                    if (response.isSuccessful) {
                        val operationResponse = response.body()!!
                        if (operationResponse.success) {
                            Unit // 更新成功
                        } else {
                            throw Exception("Failed to update mood record: ${operationResponse.message}")
                        }
                    } else {
                        throw Exception("Failed to update mood record: ${response.message()}")
                    }
                }
                
                // API後リロード方式のため、ダミーレスポンスを返却（ViewModelでloadMoodRecords()が呼ばれる）
                val storedUserId = authRepository.getStoredUserId()?.toLong() ?: throw Exception("User ID not found")
                val updatedRecord = MoodRecord(
                    id = 0L, // 実際のIDは後でloadMoodRecords()で取得される
                    userId = storedUserId,
                    date = date,
                    mood = mood,
                    note = note ?: "",
                    createdAt = null,
                    updatedAt = null
                )
                
                Result.success(updatedRecord)
            } catch (e: Exception) {
                val networkError = NetworkError.fromThrowable(e)
                Result.failure(networkError)
            }
        }
    }
    
    override suspend fun deleteMoodRecord(date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                val userId = authRepository.getStoredUserId()?.toLong() ?: throw Exception("User ID not found")
                val response = apiService.deleteMoodRecord("Bearer $token", date, userId)
                
                if (response.isSuccessful) {
                    // サーバーはJSONオブジェクト形式で返している
                    val operationResponse = response.body()
                    if (operationResponse == null) {
                        // レスポンスボディがない場合は成功として扱う
                        Result.success(Unit)
                    } else if (operationResponse.success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to delete mood record: ${operationResponse.message}"))
                    }
                } else {
                    Result.failure(Exception("Failed to delete mood record: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}