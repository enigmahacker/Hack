package com.example.jarvis.tools.impl.reminders

import android.content.Context
import com.example.jarvis.data.TaskRepository
import com.example.jarvis.data.model.JarvisTask
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class CreateReminderTool(private val repository: TaskRepository) : Tool {
    override val name: String = "create_reminder"
    override val description: String = "Creates a reminder or to-do task with a title and optional notes."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "title": {"type": "string", "description": "Title or description of the reminder."},
            "notes": {"type": "string", "description": "Optional additional notes."}
        },
        "required": ["title"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val title = params["title"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "Reminder title is missing.")
        val notes = params["notes"]?.toString() ?: ""

        val task = JarvisTask(title = title, notes = notes)
        val id = repository.addTask(task)

        return ToolResult(isSuccess = true, output = "Reminder created: '$title'.", data = mapOf("id" to id))
    }
}
