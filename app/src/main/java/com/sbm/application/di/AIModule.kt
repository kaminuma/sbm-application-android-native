package com.sbm.application.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sbm.application.data.remote.ApiService
import com.sbm.application.data.repository.ProxyAIAnalysisRepositoryImpl
import com.sbm.application.domain.repository.AIAnalysisRepository
import com.sbm.application.domain.repository.AIConfigRepository
import com.sbm.application.domain.repository.AuthRepository
import com.sbm.application.domain.service.AIPromptGenerator
import com.sbm.application.data.metrics.AIAnalysisMetrics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideAIPromptGenerator(): AIPromptGenerator {
        return AIPromptGenerator()
    }

    // バックエンドAPI経由版のみ（新実装）
    @Provides
    @Singleton
    fun provideAIAnalysisRepository(
        apiService: ApiService,
        authRepository: AuthRepository,
        metrics: com.sbm.application.data.metrics.AIAnalysisMetrics
    ): AIAnalysisRepository {
        return ProxyAIAnalysisRepositoryImpl(
            apiService = apiService,
            authRepository = authRepository,
            metrics = metrics
        )
    }
}