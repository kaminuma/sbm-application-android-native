package com.sbm.application.data.security

import android.util.Base64
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * JWT トークンの検証クラス
 * - 署名検証
 * - 有効期限検証
 * - 形式検証
 */
object JWTValidator {
    
    /**
     * JWT トークンの包括的検証
     * @param token 検証対象のJWTトークン
     * @param secret JWT署名検証用のシークレット（本番環境では適切に管理）
     * @return 検証結果（true: 有効, false: 無効）
     */
    fun validateToken(token: String, secret: String? = null): Boolean {
        return try {
            // 基本的な形式チェック
            if (!isValidJWTFormat(token)) {
                return false
            }
            
            // 署名検証（本番環境では実際のシークレットを使用）
            if (!secret.isNullOrEmpty()) {
                val algorithm = Algorithm.HMAC256(secret)
                val verifier = JWT.require(algorithm).build()
                verifier.verify(token)
            }
            
            // 有効期限とクレーム検証
            validateTokenClaims(token)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * JWT トークンの基本形式検証
     */
    private fun isValidJWTFormat(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 3) return false
        
        // 各部分がBase64エンコードされているかチェック
        return parts.all { part ->
            try {
                Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * JWT クレームの詳細検証
     */
    private fun validateTokenClaims(token: String): Boolean {
        try {
            val decodedJWT = JWT.decode(token)
            
            // 必須クレームの存在確認
            val issuer = decodedJWT.issuer
            val subject = decodedJWT.subject
            val expiresAt = decodedJWT.expiresAt
            
            // 発行者検証
            if (issuer.isNullOrEmpty() || !isValidIssuer(issuer)) {
                return false
            }
            
            // サブジェクト検証
            if (subject.isNullOrEmpty()) {
                return false
            }
            
            // 有効期限検証（期限切れまで有効）
            val currentTime = Date()
            
            return expiresAt != null && currentTime.before(expiresAt)
            
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * トークンの有効期限が近いかチェック
     * @param token JWTトークン
     * @param thresholdMinutes 閾値（分）
     * @return 有効期限まで閾値以内の場合はtrue
     */
    fun isTokenExpiringSoon(token: String, thresholdMinutes: Int = 2): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            val expiresAt = decodedJWT.expiresAt ?: return false
            
            val currentTime = Date()
            val thresholdTime = Date(currentTime.time + thresholdMinutes * 60 * 1000)
            
            // 有効期限が閾値時間より前（期限が近い）
            expiresAt.before(thresholdTime)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 有効な発行者かチェック
     */
    private fun isValidIssuer(issuer: String): Boolean {
        // 許可された発行者のみ受け入れ
        val allowedIssuers = listOf(
            "sbm-app.com",
            "api.sbm-app.com",
            "auth.sbm-app.com"
        )
        return allowedIssuers.any { allowedIssuer ->
            issuer.contains(allowedIssuer, ignoreCase = true)
        }
    }
    
    /**
     * トークンからユーザーIDを安全に取得
     */
    fun getUserIdFromToken(token: String): String? {
        return try {
            if (!validateToken(token)) return null
            
            val decodedJWT = JWT.decode(token)
            decodedJWT.subject // ユーザーIDがsubjectに格納されている前提
        } catch (e: Exception) {
            null
        }
    }
}