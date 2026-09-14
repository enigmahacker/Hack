package com.example.jarvis.tools

import android.content.Context
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult

interface Tool {
    val name: String
    val description: String
    val riskLevel: RiskLevel
    val requiredPermissions: List<String> get() = emptyList()

    /**
     * JSON Schema describing the input parameters for LLM function calling
     */
    val parametersJsonSchema: String

    suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult
}
