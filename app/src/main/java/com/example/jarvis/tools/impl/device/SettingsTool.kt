package com.example.jarvis.tools.impl.device

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class SettingsTool : Tool {
    override val name: String = "open_settings"
    override val description: String = "Opens system settings pages. Page options: 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'apps', 'main'."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "page": {"type": "string", "enum": ["wifi", "bluetooth", "display", "sound", "battery", "apps", "main"], "default": "main"}
        }
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        return try {
            val page = params["page"]?.toString()?.lowercase() ?: "main"
            val action = when (page) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound" -> Settings.ACTION_SOUND_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }

            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(isSuccess = true, output = "Opened $page settings.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Could not launch settings: ${e.message}")
        }
    }
}
