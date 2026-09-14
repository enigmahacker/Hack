package com.example.jarvis.agent

enum class JarvisState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    ERROR
}

data class JarvisUiState(
    val state: JarvisState = JarvisState.IDLE,
    val transcript: String = "",
    val spokenText: String = "",
    val activeToolName: String? = null,
    val rmsLevel: Float = 0f,
    val isWakeWordActive: Boolean = true,
    val errorMessage: String? = null,
    val pendingConfirmation: PendingConfirmation? = null
)

data class PendingConfirmation(
    val prompt: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)
