package com.example.jarvis.data.preferences

import android.content.Context
import android.content.SharedPreferences

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "jarvis_user_preferences"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_WORD_SENSITIVITY = "wake_word_sensitivity"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_BARGE_IN_ENABLED = "barge_in_enabled"
        private const val KEY_FISH_AUDIO_ENABLED = "fish_audio_enabled"
        private const val KEY_FISH_AUDIO_MODEL_ID = "fish_audio_model_id"
    }

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat(KEY_WAKE_WORD_SENSITIVITY, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_WAKE_WORD_SENSITIVITY, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.05f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var aiProvider: String
        get() = prefs.getString(KEY_AI_PROVIDER, "LOCAL") ?: "LOCAL"
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, "gemini-1.5-flash") ?: "gemini-1.5-flash"
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()

    var memoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()

    var bargeInEnabled: Boolean
        get() = prefs.getBoolean(KEY_BARGE_IN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BARGE_IN_ENABLED, value).apply()

    var fishAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_FISH_AUDIO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FISH_AUDIO_ENABLED, value).apply()

    var fishAudioModelId: String
        get() = prefs.getString(KEY_FISH_AUDIO_MODEL_ID, "9a9cf47702da476aa4629e2506d4a857") ?: "9a9cf47702da476aa4629e2506d4a857"
        set(value) = prefs.edit().putString(KEY_FISH_AUDIO_MODEL_ID, value).apply()
}
