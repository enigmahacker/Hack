package com.example.jarvis.security

import android.content.Context
import android.content.SharedPreferences
import com.example.jarvis.BuildConfig

/**
 * Secure storage interface storing sensitive API keys encrypted via Android Keystore.
 */
class SecureStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "jarvis_secure_credentials"
        private const val KEY_GEMINI_API_KEY = "enc_gemini_api_key"
        private const val KEY_OPENAI_API_KEY = "enc_openai_api_key"
        private const val KEY_CUSTOM_API_KEY = "enc_custom_api_key"
        private const val KEY_FISH_AUDIO_API_KEY = "enc_fish_audio_api_key"
        private const val KEY_FISH_AUDIO_MODEL_ID = "enc_fish_audio_model_id"
    }

    var geminiApiKey: String
        get() {
            val enc = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
            return KeystoreManager.decrypt(enc)
        }
        set(value) {
            val enc = KeystoreManager.encrypt(value)
            prefs.edit().putString(KEY_GEMINI_API_KEY, enc).apply()
        }

    var openAiApiKey: String
        get() {
            val enc = prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
            return KeystoreManager.decrypt(enc)
        }
        set(value) {
            val enc = KeystoreManager.encrypt(value)
            prefs.edit().putString(KEY_OPENAI_API_KEY, enc).apply()
        }

    var customApiKey: String
        get() {
            val enc = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
            return KeystoreManager.decrypt(enc)
        }
        set(value) {
            val enc = KeystoreManager.encrypt(value)
            prefs.edit().putString(KEY_CUSTOM_API_KEY, enc).apply()
        }

    var fishAudioApiKey: String
        get() {
            val enc = prefs.getString(KEY_FISH_AUDIO_API_KEY, "") ?: ""
            val decrypted = KeystoreManager.decrypt(enc)
            return if (decrypted.isNotBlank()) decrypted else BuildConfig.FISH_AUDIO_API_KEY
        }
        set(value) {
            val enc = KeystoreManager.encrypt(value)
            prefs.edit().putString(KEY_FISH_AUDIO_API_KEY, enc).apply()
        }

    var fishAudioModelId: String
        get() {
            val enc = prefs.getString(KEY_FISH_AUDIO_MODEL_ID, "") ?: ""
            val decrypted = KeystoreManager.decrypt(enc)
            return if (decrypted.isNotBlank()) decrypted else BuildConfig.FISH_AUDIO_MODEL_ID
        }
        set(value) {
            val enc = KeystoreManager.encrypt(value)
            prefs.edit().putString(KEY_FISH_AUDIO_MODEL_ID, enc).apply()
        }

    fun hasAnyApiKey(): Boolean {
        return geminiApiKey.isNotBlank() || openAiApiKey.isNotBlank() || customApiKey.isNotBlank() || fishAudioApiKey.isNotBlank()
    }
}
