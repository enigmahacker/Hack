package com.example.jarvis.tools

import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.tools.models.RiskLevel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class ToolRegistryTest {

    @Test
    fun testAllToolsRegisteredAndHaveValidSchemas() {
        val mockRepo = mock(JarvisRepository::class.java)
        val registry = ToolRegistry(mockRepo)
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

        // Validate JSON Schema validity
        for (tool in tools) {
            val json = JSONObject(tool.parametersJsonSchema)
            assertEquals("object", json.getString("type"))
        }

        // Validate risk levels
        assertEquals(RiskLevel.CONFIRM, registry.getTool("call_phone")?.riskLevel)
        assertEquals(RiskLevel.CONFIRM, registry.getTool("compose_sms")?.riskLevel)
        assertEquals(RiskLevel.SAFE, registry.getTool("get_battery_status")?.riskLevel)
        assertEquals(RiskLevel.SAFE, registry.getTool("get_current_time")?.riskLevel)
    }
}
