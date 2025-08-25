package com.sbm.application.domain.usecase

import com.sbm.application.domain.model.AIUsageInfo
import com.sbm.application.domain.repository.AIUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * AI利用状況を取得するUseCase
 */
class GetAIUsageUseCase @Inject constructor(
    private val aiUsageRepository: AIUsageRepository
) {
    
    /**
     * AI利用状況を取得
     */
    suspend fun execute(): Result<AIUsageInfo> {
        return aiUsageRepository.getUsageInfo()
    }
    
    /**
     * AI利用状況を定期的に取得するFlow
     * 画面表示中に定期的に更新する場合に使用
     */
    fun executeAsFlow(intervalMs: Long = 30000L): Flow<Result<AIUsageInfo>> = flow {
        while (true) {
            emit(aiUsageRepository.getUsageInfo())
            kotlinx.coroutines.delay(intervalMs)
        }
    }
}