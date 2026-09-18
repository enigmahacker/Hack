package com.example.jarvis.tools

import android.content.Context
import com.example.jarvis.data.TaskRepository
import com.example.jarvis.tools.impl.apps.LaunchUrlTool
import com.example.jarvis.tools.impl.apps.OpenAppTool
import com.example.jarvis.tools.impl.communication.CallContactTool
import com.example.jarvis.tools.impl.communication.ComposeSmsTool
import com.example.jarvis.tools.impl.communication.FindContactTool
import com.example.jarvis.tools.impl.device.BatteryTool
import com.example.jarvis.tools.impl.device.DeviceInfoTool
import com.example.jarvis.tools.impl.device.MediaControlTool
import com.example.jarvis.tools.impl.device.NetworkTool
import com.example.jarvis.tools.impl.device.SettingsTool
import com.example.jarvis.tools.impl.device.VolumeTool
import com.example.jarvis.tools.impl.reminders.CompleteReminderTool
import com.example.jarvis.tools.impl.reminders.CreateReminderTool
import com.example.jarvis.tools.impl.reminders.GetRemindersTool
import com.example.jarvis.tools.impl.time.GetTimeTool
import com.example.jarvis.tools.impl.time.SetAlarmTool
import com.example.jarvis.tools.impl.time.SetTimerTool
import com.example.jarvis.tools.impl.web.WebSearchTool
import com.example.jarvis.tools.models.ToolResult

class ToolRegistry(repository: TaskRepository) {
    private val toolsMap = mutableMapOf<String, Tool>()

    init {
        // Device Tools
        register(BatteryTool())
        register(DeviceInfoTool())
        register(NetworkTool())
        register(VolumeTool())
        register(SettingsTool())
        register(MediaControlTool())

        // Application Tools
        register(OpenAppTool())
        register(LaunchUrlTool())

        // Time Tools
        register(GetTimeTool())
        register(SetTimerTool())
        register(SetAlarmTool())

        // Reminders & Task Tools
        register(CreateReminderTool(repository))
        register(GetRemindersTool(repository))
        register(CompleteReminderTool(repository))

        // Communication Tools
        register(FindContactTool())
        register(CallContactTool())
        register(ComposeSmsTool())

        // Web Tools
        register(WebSearchTool())
    }

    fun register(tool: Tool) {
        toolsMap[tool.name] = tool
    }

    fun getTool(name: String): Tool? = toolsMap[name]

    fun getAllTools(): List<Tool> = toolsMap.values.toList()

    suspend fun executeTool(context: Context, name: String, params: Map<String, Any?>): ToolResult {
        val tool = getTool(name)
            ?: return ToolResult(isSuccess = false, output = "Tool '$name' not recognized.")

        return try {
            tool.execute(context, params)
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Tool execution error: ${e.message}")
        }
    }
}
