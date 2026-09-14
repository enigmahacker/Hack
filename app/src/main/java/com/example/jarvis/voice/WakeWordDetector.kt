package com.example.jarvis.voice

interface WakeWordDetector {
    val isListening: Boolean
    fun startListening(onWakeWordDetected: () -> Unit)
    fun stopListening()
    fun setSensitivity(sensitivity: Float)
}
