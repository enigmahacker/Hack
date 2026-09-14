package com.example.jarvis.data.model

enum class MessageRole {
    USER,
    JARVIS,
    SYSTEM,
    TOOL
}

data class ChatMessage(
    val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val toolName: String? = null,
    val toolResult: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
