package com.sbm.application.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sbm.application.BuildConfig

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // AdMob IDが設定されていない場合は表示しない
    if (BuildConfig.ADMOB_BANNER_ID.isEmpty()) {
        return
    }
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp), // レスポンシブバナー用に少し高さ調整
        factory = { context ->
            AdView(context).apply {
                // レスポンシブバナー：画面幅に応じて最適サイズを自動選択
                val displayMetrics = context.resources.displayMetrics
                val adWidthPixels = displayMetrics.widthPixels.toFloat()
                val adWidth = (adWidthPixels / displayMetrics.density).toInt()
                
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}