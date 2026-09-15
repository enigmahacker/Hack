package com.example.jarvis.memory

import com.example.jarvis.data.model.MemoryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MemoryExtractorTest {

    @Test
    fun testExtractProjectMemory() {
        val memory = MemoryExtractor.extractMemory("Remember that my project is called VAMANA")
        assertNotNull(memory)
        assertEquals(MemoryCategory.PROJECT, memory?.category)
        assertEquals("VAMANA", memory?.value)
    }

    @Test
    fun testExtractPersonMemory() {
        val memory = MemoryExtractor.extractMemory("Remember that my wife is Pepper")
        assertNotNull(memory)
        assertEquals(MemoryCategory.PERSON, memory?.category)
        assertEquals("Pepper", memory?.value)
    }

    @Test
    fun testExtractPreferenceMemory() {
        val memory = MemoryExtractor.extractMemory("Remember that my coffee preference is espresso")
        assertNotNull(memory)
        assertEquals(MemoryCategory.PREFERENCE, memory?.category)
        assertEquals("espresso", memory?.value)
    }

    @Test
    fun testExtractGenericFact() {
        val memory = MemoryExtractor.extractMemory("Remember that we need to buy milk")
        assertNotNull(memory)
        assertEquals(MemoryCategory.FACT, memory?.category)
        assertEquals("we need to buy milk", memory?.value)
    }
}
