package com.example.jarvis.voice.impl

import android.util.Log
import com.example.jarvis.voice.TextToSpeechEngine

/**
 * Composite TTS engine that prioritizes neural Fish Audio synthesis with Jarvis voice model,
 * and automatically falls back to Android local TextToSpeech on network errors, missing API key,
 * or quota limitations.
 */
class CompositeTextToSpeech(
    val fishAudioTts: TextToSpeechEngine,
    val androidTts: TextToSpeechEngine,
    private val isFishAudioEnabled: () -> Boolean,
    private val hasFishAudioKey: () -> Boolean
) : TextToSpeechEngine {

    companion object {
        private const val TAG = "CompositeTTS"
    }

    override val isSpeaking: Boolean
        get() = fishAudioTts.isSpeaking || androidTts.isSpeaking

    override fun speak(
        text: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isFishAudioEnabled() && hasFishAudioKey()) {
            Log.d(TAG, "Attempting neural speech via Fish Audio...")
            fishAudioTts.speak(
                text = text,
                onStart = onStart,
                onDone = onDone,
                onError = { fishError ->
                    Log.w(TAG, "Fish Audio synthesis error ($fishError). Falling back to Android TTS.")
                    // Fallback seamlessly to local Android Text-To-Speech
                    androidTts.speak(
                        text = text,
                        onStart = onStart,
                        onDone = onDone,
                        onError = onError
                    )
                }
            )
        } else {
            Log.d(TAG, "Using Android local TTS (Fish Audio disabled or unconfigured)")
            androidTts.speak(
                text = text,
                onStart = onStart,
                onDone = onDone,
                onError = onError
            )
        }
    }

    override fun stop() {
        fishAudioTts.stop()
        androidTts.stop()
    }

    override fun setSpeed(speed: Float) {
        fishAudioTts.setSpeed(speed)
        androidTts.setSpeed(speed)
    }

    override fun setPitch(pitch: Float) {
        fishAudioTts.setPitch(pitch)
        androidTts.setPitch(pitch)
    }

    override fun shutdown() {
        fishAudioTts.shutdown()
        androidTts.shutdown()
    }
}
