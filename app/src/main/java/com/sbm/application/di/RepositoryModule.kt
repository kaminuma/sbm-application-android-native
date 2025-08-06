package com.sbm.application.di

import com.sbm.application.data.repository.AuthRepositoryImpl
import com.sbm.application.data.repository.ActivityRepositoryImpl
import com.sbm.application.data.repository.MoodRepositoryImpl
import com.sbm.application.domain.repository.AuthRepository
import com.sbm.application.domain.repository.ActivityRepository
import com.sbm.application.domain.repository.MoodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindActivityRepository(
        activityRepositoryImpl: ActivityRepositoryImpl
    ): ActivityRepository
    
    @Binds
    @Singleton
    abstract fun bindMoodRepository(
        moodRepositoryImpl: MoodRepositoryImpl
    ): MoodRepository
}