package com.example.jarvis.tools.impl.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class OpenAppTool : Tool {
    override val name: String = "open_app"
    override val description: String = "Opens an installed Android application by name (e.g., 'YouTube', 'Spotify', 'Chrome', 'Camera', 'Maps', 'Settings', 'Calculator')."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "app_name": {"type": "string", "description": "The common or partial name of the app to launch."}
        },
        "required": ["app_name"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val appName = params["app_name"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "App name is required.")

        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            // Direct package shortcuts for common apps
            val commonPackages = mapOf(
                "youtube" to "com.google.android.youtube",
                "chrome" to "com.android.chrome",
                "spotify" to "com.spotify.music",
                "camera" to "com.google.android.GoogleCamera",
                "maps" to "com.google.android.apps.maps",
                "gmail" to "com.google.android.gm",
                "clock" to "com.google.android.deskclock",
                "calculator" to "com.google.android.calculator",
                "photos" to "com.google.android.apps.photos",
                "play store" to "com.android.vending",
                "settings" to "com.android.settings"
            )

            val lower = appName.lowercase()
            val shortcutPkg = commonPackages[lower]
            if (shortcutPkg != null) {
                val launchIntent = pm.getLaunchIntentForPackage(shortcutPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(isSuccess = true, output = "Opened $appName.")
                }
            }

            // Search by label
            var targetPackage: String? = null
            var targetLabel: String = appName
            for (ri in resolveInfos) {
                val label = ri.loadLabel(pm).toString()
                if (label.equals(appName, ignoreCase = true)) {
                    targetPackage = ri.activityInfo.packageName
                    targetLabel = label
                    break
                }
            }

            // Substring fallback
            if (targetPackage == null) {
                for (ri in resolveInfos) {
                    val label = ri.loadLabel(pm).toString()
                    if (label.contains(appName, ignoreCase = true)) {
                        targetPackage = ri.activityInfo.packageName
                        targetLabel = label
                        break
                    }
                }
            }

            if (targetPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    ToolResult(isSuccess = true, output = "Opened $targetLabel.")
                } else {
                    ToolResult(isSuccess = false, output = "Could not find a launch activity for $targetLabel.")
                }
            } else {
                ToolResult(isSuccess = false, output = "I could not find '$appName' installed on this device.")
            }
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Failed to launch $appName: ${e.message}")
        }
    }
}
