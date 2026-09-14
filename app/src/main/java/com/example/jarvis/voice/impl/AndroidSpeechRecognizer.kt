package com.example.jarvis.voice.impl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.jarvis.voice.SpeechRecognizerEngine
import java.util.Locale

class AndroidSpeechRecognizer(private val context: Context) : SpeechRecognizerEngine {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override val isListening: Boolean
        get() = listening

    override fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onRmsChanged: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition not available on this device, sir.")
                return@post
            }

            cancel()

            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listening = false
                    }

                    override fun onError(error: Int) {
                        listening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error, sir."
                            SpeechRecognizer.ERROR_CLIENT -> "Client error, sir."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required, sir."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error, sir."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout, sir."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected, sir."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy, sir."
                            SpeechRecognizer.ERROR_SERVER -> "Server error, sir."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected, sir."
                            else -> "Recognition issue, sir."
                        }
                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onFinalResult(text)
                        } else {
                            onError("No speech recognized, sir.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            recognizer?.startListening(intent)
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            listening = false
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
            recognizer = null
            listening = false
        }
    }
}
