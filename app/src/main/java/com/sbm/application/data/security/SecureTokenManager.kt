package com.sbm.application.data.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * セキュアなトークン管理クラス
 * - Android Keystore を使用した暗号化
 * - フォールバックなしの安全な実装
 */
class SecureTokenManager(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "SBM_AUTH_KEY"
        private const val PREFS_NAME = "sbm_secure_auth"
        private const val TOKEN_KEY = "encrypted_token"
    }
    
    private val keystore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }
    
    /**
     * 暗号化キーの生成・取得
     * @throws SecurityException 暗号化キー生成に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateSecretKey(): SecretKey {
        // 既存キーチェック
        keystore.getKey(KEYSTORE_ALIAS, null)?.let { 
            return it as SecretKey 
        }
        
        // 新規キー生成
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // バックグラウンド処理対応
            .build()
            
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * トークンの暗号化保存
     * @param token 保存するトークン
     * @throws SecurityException 暗号化に失敗した場合（フォールバックなし）
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveToken(token: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw SecurityException("Android 6.0 未満はサポート対象外です")
        }
        
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encryptedBytes = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // IV + 暗号化データを結合
            val combined = iv + encryptedBytes
            val encodedData = Base64.encodeToString(combined, Base64.NO_WRAP)
            
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TOKEN_KEY, encodedData)
                .apply()
                
        } catch (e: Exception) {
            // フォールバックなし - 例外をそのまま投げる
            throw SecurityException("トークンの暗号化に失敗しました", e)
        }
    }
    
    /**
     * トークンの復号化取得
     * @return 復号化されたトークン、存在しない場合はnull
     * @throws SecurityException 復号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getToken(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw SecurityException("Android 6.0 未満はサポート対象外です")
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedData = prefs.getString(TOKEN_KEY, null) ?: return null
        
        try {
            val secretKey = getOrCreateSecretKey()
            val combined = Base64.decode(encodedData, Base64.NO_WRAP)
            
            // IV（最初の12バイト）と暗号化データを分離
            val iv = combined.sliceArray(0..11)
            val encryptedBytes = combined.sliceArray(12 until combined.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
            
        } catch (e: Exception) {
            // 復号化失敗時はトークンを削除（セキュリティ上の理由）
            clearToken()
            throw SecurityException("トークンの復号化に失敗しました", e)
        }
    }
    
    /**
     * トークンの削除
     */
    fun clearToken() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(TOKEN_KEY)
            .apply()
    }
}