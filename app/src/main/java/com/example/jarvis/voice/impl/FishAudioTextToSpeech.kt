package com.example.jarvis.voice.impl

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.jarvis.voice.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class FishAudioTextToSpeech(
    private val context: Context,
    private val apiKeyProvider: () -> String,
    private val modelIdProvider: () -> String,
    private var speechRate: Float = 1.0f,
    private var speechPitch: Float = 1.0f
) : TextToSpeechEngine {

    companion object {
        private const val TAG = "FishAudioTTS"
        private const val FISH_TTS_URL = "https://api.fish.audio/v1/tts"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingAudio = false

    override val isSpeaking: Boolean
        get() = isPlayingAudio || (mediaPlayer?.isPlaying ?: false)

    override fun speak(
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()

        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            onError("Fish Audio API key is not configured")
            return
        }

        // Enforce JARVIS signature conversational ending
        val trimmed = text.trim()
        val speechText = if (!trimmed.lowercase(Locale.ENGLISH).endsWith("sir.") &&
            !trimmed.lowercase(Locale.ENGLISH).endsWith("sir")
        ) {
            val base = trimmed.removeSuffix(".")
            "$base, sir."
        } else {
            trimmed
        }

        val modelId = modelIdProvider().trim()

        currentJob = scope.launch {
            try {
                val jsonPayload = JSONObject().apply {
                    put("text", speechText)
                    if (modelId.isNotBlank()) {
                        put("reference_id", modelId)
                    }
                    put("format", "mp3")
                    put("latency", "normal")
                }.toString()

                val request = Request.Builder()
                    .url(FISH_TTS_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.w(TAG, "Fish Audio HTTP error: ${response.code} -> $errorBody")
                    withContext(Dispatchers.Main) {
                        onError("Fish Audio error HTTP ${response.code}: $errorBody")
                    }
                    return@launch
                }

                val responseBody = response.body
                if (responseBody == null) {
                    withContext(Dispatchers.Main) {
                        onError("Empty response body from Fish Audio")
                    }
                    return@launch
                }

                // Write audio to temporary cache file
                val tempFile = File(context.cacheDir, "fish_speech_${System.currentTimeMillis()}.mp3")
                responseBody.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    playAudioFile(tempFile, onStart, onDone, onError)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Fish Audio synthesis failed", e)
                withContext(Dispatchers.Main) {
                    onError("Fish Audio failure: ${e.message}")
                }
            }
        }
    }

    private fun playAudioFile(
        file: File,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build()
                )
                setDataSource(file.absolutePath)

                setOnPreparedListener { player ->
                    try {
                        isPlayingAudio = true
                        onStart()
                        try {
                            if (speechRate != 1.0f) {
                                player.playbackParams = player.playbackParams.setSpeed(speechRate)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to set playback params: ${e.message}")
                        }
                        player.start()
                    } catch (e: Exception) {
                        isPlayingAudio = false
                        onError("Playback start failed: ${e.message}")
                    }
                }

                setOnCompletionListener { player ->
                    isPlayingAudio = false
                    onDone()
                    player.release()
                    mediaPlayer = null
                    file.delete()
                }

                setOnErrorListener { player, what, extra ->
                    isPlayingAudio = false
                    onError("MediaPlayer error code $what extra $extra")
                    player.release()
                    mediaPlayer = null
                    file.delete()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            isPlayingAudio = false
            onError("Failed to initialize MediaPlayer: ${e.message}")
            file.delete()
        }
    }

    override fun stop() {
        currentJob?.cancel()
        currentJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
            isPlayingAudio = false
        }
    }

    override fun setSpeed(speed: Float) {
        this.speechRate = speed
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error adjusting playback speed: ${e.message}")
        }
    }

    override fun setPitch(pitch: Float) {
        this.speechPitch = pitch
    }

    override fun shutdown() {
        stop()
        try {
            context.cacheDir.listFiles { _, name ->
                name.startsWith("fish_speech_") && name.endsWith(".mp3")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning cache: ${e.message}")
        }
    }
}
