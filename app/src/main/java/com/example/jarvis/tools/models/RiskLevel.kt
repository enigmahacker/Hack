package com.example.jarvis.tools.models

enum class RiskLevel {
    SAFE,       // e.g. read battery, get time, open installed app
    CONFIRM,    // e.g. make a phone call, delete reminder, compose SMS
    SENSITIVE,  // e.g. read personal contacts, read calendar
    BLOCKED     // e.g. arbitrary shell execution or unsafe OS tampering
}

data class ToolResult(
    val isSuccess: Boolean,
    val output: String,
    val data: Map<String, Any?>? = null,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)
