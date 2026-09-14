package com.example.jarvis.tools.impl.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class LaunchUrlTool : Tool {
    override val name: String = "open_url"
    override val description: String = "Opens a web address in the user's default browser."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "url": {"type": "string", "description": "The URL to open."}
        },
        "required": ["url"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        var rawUrl = params["url"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "URL parameter is missing.")

        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://$rawUrl"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(isSuccess = true, output = "Opened $rawUrl.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Unable to open link: ${e.message}")
        }
    }
}
