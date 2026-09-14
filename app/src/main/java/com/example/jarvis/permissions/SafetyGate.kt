package com.example.jarvis.permissions

import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

object SafetyGate {

    sealed class SafetyCheckResult {
        object Allowed : SafetyCheckResult()
        data class RequiresConfirmation(val prompt: String) : SafetyCheckResult()
        data class Blocked(val reason: String) : SafetyCheckResult()
    }

    fun checkSafety(tool: Tool, params: Map<String, Any?>): SafetyCheckResult {
        when (tool.riskLevel) {
            RiskLevel.SAFE -> return SafetyCheckResult.Allowed
            RiskLevel.SENSITIVE -> return SafetyCheckResult.Allowed
            RiskLevel.CONFIRM -> {
                val prompt = when (tool.name) {
                    "call_phone" -> {
                        val number = params["phone_number"] ?: "the contact"
                        "Are you sure you want to place a call to $number, sir?"
                    }
                    "compose_sms" -> {
                        val number = params["phone_number"] ?: "the contact"
                        "Shall I send this message to $number, sir?"
                    }
                    else -> "Are you certain you wish to proceed with ${tool.name}, sir?"
                }
                return SafetyCheckResult.RequiresConfirmation(prompt)
            }
            RiskLevel.BLOCKED -> {
                return SafetyCheckResult.Blocked("Android security policies strictly prohibit this operation, sir.")
            }
        }
    }
}
