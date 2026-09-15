package com.example.jarvis.memory

import com.example.jarvis.data.model.MemoryCategory
import com.example.jarvis.data.model.MemoryItem
import java.util.Locale

object MemoryExtractor {

    private val REMEMBER_PATTERNS = listOf(
        Regex("""(?i)remember\s+that\s+my\s+([a-zA-Z0-9_\s]+?)\s+(?:is\s+called|is\s+named|is|called|named)\s+(.+)"""),
        Regex("""(?i)remember\s+that\s+([a-zA-Z0-9_\s]+?)\s+(?:is\s+called|is\s+named|is|called|named)\s+(.+)"""),
        Regex("""(?i)remember\s+([a-zA-Z0-9_\s]+?)\s+(?:is\s+called|is\s+named|is|called|named)\s+(.+)"""),
        Regex("""(?i)remember\s+that\s+(.+)"""),
        Regex("""(?i)remember\s+(.+)""")
    )

    fun extractMemory(text: String): MemoryItem? {
        val trimmed = text.trim().removeSuffix(".")
        for (pattern in REMEMBER_PATTERNS) {
            val match = pattern.matchEntire(trimmed) ?: continue
            val groups = match.groupValues

            if (groups.size >= 3) {
                val subject = groups[1].trim().lowercase(Locale.getDefault())
                val value = groups[2].trim()

                val category = when {
                    subject.contains("project") -> MemoryCategory.PROJECT
                    subject.contains("wife") || subject.contains("husband") ||
                            subject.contains("friend") || subject.contains("boss") ||
                            subject.contains("colleague") || subject.contains("mother") ||
                            subject.contains("father") || subject.contains("brother") ||
                            subject.contains("sister") || subject.contains("partner") -> MemoryCategory.PERSON
                    subject.contains("device") || subject.contains("phone") ||
                            subject.contains("laptop") || subject.contains("car") ||
                            subject.contains("tv") || subject.contains("server") -> MemoryCategory.DEVICE
                    subject.contains("task") || subject.contains("todo") -> MemoryCategory.TASK
                    subject.contains("name") || subject.contains("like") ||
                            subject.contains("preference") || subject.contains("coffee") ||
                            subject.contains("drink") || subject.contains("color") -> MemoryCategory.PREFERENCE
                    else -> MemoryCategory.FACT
                }

                val key = subject.replace(" ", "_")
                return MemoryItem(category = category, key = key, value = value)
            } else if (groups.size == 2) {
                val fact = groups[1].trim()
                return MemoryItem(category = MemoryCategory.FACT, key = "fact_${System.currentTimeMillis() % 10000}", value = fact)
            }
        }
        return null
    }
}
