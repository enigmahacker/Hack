package com.example.jarvis.tools.impl.reminders

import android.content.Context
import com.example.jarvis.data.TaskRepository
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class CompleteReminderTool(private val repository: TaskRepository) : Tool {
    override val name: String = "complete_reminder"
    override val description: String = "Marks a reminder or to-do task as completed by title keyword or ID."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "query": {"type": "string", "description": "The title keyword or ID of the task to complete."}
        },
        "required": ["query"]
    }"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val query = params["query"]?.toString()?.trim()
            ?: return ToolResult(isSuccess = false, output = "Task title or ID is required.")

        val success = repository.completeTask(query)
        return if (success) {
            ToolResult(isSuccess = true, output = "Marked reminder '$query' as completed.")
        } else {
            ToolResult(isSuccess = false, output = "Could not find an active reminder matching '$query'.")
        }
    }
}
