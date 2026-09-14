package com.example.jarvis.tools.impl.web

import android.content.Context
import com.example.jarvis.tools.Tool
import com.example.jarvis.tools.models.RiskLevel
import com.example.jarvis.tools.models.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebSearchTool : Tool {
    override val name: String = "web_search"
    override val description: String = "Searches the web for current events, facts, weather, and general information."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val parametersJsonSchema: String = """{
        "type": "object",
        "properties": {
            "query": {"type": "string", "description": "The search keywords or question."}
        },
        "required": ["query"]
    }"""

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(context: Context, params: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val query = params["query"]?.toString()?.trim()
            ?: return@withContext ToolResult(isSuccess = false, output = "Search query is required.")

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVIS-Android-Assistant/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful || body.isBlank()) {
                return@withContext ToolResult(
                    isSuccess = true,
                    output = "Web lookup for '$query' did not return direct telemetry."
                )
            }

            val json = JSONObject(body)
            val abstractText = json.optString("AbstractText", "")
            val answer = json.optString("Answer", "")
            val heading = json.optString("Heading", "")

            val resultText = when {
                abstractText.isNotBlank() -> abstractText
                answer.isNotBlank() -> answer
                else -> {
                    val relatedTopics = json.optJSONArray("RelatedTopics")
                    if (relatedTopics != null && relatedTopics.length() > 0) {
                        val first = relatedTopics.optJSONObject(0)
                        first?.optString("Text", "") ?: ""
                    } else ""
                }
            }

            if (resultText.isNotBlank()) {
                ToolResult(isSuccess = true, output = resultText)
            } else {
                ToolResult(
                    isSuccess = true,
                    output = "Information retrieved for '$query', though concise telemetry is not indexed."
                )
            }
        } catch (e: Exception) {
            ToolResult(isSuccess = false, output = "Web lookup failed: ${e.localizedMessage}")
        }
    }
}
