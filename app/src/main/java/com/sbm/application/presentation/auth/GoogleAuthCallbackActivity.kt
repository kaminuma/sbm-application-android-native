package com.sbm.application.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sbm.application.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoogleAuthCallbackActivity : ComponentActivity() {
    
    private val viewModel: GoogleAuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // URLからパラメータを取得
        val uri = intent.data
        val sessionId = uri?.getQueryParameter("session")
        val error = uri?.getQueryParameter("error")
        
        when {
            error != null -> {
                // エラー処理
                Toast.makeText(this, "認証エラー: $error", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
            sessionId != null -> {
                // セッションIDを使ってJWTトークンを取得
                fetchJwtToken(sessionId)
            }
            else -> {
                Toast.makeText(this, "セッションIDが見つかりません", Toast.LENGTH_LONG).show()
                navigateToLogin()
            }
        }
    }
    
    private fun fetchJwtToken(sessionId: String) {
        lifecycleScope.launch {
            viewModel.fetchJwtToken(sessionId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        // メイン画面へ遷移
                        startActivity(Intent(this@GoogleAuthCallbackActivity, MainActivity::class.java))
                        finishAffinity()
                    }
                    is Result.Error -> {
                        Toast.makeText(
                            this@GoogleAuthCallbackActivity,
                            "認証失敗: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToLogin()
                    }
                    is Result.Loading -> {
                        // ローディング表示（必要に応じて）
                    }
                }
            }
        }
    }
    
    private fun navigateToLogin() {
        // MainActivityにフラグを設定してログイン画面へ戻す
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_LOGIN", true)
        }
        startActivity(intent)
        finish()
    }
}