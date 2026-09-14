package com.example.jarvis.tools.impl.time

import android.content.Context
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GetTimeTool : Tool {
    override val name: String = "get_current_time"
    override val description: String = "Gets the current time, date, day of the week, and timezone."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{"type":"object","properties":{}}"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val tz = TimeZone.getDefault().displayName

        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)

        val output = "The time is $timeStr on $dateStr ($tz)."
        return ToolResult(
            isSuccess = true,
            output = output,
            data = mapOf(
                "time" to timeStr,
                "date" to dateStr,
                "timestamp" to now.time
            )
        )
    }
}
