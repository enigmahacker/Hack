package com.example.jarvis.voice.impl

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.jarvis.voice.TextToSpeechEngine
import java.util.Locale
import java.util.UUID

class AndroidTextToSpeech(
    context: Context,
    private var speechRate: Float = 1.05f,
    private var speechPitch: Float = 0.95f
) : TextToSpeechEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speakingState = false

    override val isSpeaking: Boolean
        get() = speakingState || (tts?.isSpeaking ?: false)

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(speechPitch)
                isInitialized = true
            }
        }
    }

    override fun speak(
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isInitialized || tts == null) {
            onError("TTS engine is initializing...")
            return
        }

        // Format to ensure JARVIS personality ending: naturally closes with ", sir."
        val trimmed = text.trim()
        val speechText = if (!trimmed.lowercase(Locale.ENGLISH).endsWith("sir.") &&
            !trimmed.lowercase(Locale.ENGLISH).endsWith("sir")
        ) {
            val base = trimmed.removeSuffix(".")
            "$base, sir."
        } else {
            trimmed
        }

        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (id == utteranceId) {
                    speakingState = true
                    onStart()
                }
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    speakingState = false
                    onDone()
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    speakingState = false
                    onError("Speech playback failed")
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    speakingState = false
                    onError("Speech error code: $errorCode")
                }
            }
        })

        val params = Bundle()
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    override fun stop() {
        tts?.stop()
        speakingState = false
    }

    override fun setSpeed(speed: Float) {
        this.speechRate = speed
        tts?.setSpeechRate(speed)
    }

    override fun setPitch(pitch: Float) {
        this.speechPitch = pitch
        tts?.setPitch(pitch)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        speakingState = false
    }
}
