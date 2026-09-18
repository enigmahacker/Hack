package com.example.jarvis.tools

import com.example.jarvis.data.TaskRepository
import com.example.jarvis.data.model.JarvisTask
import com.example.jarvis.tools.models.RiskLevel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    private class FakeTaskRepository : TaskRepository {
        override suspend fun addTask(task: JarvisTask): Long = 1L
        override suspend fun getTasks(): List<JarvisTask> = emptyList()
        override suspend fun completeTask(query: String): Boolean = true
        override suspend fun deleteTask(id: Long): Int = 1
    }

    @Test
    fun testAllToolsRegisteredAndHaveValidSchemas() {
        val fakeRepo = FakeTaskRepository()
        val registry = ToolRegistry(fakeRepo)
        val tools = registry.getAllTools()

        assertTrue(tools.size >= 16)

        // Check key tools are present
        assertNotNull(registry.getTool("get_battery_status"))
        assertNotNull(registry.getTool("open_app"))
        assertNotNull(registry.getTool("get_current_time"))
        assertNotNull(registry.getTool("set_timer"))
        assertNotNull(registry.getTool("set_alarm"))
        assertNotNull(registry.getTool("create_reminder"))
        assertNotNull(registry.getTool("call_phone"))
        assertNotNull(registry.getTool("compose_sms"))
        assertNotNull(registry.getTool("web_search"))

        // Validate JSON Schema format
        for (tool in tools) {
            assertTrue(tool.parametersJsonSchema.startsWith("{"))
            assertTrue(tool.parametersJsonSchema.contains("\"type\""))
        }

        // Validate risk levels
        assertEquals(RiskLevel.CONFIRM, registry.getTool("call_phone")?.riskLevel)
        assertEquals(RiskLevel.CONFIRM, registry.getTool("compose_sms")?.riskLevel)
        assertEquals(RiskLevel.SAFE, registry.getTool("get_battery_status")?.riskLevel)
        assertEquals(RiskLevel.SAFE, registry.getTool("get_current_time")?.riskLevel)
    }
}
