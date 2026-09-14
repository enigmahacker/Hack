package com.example.jarvis.tools.impl.device

import android.content.Context
import android.media.AudioManager
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class VolumeTool : Tool {
    override val name: String = "control_volume"
    override val description: String = "Reads or sets the media/ring volume level. Provide action 'get' or 'set' with level (0-100)."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "action": {"type": "string", "enum": ["get", "set", "mute", "unmute"]},
            "stream": {"type": "string", "enum": ["media", "ring", "alarm"], "default": "media"},
            "level": {"type": "integer", "description": "Volume percentage from 0 to 100"}
        },
        "required": ["action"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ToolResult(isSuccess = false, output = "Audio service unavailable.")

            val streamType = when (params["stream"]?.toString()?.lowercase()) {
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                else -> AudioManager.STREAM_MUSIC
            }

            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val currentVolume = audioManager.getStreamVolume(streamType)
            val currentPct = (currentVolume * 100) / maxVolume

            val action = params["action"]?.toString()?.lowercase() ?: "get"

            when (action) {
                "set" -> {
                    val targetPct = (params["level"] as? Number)?.toInt() ?: currentPct
                    val targetIndex = ((targetPct.coerceIn(0, 100) * maxVolume) / 100).coerceIn(0, maxVolume)
                    audioManager.setStreamVolume(streamType, targetIndex, AudioManager.FLAG_SHOW_UI)
                    ToolResult(isSuccess = true, output = "Volume adjusted to $targetPct percent.")
                }
                "mute" -> {
                    audioManager.setStreamVolume(streamType, 0, AudioManager.FLAG_SHOW_UI)
                    ToolResult(isSuccess = true, output = "Volume muted.")
                }
                "unmute" -> {
                    val halfIndex = maxVolume / 2
                    audioManager.setStreamVolume(streamType, halfIndex, AudioManager.FLAG_SHOW_UI)
                    ToolResult(isSuccess = true, output = "Volume restored to 50 percent.")
                }
                else -> {
                    ToolResult(isSuccess = true, output = "Current volume is at $currentPct percent.")
                }
            }
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Failed to adjust volume: ${e.message}")
        }
    }
}
