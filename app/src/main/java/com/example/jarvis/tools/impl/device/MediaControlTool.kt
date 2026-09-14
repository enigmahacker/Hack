package com.example.jarvis.tools.impl.device

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class MediaControlTool : Tool {
    override val name: String = "control_media"
    override val description: String = "Controls background media playback. Commands: 'play', 'pause', 'play_pause', 'next', 'previous'."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "command": {"type": "string", "enum": ["play", "pause", "play_pause", "next", "previous"]}
        },
        "required": ["command"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ToolResult(isSuccess = false, output = "Audio service unavailable.")

            val command = params["command"]?.toString()?.lowercase() ?: "play_pause"
            val keyCode = when (command) {
                "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
                "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
                "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }

            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

            ToolResult(isSuccess = true, output = "Executed media command: $command.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Media command failed: ${e.message}")
        }
    }
}
