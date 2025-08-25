package com.sbm.application.domain.repository

import com.sbm.application.domain.model.AIUsageInfo
import com.sbm.application.domain.model.AIUsageError

/**
 * AI利用状況を管理するRepositoryインターフェース
 */
interface AIUsageRepository {
    
    /**
     * AI利用状況を取得
     * @return 利用状況情報、取得失敗時はnull
     */
    suspend fun getUsageInfo(): Result<AIUsageInfo>
    
    /**
     * デバッグ用：利用制限情報を取得
     * @return デバッグ情報のMap
     */
    suspend fun getDebugLimits(): Result<Map<String, Any>>
    
    /**
     * デバッグ用：指定ユーザーの利用回数をリセット
     * @param userId リセット対象のユーザーID
     * @return リセット結果
     */
    suspend fun resetUserLimits(userId: Int): Result<Map<String, Any>>
}