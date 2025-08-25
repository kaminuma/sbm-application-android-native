package com.sbm.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SBMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.sbm.application.config.ApiConfig.API_BASE_URL
    }
}