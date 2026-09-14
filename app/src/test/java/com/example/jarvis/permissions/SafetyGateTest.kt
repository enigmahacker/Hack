package com.example.jarvis.permissions

import android.content.Context
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGateTest {

    private class MockTool(
        override val name: String,
        override val riskLevel: RiskLevel
    ) : Tool {
        override val description: String = "Mock"
        override val parametersJsonSchema: String = "{}"
        override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult {
            return ToolResult(isSuccess = true, output = "Success")
        }
    }

    @Test
    fun testSafeToolAllowed() {
        val tool = MockTool("battery", RiskLevel.SAFE)
        val result = SafetyGate.checkSafety(tool, emptyMap())
        assertTrue(result is SafetyGate.SafetyCheckResult.Allowed)
    }

    @Test
    fun testConfirmToolRequiresConfirmation() {
        val tool = MockTool("call_phone", RiskLevel.CONFIRM)
        val result = SafetyGate.checkSafety(tool, mapOf("phone_number" to "123456"))
        assertTrue(result is SafetyGate.SafetyCheckResult.RequiresConfirmation)
    }

    @Test
    fun testBlockedToolIsBlocked() {
        val tool = MockTool("arbitrary_shell", RiskLevel.BLOCKED)
        val result = SafetyGate.checkSafety(tool, emptyMap())
        assertTrue(result is SafetyGate.SafetyCheckResult.Blocked)
    }
}
