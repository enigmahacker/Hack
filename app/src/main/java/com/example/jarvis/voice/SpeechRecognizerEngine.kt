package com.example.jarvis.voice

interface SpeechRecognizerEngine {
    val isListening: Boolean
    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onRmsChanged: (Float) -> Unit = {},
        onError: (String) -> Unit
    )
    fun stopListening()
    fun cancel()
}
