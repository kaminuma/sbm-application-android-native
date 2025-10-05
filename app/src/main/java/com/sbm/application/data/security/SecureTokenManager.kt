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
        private const val ACCESS_TOKEN_KEY = "encrypted_access_token"
        private const val REFRESH_TOKEN_KEY = "encrypted_refresh_token"
        private const val USER_ID_KEY = "encrypted_user_id"
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
     * データの暗号化保存（共通メソッド）
     * @param data 保存するデータ
     * @param key 保存先キー
     * @throws SecurityException 暗号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveEncryptedData(data: String, key: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw SecurityException("Android 6.0 未満はサポート対象外です")
        }
        
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // IV + 暗号化データを結合
            val combined = iv + encryptedBytes
            val encodedData = Base64.encodeToString(combined, Base64.NO_WRAP)
            
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, encodedData)
                .apply()
                
        } catch (e: Exception) {
            throw SecurityException("データの暗号化に失敗しました", e)
        }
    }
    
    /**
     * データの復号化取得（共通メソッド）
     * @param key 取得元キー
     * @return 復号化されたデータ、存在しない場合はnull
     * @throws SecurityException 復号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getEncryptedData(key: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw SecurityException("Android 6.0 未満はサポート対象外です")
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedData = prefs.getString(key, null) ?: return null
        
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
            // 復号化失敗時はデータを削除（セキュリティ上の理由）
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(key)
                .apply()
            throw SecurityException("データの復号化に失敗しました", e)
        }
    }

    /**
     * アクセストークンの保存
     * @param token 保存するアクセストークン
     * @throws SecurityException 暗号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveAccessToken(token: String) {
        saveEncryptedData(token, ACCESS_TOKEN_KEY)
    }
    
    /**
     * リフレッシュトークンの保存
     * @param token 保存するリフレッシュトークン
     * @throws SecurityException 暗号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveRefreshToken(token: String) {
        saveEncryptedData(token, REFRESH_TOKEN_KEY)
    }
    
    /**
     * ユーザーIDの保存
     * @param userId 保存するユーザーID
     * @throws SecurityException 暗号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveUserId(userId: String) {
        saveEncryptedData(userId, USER_ID_KEY)
    }
    
    /**
     * アクセストークンの取得
     * @return 復号化されたアクセストークン、存在しない場合はnull
     * @throws SecurityException 復号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getAccessToken(): String? {
        return getEncryptedData(ACCESS_TOKEN_KEY)
    }
    
    /**
     * リフレッシュトークンの取得
     * @return 復号化されたリフレッシュトークン、存在しない場合はnull
     * @throws SecurityException 復号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getRefreshToken(): String? {
        return getEncryptedData(REFRESH_TOKEN_KEY)
    }
    
    /**
     * ユーザーIDの取得
     * @return 復号化されたユーザーID、存在しない場合はnull
     * @throws SecurityException 復号化に失敗した場合
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun getUserId(): String? {
        return getEncryptedData(USER_ID_KEY)
    }
    
    /**
     * 全認証データの削除
     */
    fun clearAllTokens() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(USER_ID_KEY)
            .apply()
    }
    
    /**
     * レガシー対応：旧メソッドの維持
     * @deprecated saveAccessToken を使用してください
     */
    @Deprecated("Use saveAccessToken instead", ReplaceWith("saveAccessToken(token)"))
    @RequiresApi(Build.VERSION_CODES.M)
    fun saveToken(token: String) {
        saveAccessToken(token)
    }
    
    /**
     * レガシー対応：旧メソッドの維持
     * @deprecated getAccessToken を使用してください
     */
    @Deprecated("Use getAccessToken instead", ReplaceWith("getAccessToken()"))
    @RequiresApi(Build.VERSION_CODES.M)
    fun getToken(): String? {
        return getAccessToken()
    }
    
    /**
     * レガシー対応：旧メソッドの維持
     * @deprecated clearAllTokens を使用してください
     */
    @Deprecated("Use clearAllTokens instead", ReplaceWith("clearAllTokens()"))
    fun clearToken() {
        clearAllTokens()
    }
}