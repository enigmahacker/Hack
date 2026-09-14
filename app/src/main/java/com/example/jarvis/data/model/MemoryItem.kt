package com.example.jarvis.data.model

enum class MemoryCategory {
    PREFERENCE,
    PERSON,
    PROJECT,
    TASK,
    FACT,
    DEVICE
}

data class MemoryItem(
    val id: Long = 0,
    val category: MemoryCategory,
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
