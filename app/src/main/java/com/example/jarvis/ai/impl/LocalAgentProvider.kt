package com.example.jarvis.ai.impl

import com.example.jarvis.ai.AIProvider
import com.example.jarvis.ai.models.AIResponse
import com.example.jarvis.ai.models.ToolCall
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.tools.Tool
import java.util.Locale

class LocalAgentProvider : AIProvider {
    override val name: String = "Local Engine"

    override suspend fun generateResponse(
        messages: List<ChatMessage>,
        memories: List<MemoryItem>,
        availableTools: List<Tool>
    ): AIResponse {
        val lastUserMsg = messages.lastOrNull { it.role == com.example.jarvis.data.model.MessageRole.USER }?.content
            ?: return AIResponse("At your service, sir.")

        val text = lastUserMsg.lowercase(Locale.ENGLISH).trim().removeSuffix(".")

        // 1. Battery Telemetry
        if (text.contains("battery") || text.contains("charge") || text.contains("power level") || text.contains("battery level")) {
            return AIResponse(
                content = "Querying power telemetry...",
                toolCalls = listOf(ToolCall("get_battery_status", emptyMap()))
            )
        }

        // 2. Open Application
        val openAppRegex = Regex("""(?i)(?:open|launch|start|run)\s+(?:the\s+)?([a-zA-Z0-9\s]+)""")
        val openAppMatch = openAppRegex.find(text)
        if (openAppMatch != null) {
            val rawApp = openAppMatch.groupValues[1].trim()
            if (!rawApp.contains("setting") && !rawApp.contains("timer") && !rawApp.contains("alarm")) {
                return AIResponse(
                    content = "Launching $rawApp...",
                    toolCalls = listOf(ToolCall("open_app", mapOf("app_name" to rawApp)))
                )
            }
        }

        // 3. Time & Date
        if (text.contains("time") || text.contains("date") || text.contains("what day") || text.contains("what is today")) {
            return AIResponse(
                content = "Checking chronometer...",
                toolCalls = listOf(ToolCall("get_current_time", emptyMap()))
            )
        }

        // 4. Timer
        if (text.contains("timer")) {
            val secRegex = Regex("""(\d+)\s*(?:second|sec)""")
            val minRegex = Regex("""(\d+)\s*(?:minute|min)""")
            val secMatch = secRegex.find(text)
            val minMatch = minRegex.find(text)

            var totalSeconds = 0
            if (minMatch != null) {
                totalSeconds += (minMatch.groupValues[1].toIntOrNull() ?: 0) * 60
            }
            if (secMatch != null) {
                totalSeconds += (secMatch.groupValues[1].toIntOrNull() ?: 0)
            }
            if (totalSeconds == 0) totalSeconds = 300 // default 5 min

            return AIResponse(
                content = "Configuring timer...",
                toolCalls = listOf(ToolCall("set_timer", mapOf("duration_seconds" to totalSeconds)))
            )
        }

        // 5. Alarm
        if (text.contains("alarm")) {
            val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
            val match = timeRegex.find(text)
            var hour = 7
            var minute = 0
            if (match != null) {
                val h = match.groupValues[1].toIntOrNull() ?: 7
                val m = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3].lowercase()
                hour = when {
                    ampm == "pm" && h < 12 -> h + 12
                    ampm == "am" && h == 12 -> 0
                    else -> h
                }
                minute = m
            }
            return AIResponse(
                content = "Setting alarm...",
                toolCalls = listOf(ToolCall("set_alarm", mapOf("hour" to hour, "minute" to minute)))
            )
        }

        // 6. Device Info
        if (text.contains("device info") || text.contains("system status") || text.contains("device specifications") || text.contains("what phone")) {
            return AIResponse(
                content = "Diagnostic check initialized...",
                toolCalls = listOf(ToolCall("get_device_info", emptyMap()))
            )
        }

        // 7. Network Status
        if (text.contains("network") || text.contains("wifi") || text.contains("connection") || text.contains("internet")) {
            return AIResponse(
                content = "Checking network interfaces...",
                toolCalls = listOf(ToolCall("get_network_status", emptyMap()))
            )
        }

        // 8. Volume Control
        if (text.contains("volume")) {
            if (text.contains("mute")) {
                return AIResponse(
                    content = "Muting audio...",
                    toolCalls = listOf(ToolCall("control_volume", mapOf("action" to "mute")))
                )
            }
            val numRegex = Regex("""(\d{1,3})""")
            val numMatch = numRegex.find(text)
            if (numMatch != null && (text.contains("set") || text.contains("to"))) {
                val level = numMatch.groupValues[1].toIntOrNull() ?: 50
                return AIResponse(
                    content = "Adjusting volume...",
                    toolCalls = listOf(ToolCall("control_volume", mapOf("action" to "set", "level" to level)))
                )
            }
            return AIResponse(
                content = "Checking volume levels...",
                toolCalls = listOf(ToolCall("control_volume", mapOf("action" to "get")))
            )
        }

        // 9. Media Playback
        if (text.contains("pause") || text.contains("stop music") || text.contains("resume") || text.contains("play music") || text.contains("next song") || text.contains("skip")) {
            val cmd = when {
                text.contains("pause") || text.contains("stop music") -> "pause"
                text.contains("resume") || text.contains("play") -> "play"
                text.contains("next") || text.contains("skip") -> "next"
                else -> "play_pause"
            }
            return AIResponse(
                content = "Controlling media...",
                toolCalls = listOf(ToolCall("control_media", mapOf("command" to cmd)))
            )
        }

        // 10. Settings
        if (text.contains("setting")) {
            val page = when {
                text.contains("wifi") -> "wifi"
                text.contains("bluetooth") -> "bluetooth"
                text.contains("display") -> "display"
                text.contains("sound") -> "sound"
                text.contains("battery") -> "battery"
                text.contains("app") -> "apps"
                else -> "main"
            }
            return AIResponse(
                content = "Opening system settings...",
                toolCalls = listOf(ToolCall("open_settings", mapOf("page" to page)))
            )
        }

        // 11. Reminders & Tasks
        if (text.contains("remind me to") || text.contains("create reminder") || text.contains("add reminder")) {
            val reminderTitle = text.substringAfter("remind me to ")
                .substringAfter("create reminder ")
                .substringAfter("add reminder ")
            return AIResponse(
                content = "Saving reminder...",
                toolCalls = listOf(ToolCall("create_reminder", mapOf("title" to reminderTitle)))
            )
        }
        if (text.contains("reminders") || text.contains("tasks") || text.contains("to do")) {
            return AIResponse(
                content = "Retrieving tasks...",
                toolCalls = listOf(ToolCall("get_reminders", emptyMap()))
            )
        }

        // 12. Web Search / Information Query
        if (text.startsWith("who is") || text.startsWith("what is") || text.startsWith("search") || text.contains("weather")) {
            return AIResponse(
                content = "Consulting external sources...",
                toolCalls = listOf(ToolCall("web_search", mapOf("query" to text)))
            )
        }

        // 13. Conversational / JARVIS Persona
        if (text == "jarvis" || text == "hello" || text == "hi" || text == "hey" || text == "are you there") {
            return AIResponse("At your service, sir. How may I assist you?")
        }
        if (text.contains("who are you") || text.contains("what are you")) {
            return AIResponse("I am JARVIS, your personal artificial intelligence assistant for Android, sir.")
        }
        if (text.contains("thank") || text.contains("thanks")) {
            return AIResponse("Always a pleasure to be of assistance, sir.")
        }

        // Default conversational response
        return AIResponse("I am standing by and ready for your command, sir.")
    }
}
