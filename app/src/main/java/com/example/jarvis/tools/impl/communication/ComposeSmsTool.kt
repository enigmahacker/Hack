package com.example.jarvis.tools.impl.communication

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class ComposeSmsTool : Tool {
    override val name: String = "compose_sms"
    override val description: String = "Prepares an SMS text message with recipient and message body. Requires confirmation."
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "phone_number": {"type": "string", "description": "The recipient's phone number."},
            "message": {"type": "string", "description": "The text message content to send."}
        },
        "required": ["phone_number", "message"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val phoneNumber = params["phone_number"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "Phone number is required.")
        val message = params["message"]?.toString() ?: ""

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(isSuccess = true, output = "Composed SMS for $phoneNumber.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Could not open SMS composer: ${e.message}")
        }
    }
}
