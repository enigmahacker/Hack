package com.example.jarvis.tools.impl.communication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class FindContactTool : Tool {
    override val name: String = "find_contact"
    override val description: String = "Searches device contacts by name and returns phone numbers."
    override val riskLevel: RiskLevel = RiskLevel.SENSITIVE
    override val requiredPermissions: List<String> = listOf(Manifest.permission.READ_CONTACTS)
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "name": {"type": "string", "description": "The contact name to search for."}
        },
        "required": ["name"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val nameQuery = params["name"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "Contact name is required.")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return ToolResult(
                isSuccess = false,
                output = "Contacts permission is not granted. Please grant contacts access in Settings."
            )
        }

        return try {
            val resolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$nameQuery%")

            val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            val results = mutableListOf<String>()

            cursor?.use {
                val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && results.size < 5) {
                    val name = it.getString(nameIdx)
                    val num = it.getString(numIdx)
                    results.add("$name: $num")
                }
            }

            if (results.isEmpty()) {
                ToolResult(isSuccess = true, output = "No contacts found matching '$nameQuery'.")
            } else {
                ToolResult(isSuccess = true, output = "Found contact: ${results.joinToString(", ")}.")
            }
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Failed to query contacts: ${e.message}")
        }
    }
}
