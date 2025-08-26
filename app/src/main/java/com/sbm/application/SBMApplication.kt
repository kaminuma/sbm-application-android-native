package com.sbm.application

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SBMApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Google Mobile Ads SDK初期化
        MobileAds.initialize(this)
        
        // APIConfig初期化
        com.sbm.application.config.ApiConfig.API_BASE_URL
    }
}