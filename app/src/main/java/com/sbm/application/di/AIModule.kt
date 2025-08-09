package com.sbm.application.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sbm.application.data.local.ConfigManager
import com.sbm.application.data.remote.GeminiApiService
import com.sbm.application.data.repository.AIConfigRepositoryImpl
import com.sbm.application.data.repository.GeminiAnalysisRepositoryImpl
import com.sbm.application.domain.repository.AIAnalysisRepository
import com.sbm.application.domain.repository.AIConfigRepository
import com.sbm.application.domain.service.AIPromptGenerator
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
    @Named("gemini_http_client")
    fun provideGeminiOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("gemini_retrofit")
    fun provideGeminiRetrofit(
        @Named("gemini_http_client") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GeminiApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(
        @Named("gemini_retrofit") retrofit: Retrofit
    ): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAIPromptGenerator(): AIPromptGenerator {
        return AIPromptGenerator()
    }

    @Provides
    @Singleton
    fun provideAIConfigRepository(): AIConfigRepository {
        return AIConfigRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideAIAnalysisRepository(
        geminiApiService: GeminiApiService,
        configManager: ConfigManager,
        gson: Gson,
        promptGenerator: AIPromptGenerator
    ): AIAnalysisRepository {
        return GeminiAnalysisRepositoryImpl(
            geminiApiService = geminiApiService,
            configManager = configManager,
            gson = gson,
            promptGenerator = promptGenerator
        )
    }
}