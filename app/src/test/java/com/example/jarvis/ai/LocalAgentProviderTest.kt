package com.example.jarvis.ai

import com.example.jarvis.ai.impl.LocalAgentProvider
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.MessageRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAgentProviderTest {

    private val provider = LocalAgentProvider()

    @Test
    fun testBatteryIntentResolution() = runBlocking {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "What's my battery level?"))
        val response = provider.generateResponse(messages, emptyList(), emptyList())
        assertEquals(1, response.toolCalls.size)
        assertEquals("get_battery_status", response.toolCalls[0].name)
    }

    @Test
    fun testOpenAppIntentResolution() = runBlocking {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "Open YouTube"))
        val response = provider.generateResponse(messages, emptyList(), emptyList())
        assertEquals(1, response.toolCalls.size)
        assertEquals("open_app", response.toolCalls[0].name)
        assertEquals("youtube", response.toolCalls[0].arguments["app_name"])
    }

    @Test
    fun testTimeIntentResolution() = runBlocking {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "What time is it?"))
        val response = provider.generateResponse(messages, emptyList(), emptyList())
        assertEquals(1, response.toolCalls.size)
        assertEquals("get_current_time", response.toolCalls[0].name)
    }

    @Test
    fun testTimerIntentResolution() = runBlocking {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "Set a timer for 5 minutes"))
        val response = provider.generateResponse(messages, emptyList(), emptyList())
        assertEquals(1, response.toolCalls.size)
        assertEquals("set_timer", response.toolCalls[0].name)
        assertEquals(300, response.toolCalls[0].arguments["duration_seconds"])
    }

    @Test
    fun testJarvisIdentityResponse() = runBlocking {
        val messages = listOf(ChatMessage(role = MessageRole.USER, content = "Who are you?"))
        val response = provider.generateResponse(messages, emptyList(), emptyList())
        assertTrue(response.content.contains("JARVIS"))
        assertTrue(response.content.endsWith("sir."))
    }
}
