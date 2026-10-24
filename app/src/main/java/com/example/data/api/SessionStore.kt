package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.model.Session
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(Session::class.java)

    companion object {
        private const val PREFS_NAME = "supabase_sessions_secure"
        private const val KEY_ALIAS = "supabase_session_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val ACTIVE_USER_ID = "active_user_id"
        private const val SESSION_PREFIX = "session_"
    }

    init {
        getOrCreateSecretKey()
    }

    fun saveSession(session: Session, isActive: Boolean = true) {
        val json = adapter.toJson(session)
        val encrypted = encrypt(json)
        if (encrypted != null) {
            val editor = prefs.edit()
            editor.putString(SESSION_PREFIX + session.userId, encrypted)
            if (isActive) {
                editor.putString(ACTIVE_USER_ID, session.userId)
            }
            editor.commit() // Synchronous write as requested
        }
    }

    fun getSession(userId: String): Session? {
        val encrypted = prefs.getString(SESSION_PREFIX + userId, null) ?: return null
        val decrypted = decrypt(encrypted) ?: return null
        return try {
            adapter.fromJson(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun getActiveSession(): Session? {
        val userId = prefs.getString(ACTIVE_USER_ID, null) ?: return null
        return getSession(userId)
    }

    fun setActiveUserId(userId: String?) {
        prefs.edit().putString(ACTIVE_USER_ID, userId).commit()
    }

    fun getActiveUserId(): String? = prefs.getString(ACTIVE_USER_ID, null)

    fun deleteSession(userId: String) {
        val editor = prefs.edit()
        editor.remove(SESSION_PREFIX + userId)
        if (getActiveUserId() == userId) {
            editor.remove(ACTIVE_USER_ID)
        }
        editor.commit()
    }

    fun getSessionByEmail(email: String): Session? {
        return getAllUserIds().mapNotNull { getSession(it) }
            .find { it.email.equals(email, ignoreCase = true) }
    }

    fun getAllUserIds(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(SESSION_PREFIX) }
            .map { it.removePrefix(SESSION_PREFIX) }
    }

    // --- Encryption Logic ---

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(data: String): String? {
        return try {
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Result: IV (12 bytes) + EncryptedData
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = ByteArray(12)
            val encrypted = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encrypted, 0, encrypted.size)
            
            val cipher = Cipher.getInstance(AES_MODE)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
