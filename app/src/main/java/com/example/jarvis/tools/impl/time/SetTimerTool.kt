package com.example.jarvis.tools.impl.time

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class SetTimerTool : Tool {
    override val name: String = "set_timer"
    override val description: String = "Sets a countdown timer for a specified duration in seconds or minutes."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "duration_seconds": {"type": "integer", "description": "Timer length in seconds."},
            "label": {"type": "string", "description": "Optional label for the timer.", "default": "JARVIS Timer"}
        },
        "required": ["duration_seconds"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val seconds = (params["duration_seconds"] as? Number)?.toInt()
            ?: return ToolResult(isSuccess = false, output = "Duration in seconds is required.")
        val label = params["label"]?.toString() ?: "JARVIS Timer"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val min = seconds / 60
            val sec = seconds % 60
            val durDesc = if (min > 0) "$min minute${if (min > 1) "s" else ""} and $sec second${if (sec != 1) "s" else ""}" else "$sec seconds"
            ToolResult(isSuccess = true, output = "Timer set for $durDesc.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Could not set timer: ${e.message}")
        }
    }
}
