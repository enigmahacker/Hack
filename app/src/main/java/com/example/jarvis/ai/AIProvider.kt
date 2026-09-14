package com.example.jarvis.ai

import com.example.jarvis.ai.models.AIResponse
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.tools.Tool

interface AIProvider {
    val name: String
    suspend fun generateResponse(
        messages: List<ChatMessage>,
        memories: List<MemoryItem>,
        availableTools: List<Tool>
    ): AIResponse
}
