package com.sbm.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sbm.application.presentation.navigation.SBMNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var shouldCheckAuth = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Googleログイン成功フラグをチェック
        val isGoogleAuthSuccess = intent.getBooleanExtra("GOOGLE_AUTH_SUCCESS", false)
        
        setContent {
            SBMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SBMNavigation(
                        isGoogleAuthSuccess = isGoogleAuthSuccess,
                        shouldReCheckAuth = false
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // バックグラウンドから復帰時は認証チェックフラグを立てる
        if (!shouldCheckAuth) {
            shouldCheckAuth = true  // 初回は除外
        } else {
            // 2回目以降のonResumeで認証チェックをトリガー
            setContent {
                SBMTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SBMNavigation(
                            isGoogleAuthSuccess = intent.getBooleanExtra("GOOGLE_AUTH_SUCCESS", false),
                            shouldReCheckAuth = true
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun SBMTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}