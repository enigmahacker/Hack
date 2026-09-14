package com.example.jarvis.tools.impl.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class BatteryTool : Tool {
    override val name: String = "get_battery_status"
    override val description: String = "Retrieves the current Android battery level percentage and charging status."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{"type":"object","properties":{}}"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val plugSource = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC power"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless charging"
                else -> "battery"
            }

            val chargingDesc = if (isCharging) "currently charging via $plugSource" else "not charging"
            val output = "Battery is at $batteryPct percent and is $chargingDesc."

            ToolResult(
                isSuccess = true,
                output = output,
                data = mapOf(
                    "percentage" to batteryPct,
                    "isCharging" to isCharging,
                    "source" to plugSource
                )
            )
        } catch (e: Exception) {
            ToolResult(
                isSuccess = false,
                output = "Unable to read battery telemetry: ${e.localizedMessage}"
            )
        }
    }
}
