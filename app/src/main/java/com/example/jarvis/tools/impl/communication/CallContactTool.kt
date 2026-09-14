package com.example.jarvis.tools.impl.communication

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class CallContactTool : Tool {
    override val name: String = "call_phone"
    override val description: String = "Initiates a phone call to a specified phone number or contact. Requires confirmation."
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "phone_number": {"type": "string", "description": "The phone number to call."},
            "contact_name": {"type": "string", "description": "Optional name of the person."}
        },
        "required": ["phone_number"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val phoneNumber = params["phone_number"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "Phone number is required.")
        val contactName = params["contact_name"]?.toString() ?: phoneNumber

        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(isSuccess = true, output = "Calling $contactName at $phoneNumber.")
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Unable to initiate call: ${e.message}")
        }
    }
}
