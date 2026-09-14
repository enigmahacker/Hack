package com.example.jarvis.ai.models

data class ToolCall(
    val name: String,
    val arguments: Map<String, Any?>
)

data class AIResponse(
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val isFinished: Boolean = true
)
