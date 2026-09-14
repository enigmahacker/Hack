package com.example.jarvis.tools.impl.time

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class SetAlarmTool : Tool {
    override val name: String = "set_alarm"
    override val description: String = "Sets an alarm for a given hour (0-23) and minute (0-59)."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "hour": {"type": "integer", "description": "Hour of the day in 24h format (0-23)."},
            "minute": {"type": "integer", "description": "Minute of the hour (0-59).", "default": 0},
            "message": {"type": "string", "description": "Optional alarm label.", "default": "JARVIS Alarm"}
        },
        "required": ["hour"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val hour = (params["hour"] as? Number)?.toInt()
            ?: return ToolResult(isSuccess = false, output = "Alarm hour is required.")
        val minute = (params["minute"] as? Number)?.toInt() ?: 0
        val message = params["message"]?.toString() ?: "JARVIS Alarm"

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            val displayMin = String.format("%02d", minute)
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            ToolResult(isSuccess = true, output = "Alarm set for $displayHour:$displayMin $amPm.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Unable to set alarm: ${e.message}")
        }
    }
}
