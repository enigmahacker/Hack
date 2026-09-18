package com.example.jarvis.data

import android.content.Context
import com.example.jarvis.data.local.JarvisDbHelper
import com.example.jarvis.data.model.ChatMessage
import com.example.jarvis.data.model.JarvisTask
import com.example.jarvis.data.model.MemoryItem
import com.example.jarvis.data.preferences.JarvisPreferences
import com.example.jarvis.security.SecureStorage

class JarvisRepository(context: Context) : TaskRepository {
    val dbHelper = JarvisDbHelper(context)
    val preferences = JarvisPreferences(context)
    val secureStorage = SecureStorage(context)

    suspend fun saveMemory(item: MemoryItem): Long = dbHelper.insertMemory(item)
    suspend fun getMemories(): List<MemoryItem> = dbHelper.getAllMemories()
    suspend fun deleteMemory(id: Long): Int = dbHelper.deleteMemory(id)
    suspend fun clearMemories(): Int = dbHelper.clearAllMemories()

    suspend fun saveMessage(msg: ChatMessage): Long = dbHelper.insertMessage(msg)
    suspend fun getMessages(limit: Int = 50): List<ChatMessage> = dbHelper.getRecentMessages(limit)
    suspend fun clearMessages(): Int = dbHelper.clearMessages()

    override suspend fun addTask(task: JarvisTask): Long = dbHelper.insertTask(task)
    override suspend fun getTasks(): List<JarvisTask> = dbHelper.getAllTasks()
    override suspend fun completeTask(query: String): Boolean = dbHelper.completeTask(query)
    override suspend fun deleteTask(id: Long): Int = dbHelper.deleteTask(id)
}
