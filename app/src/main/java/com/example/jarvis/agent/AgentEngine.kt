package com.example.jarvis.agent

import android.content.Context
import com.example.jarvis.ai.AIProvider
import com.example.jarvis.ai.impl.GeminiAIProvider
import com.example.jarvis.ai.impl.LocalAgentProvider
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MessageRole
import com.example.jarvis.memory.MemoryExtractor
import com.example.jarvis.permissions.SafetyGate
import com.example.jarvis.tools.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AgentEngine(
    private val repository: JarvisRepository,
    private val toolRegistry: ToolRegistry
) {
    private val localProvider = LocalAgentProvider()
    private val geminiProvider = GeminiAIProvider(repository.secureStorage)

    private fun getActiveProvider(): AIProvider {
        val configured = repository.preferences.aiProvider
        return when (configured) {
            "GEMINI" -> {
                if (repository.secureStorage.geminiApiKey.isNotBlank()) geminiProvider else localProvider
            }
            else -> localProvider
        }
    }

    suspend fun processUserPrompt(
        userInput: String,
        context: Context,
        onStateChange: (JarvisState, String?) -> Unit,
        onSpokenResponse: (String) -> Unit,
        onConfirmationNeeded: (PendingConfirmation) -> Unit
    ) = withContext(Dispatchers.Default) {
        val trimmedInput = userInput.trim()
        if (trimmedInput.isBlank()) {
            onStateChange(JarvisState.IDLE, null)
            return@withContext
        }

        // 1. Record User Message
        repository.saveMessage(ChatMessage(role = MessageRole.USER, content = trimmedInput))

        // 2. Check for explicit memory statements ("Remember that my project is called VAMANA")
        if (repository.preferences.memoryEnabled) {
            val memoryItem = MemoryExtractor.extractMemory(trimmedInput)
            if (memoryItem != null) {
                repository.saveMemory(memoryItem)
                val reply = "Understood. I have recorded that your ${memoryItem.key.replace("_", " ")} is ${memoryItem.value}, sir."
                recordAndSpeak(reply, onStateChange, onSpokenResponse)
                return@withContext
            }
        }

        onStateChange(JarvisState.THINKING, null)

        // 3. Gather Context & Memory
        val memories = if (repository.preferences.memoryEnabled) repository.getMemories() else emptyList()
        val recentMessages = repository.getMessages(10)
        val availableTools = toolRegistry.getAllTools()

        // 4. Query AI Provider
        val provider = getActiveProvider()
        val aiResponse = try {
            provider.generateResponse(recentMessages, memories, availableTools)
        } catch (e: Exception) {
            // Graceful fallback to local agent on network/API failure
            localProvider.generateResponse(recentMessages, memories, availableTools)
        }

        // 5. Handle Tool Calls if planned
        if (aiResponse.toolCalls.isNotEmpty()) {
            val toolCall = aiResponse.toolCalls.first()
            val tool = toolRegistry.getTool(toolCall.name)

            if (tool != null) {
                onStateChange(JarvisState.EXECUTING, tool.name)

                // Safety validation
                when (val safetyResult = SafetyGate.checkSafety(tool, toolCall.arguments)) {
                    is SafetyGate.SafetyCheckResult.Blocked -> {
                        val reply = safetyResult.reason
                        recordAndSpeak(reply, onStateChange, onSpokenResponse)
                        return@withContext
                    }
                    is SafetyGate.SafetyCheckResult.RequiresConfirmation -> {
                        val confirmation = PendingConfirmation(
                            prompt = safetyResult.prompt,
                            onConfirm = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    executeToolAndComplete(tool.name, toolCall.arguments, context, onStateChange, onSpokenResponse)
                                }
                            },
                            onCancel = {
                                CoroutineScope(Dispatchers.Default).launch {
                                    recordAndSpeak("Action canceled, sir.", onStateChange, onSpokenResponse)
                                }
                            }
                        )
                        onConfirmationNeeded(confirmation)
                        return@withContext
                    }
                    is SafetyGate.SafetyCheckResult.Allowed -> {
                        executeToolAndComplete(tool.name, toolCall.arguments, context, onStateChange, onSpokenResponse)
                        return@withContext
                    }
                }
            }
        }

        // Direct conversational answer
        val answer = aiResponse.content.ifBlank { "At your command, sir." }
        recordAndSpeak(answer, onStateChange, onSpokenResponse)
    }

    private suspend fun executeToolAndComplete(
        toolName: String,
        arguments: Map<String, Any?>,
        context: Context,
        onStateChange: (JarvisState, String?) -> Unit,
        onSpokenResponse: (String) -> Unit
    ) {
        onStateChange(JarvisState.EXECUTING, toolName)
        val result = toolRegistry.executeTool(context, toolName, arguments)

        val reply = if (result.isSuccess) {
            result.output
        } else {
            "I was unable to complete the action: ${result.output}, sir."
        }

        repository.saveMessage(
            ChatMessage(
                role = MessageRole.TOOL,
                content = reply,
                toolName = toolName,
                toolResult = result.output
            )
        )

        recordAndSpeak(reply, onStateChange, onSpokenResponse)
    }

    private suspend fun recordAndSpeak(
        rawText: String,
        onStateChange: (JarvisState, String?) -> Unit,
        onSpokenResponse: (String) -> Unit
    ) {
        // Enforce JARVIS spoken styling: naturally ends with ", sir."
        val trimmed = rawText.trim()
        val formatted = if (!trimmed.lowercase(Locale.ENGLISH).endsWith("sir.") &&
            !trimmed.lowercase(Locale.ENGLISH).endsWith("sir")
        ) {
            val base = trimmed.removeSuffix(".")
            "$base, sir."
        } else {
            trimmed
        }

        withContext(Dispatchers.IO) {
            repository.saveMessage(ChatMessage(role = MessageRole.JARVIS, content = formatted))
        }

        onStateChange(JarvisState.SPEAKING, null)
        onSpokenResponse(formatted)
    }
}
