package com.example.jarvis.ai.impl

import com.example.jarvis.ai.AIProvider
import com.example.jarvis.ai.models.AIResponse
import com.example.jarvis.ai.models.ToolCall
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.data.model.MessageRole
import com.example.jarvis.security.SecureStorage
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIProvider(
    private val secureStorage: SecureStorage,
    private val model: String = "gemini-1.5-flash"
) : AIProvider {
    override val name: String = "Google Gemini"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun generateResponse(
        messages: List<ChatMessage>,
        memories: List<MemoryItem>,
        availableTools: List<Tool>
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = secureStorage.geminiApiKey
        if (apiKey.isBlank()) {
            return@withContext AIResponse(
                "Gemini API key is not configured. Please enter your API key in Settings, sir."
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val systemInstruction = buildSystemPrompt(memories)

            val payload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })

                val contentsArray = JSONArray()
                for (msg in messages.takeLast(10)) {
                    val role = if (msg.role == MessageRole.USER) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                    })
                }
                put("contents", contentsArray)

                // Tool declarations
                if (availableTools.isNotEmpty()) {
                    val functionDeclarations = JSONArray()
                    for (tool in availableTools) {
                        functionDeclarations.put(JSONObject().apply {
                            put("name", tool.name)
                            put("description", tool.description)
                            try {
                                put("parameters", JSONObject(tool.parametersJsonSchema))
                            } catch (e: Exception) {
                                put("parameters", JSONObject().put("type", "object"))
                            }
                        })
                    }
                    put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations)))
                }
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AIResponse(
                    "Unable to reach the Gemini cognitive core right now, sir."
                )
            }

            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext AIResponse("I received no response from the network, sir.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")

            var responseText = ""
            val toolCalls = mutableListOf<ToolCall>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        responseText += part.getString("text")
                    }
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.getString("name")
                        val fnArgs = fn.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (fnArgs != null) {
                            val keys = fnArgs.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = fnArgs.get(k)
                            }
                        }
                        toolCalls.add(ToolCall(fnName, argsMap))
                    }
                }
            }

            AIResponse(content = responseText.trim(), toolCalls = toolCalls)
        } catch (e: Exception) {
            AIResponse("Cognitive network failure: ${e.localizedMessage}, sir.")
        }
    }

    private fun buildSystemPrompt(memories: List<MemoryItem>): String {
        val memoryContext = if (memories.isNotEmpty()) {
            "Known facts and user context:\n" + memories.joinToString("\n") { "- [${it.category}]: ${it.key} = ${it.value}" }
        } else ""

        return """
            You are JARVIS, an elite, highly intelligent, calm, and professional voice-first AI assistant for Android inspired by Iron Man's JARVIS.
            Guidelines:
            1. Keep responses concise, direct, helpful, and natural.
            2. Never be verbose or recite robotic boilerplate.
            3. When executing a tool, call the corresponding function.
            4. Naturally conclude spoken statements with ', sir.'
            5. Never use 'sir' as an excuse for lack of useful insight.
            $memoryContext
        """.trimIndent()
    }
}
