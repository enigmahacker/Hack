package com.example.jarvis.data

import com.example.jarvis.data.model.JarvisTask

interface TaskRepository {
    suspend fun addTask(task: JarvisTask): Long
    suspend fun getTasks(): List<JarvisTask>
    suspend fun completeTask(query: String): Boolean
    suspend fun deleteTask(id: Long): Int
}
