package com.example.jarvis.voice

interface TextToSpeechEngine {
    val isSpeaking: Boolean
    fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    fun stop()
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun shutdown()
}
