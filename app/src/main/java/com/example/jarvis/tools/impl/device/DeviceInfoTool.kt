package com.example.jarvis.tools.impl.device

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult
import java.util.concurrent.TimeUnit

class DeviceInfoTool : Tool {
    override val name: String = "get_device_info"
    override val description: String = "Retrieves device specifications, Android version, manufacturer, and uptime."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{"type":"object","properties":{}}"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            val osVersion = Build.VERSION.RELEASE
            val sdkInt = Build.VERSION.SDK_INT
            val uptimeHours = TimeUnit.MILLISECONDS.toHours(SystemClock.elapsedRealtime())

            val output = "Device is a $manufacturer $model running Android $osVersion (API $sdkInt). System uptime is $uptimeHours hours."
            ToolResult(
                isSuccess = true,
                output = output,
                data = mapOf(
                    "manufacturer" to manufacturer,
                    "model" to model,
                    "version" to osVersion,
                    "sdk" to sdkInt,
                    "uptimeHours" to uptimeHours
                )
            )
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Error querying device info: ${e.message}")
        }
    }
}
