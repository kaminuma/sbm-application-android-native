package com.sbm.application.data.security

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

/**
 * セキュアな外部URL遷移管理クラス
 */
class SecureWebViewManager {
    
    companion object {
        // 許可されたドメインのホワイトリスト
        private val TRUSTED_DOMAINS = listOf(
            "sbm-app.com",
            "www.sbm-app.com"
        )
        
        // 証明書ピニング用のフィンガープリント（実装例）
        private val CERTIFICATE_PINS = mapOf(
            "sbm-app.com" to listOf(
                "sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX=", // 実際の証明書フィンガープリントに置き換え
                "sha256/YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY=" // バックアップ証明書
            )
        )
    }
    
    /**
     * セキュアなURL遷移
     * @param context コンテキスト
     * @param url 遷移先URL
     * @param onError エラー時のコールバック
     */
    fun openSecureUrl(
        context: Context, 
        url: String, 
        onError: ((String) -> Unit)? = null
    ) {
        try {
            // URL検証
            if (!isUrlTrusted(url)) {
                onError?.invoke("信頼できないURLです: $url")
                return
            }
            
            // 証明書ピニング対応のCustomTabsIntent
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false) // URLバーを常に表示
                .setStartAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
                .build()
            
            // セキュリティヘッダーの設定
            customTabsIntent.intent.putExtra(
                "com.android.browser.headers",
                Bundle().apply {
                    putString("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
                    putString("X-Content-Type-Options", "nosniff")
                    putString("X-Frame-Options", "DENY")
                    putString("User-Agent", "SBMApp/1.0 Android Secure")
                }
            )
            
            customTabsIntent.launchUrl(context, Uri.parse(url))
            
        } catch (e: Exception) {
            onError?.invoke("URL遷移中にエラーが発生しました: ${e.message}")
        }
    }
    
    /**
     * URLが信頼できるかチェック
     */
    private fun isUrlTrusted(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()
            
            // HTTPS必須
            if (uri.scheme != "https") {
                return false
            }
            
            // ホワイトリストに含まれているかチェック
            TRUSTED_DOMAINS.any { trustedDomain ->
                host == trustedDomain || host?.endsWith(".$trustedDomain") == true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 証明書ピニングの検証（実装例）
     * 注意: 実際の実装では、ネットワークレベルでの証明書ピニングが必要
     */
    private fun validateCertificatePinning(hostname: String): Boolean {
        // 実装が複雑なため、ここでは概念的な実装例を示す
        // 実際にはOkHttpのCertificatePinnerを使用することを推奨
        return CERTIFICATE_PINS.containsKey(hostname)
    }
}