package com.sbm.application.data.repository

// import android.util.Log // Removed for production
import com.sbm.application.BuildConfig
import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.remote.dto.ActivityDto
import com.sbm.application.data.network.NetworkError
import com.sbm.application.data.network.NetworkUtil
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.repository.ActivityRepository
import com.sbm.application.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val networkUtil: NetworkUtil
) : ActivityRepository {
    
    private suspend fun getAuthToken(): String {
        return authRepository.getStoredToken() ?: throw Exception("Not authenticated")
    }
    
    override suspend fun getActivities(): Result<List<Activity>> {
        return withContext(Dispatchers.IO) {
            try {
                // ネットワーク接続チェック
                // ネットワークチェックを削除（API後リロード方式ではViewModelで判断）
                
                // リトライ機能付きでAPI呼び出し
                val activities = networkUtil.retryWithBackoff(maxRetries = 3) {
                    val token = getAuthToken()
                    val userId = authRepository.getStoredUserId()?.toInt() ?: throw Exception("User ID not found")
                    val response = apiService.getActivities("Bearer $token", userId)
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        
                        responseBody?.map { dto ->
                            // APIレスポンスの実際の形式に対応
                            val (extractedDate, extractedStartTime) = extractDateAndTime(dto.start)
                            val (_, extractedEndTime) = extractDateAndTime(dto.end)
                            
                            Activity(
                                activityId = dto.activityId,
                                userId = dto.userId.toLong(), // Int → Long 変換
                                title = dto.title ?: "",
                                contents = dto.contents,
                                start = extractedStartTime,
                                end = extractedEndTime,
                                date = dto.date ?: extractedDate, // dateがnullの場合はstartから抽出
                                category = if (dto.category.isBlank()) "その他" else dto.category,
                                categorySub = dto.categorySub
                            )
                        }?.sortedByDescending { it.date } ?: emptyList() // 日付降順でソート
                    } else {
                        throw Exception("Failed to get activities: ${response.message()}")
                    }
                }
                
                Result.success(activities)
            } catch (e: Exception) {
                val networkError = NetworkError.fromThrowable(e)
                Result.failure(networkError)
            }
        }
    }
    
    override suspend fun createActivity(title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?): Result<Activity> {
        return withContext(Dispatchers.IO) {
            try {
                // userId を取得（retryWithBackoff の外で定義）
                val userId = authRepository.getStoredUserId() ?: throw Exception("User ID not found")
                
                // createActivity はリトライなしで実行（重複登録を防ぐため）
                val token = getAuthToken()
                
                val request = ActivityDto.CreateRequest(
                    userId = userId.toInt(),
                    title = title,
                    contents = contents,
                    start = start,
                    end = end,
                    date = date,
                    category = category,
                    categorySub = categorySub
                )
                
                val response = apiService.createActivity("Bearer $token", request)
                
                if (response.isSuccessful) {
                    // ResponseBodyからプレーンテキストを取得
                    val responseBodyString = response.body()?.string() ?: ""
                } else {
                    throw Exception("Failed to create activity: ${response.message()}")
                }
                
                // API後リロード方式のため、ダミーレスポンスを返却（ViewModelでloadActivities()が呼ばれる）
                val storedUserId = authRepository.getStoredUserId()?.toLong()
                
                if (storedUserId == null) {
                    throw Exception("User ID not found for response creation")
                }
                
                val createdActivity = Activity(
                    activityId = 0L, // 実際のIDは後でloadActivities()で取得される
                    userId = storedUserId,
                    title = title,
                    contents = contents,
                    start = start,
                    end = end,
                    date = date,
                    category = category,
                    categorySub = categorySub
                )
                
                Result.success(createdActivity)
            } catch (e: Exception) {
                val networkError = NetworkError.fromThrowable(e)
                Result.failure(networkError)
            }
        }
    }
    
    override suspend fun updateActivity(activityId: Long, title: String, contents: String?, start: String, end: String, date: String, category: String, categorySub: String?): Result<Activity> {
        return withContext(Dispatchers.IO) {
            try {
                networkUtil.retryWithBackoff(maxRetries = 3) {
                    val token = getAuthToken()
                    val userId = authRepository.getStoredUserId()?.toInt() ?: throw Exception("User ID not found")
                    
                    val request = ActivityDto.UpdateRequest(
                        userId = userId,
                        title = title,
                        contents = contents,
                        start = start,
                        end = end,
                        date = date,
                        category = category,
                        categorySub = categorySub
                    )
                    
                    val response = apiService.updateActivity("Bearer $token", activityId, request)
                    
                    if (response.isSuccessful) {
                        // ResponseBodyから空のレスポンスを処理
                        val responseBodyString = response.body()?.string() ?: ""
                        Unit // 更新成功
                    } else {
                        throw Exception("Failed to update activity: ${response.message()}")
                    }
                }
                
                // 更新成功時はダミーのActivityオブジェクトを返す
                val storedUserId = authRepository.getStoredUserId()?.toLong() ?: throw Exception("User ID not found")
                val updatedActivity = Activity(
                    activityId = activityId,
                    userId = storedUserId,
                    title = title,
                    contents = contents,
                    start = start,
                    end = end,
                    date = date,
                    category = category,
                    categorySub = categorySub
                )
                
                Result.success(updatedActivity)
            } catch (e: Exception) {
                val networkError = NetworkError.fromThrowable(e)
                Result.failure(networkError)
            }
        }
    }
    
    override suspend fun deleteActivity(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAuthToken()
                
                if (token.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("No authentication token available"))
                }
                
                val response = apiService.deleteActivity("Bearer $token", id)
                
                if (response.isSuccessful) {
                    // ResponseBodyから空のレスポンスを処理
                    val responseBodyString = try {
                        response.body()?.string() ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete activity: HTTP ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * APIレスポンスの時刻文字列から日付と時刻を抽出
     * 入力例: "2024-11-28 22:50" → ("2024-11-28", "22:50")
     */
    private fun extractDateAndTime(dateTimeString: String?): Pair<String, String> {
        if (dateTimeString.isNullOrBlank()) {
            return Pair("2024-01-01", "00:00")
        }
        
        return try {
            if (dateTimeString.contains(" ")) {
                // "YYYY-MM-DD HH:mm" 形式
                val parts = dateTimeString.split(" ")
                val date = parts[0]
                val time = parts[1]
                Pair(date, time)
            } else if (dateTimeString.contains("T")) {
                // ISO 8601形式 "YYYY-MM-DDTHH:mm:ss"
                val parts = dateTimeString.split("T")
                val date = parts[0]
                val time = parts[1].substring(0, 5) // HH:mm部分のみ
                Pair(date, time)
            } else {
                // 時刻のみの場合
                Pair("2024-01-01", dateTimeString)
            }
        } catch (e: Exception) {
            Pair("2024-01-01", "00:00")
        }
    }
}