package com.example.jarvis.tools.impl.reminders

import android.content.Context
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

class GetRemindersTool(private val repository: JarvisRepository) : Tool {
    override val name: String = "get_reminders"
    override val description: String = "Retrieves current active reminders and to-do tasks."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{"type":"object","properties":{}}"""

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
        val tasks = repository.getTasks().filter { !it.isCompleted }
        if (tasks.isEmpty()) {
            return ToolResult(isSuccess = true, output = "You have no active reminders at the moment.")
        }

        val titles = tasks.joinToString(", ") { it.title }
        val output = "You have ${tasks.size} active reminder${if (tasks.size > 1) "s" else ""}: $titles."
        return ToolResult(isSuccess = true, output = output, data = mapOf("count" to tasks.size))
    }
}
