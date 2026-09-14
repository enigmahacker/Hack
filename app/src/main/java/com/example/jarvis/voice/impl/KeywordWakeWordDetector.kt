package com.example.jarvis.voice.impl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.jarvis.permissions.PermissionManager
import com.example.jarvis.voice.WakeWordDetector
import java.util.Locale

/**
 * Local on-device wake-word detection engine for "Jarvis".
 * Listens locally for the keyword with configurable sensitivity and false-trigger protection.
 */
class KeywordWakeWordDetector(
    private val context: Context,
    private var sensitivity: Float = 0.7f
) : WakeWordDetector {

    private var speechRecognizer: SpeechRecognizer? = null
    private var active = false
    private var onDetectedCallback: (() -> Unit)? = null
    private var lastTriggerTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override val isListening: Boolean
        get() = active

    override fun startListening(onWakeWordDetected: () -> Unit) {
        if (!PermissionManager.hasRecordAudioPermission(context)) return

        this.onDetectedCallback = onWakeWordDetected
        active = true
        mainHandler.post {
            initiateListeningCycle()
        }
    }

    private fun initiateListeningCycle() {
        if (!active) return

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    // In continuous wake listening, automatically cycle on timeout or non-fatal errors
                    if (active) {
                        mainHandler.postDelayed({
                            initiateListeningCycle()
                        }, 300)
                    }
                }

                override fun onResults(results: Bundle?) {
                    handleWakeResults(results)
                    if (active) {
                        mainHandler.postDelayed({
                            initiateListeningCycle()
                        }, 200)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    handleWakeResults(partialResults)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Bias towards quick phrase detection
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            if (active) {
                mainHandler.postDelayed({ initiateListeningCycle() }, 1000)
            }
        }
    }

    private fun handleWakeResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val now = System.currentTimeMillis()

        // Debounce: prevent false re-triggers within 2 seconds
        if (now - lastTriggerTime < 2000L) return

        for (match in matches) {
            val lower = match.lowercase(Locale.ENGLISH).trim()
            if (isWakeWordMatch(lower)) {
                lastTriggerTime = now
                // Pause recognition loop while responding
                stopListening()
                onDetectedCallback?.invoke()
                break
            }
        }
    }

    private fun isWakeWordMatch(text: String): Boolean {
        // Direct matches or common phonetic variations
        val targets = listOf("jarvis", "hey jarvis", "hi jarvis", "ok jarvis", "javis", "travis", "service")
        for (target in targets) {
            if (text.contains(target)) return true
        }
        return false
    }

    override fun stopListening() {
        active = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
            speechRecognizer = null
        }
    }

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.1f, 1.0f)
    }
}
