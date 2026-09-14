package com.example.jarvis.data.model

data class JarvisTask(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueTimestamp: Long? = null,
    val isCompleted: Boolean = false,
    val priority: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
