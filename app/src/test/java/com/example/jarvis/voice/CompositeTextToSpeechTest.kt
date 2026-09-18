package com.example.jarvis.voice

import com.example.jarvis.voice.impl.CompositeTextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeTextToSpeechTest {

    private class FakeTtsEngine : TextToSpeechEngine {
        var speakCalls = mutableListOf<String>()
        var shouldFail = false
        var failureMessage = "Simulated error"
        override val isSpeaking: Boolean = false

        override fun speak(
            text: String,
            onStart: () -> Unit,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ) {
            speakCalls.add(text)
            if (shouldFail) {
                onError(failureMessage)
            } else {
                onStart()
                onDone()
            }
        }

        override fun stop() {}
        override fun setSpeed(speed: Float) {}
        override fun setPitch(pitch: Float) {}
        override fun shutdown() {}
    }

    @Test
    fun testDelegatesToFishAudioWhenEnabledAndKeyPresent() {
        val fishTts = FakeTtsEngine()
        val androidTts = FakeTtsEngine()

        val composite = CompositeTextToSpeech(
            fishAudioTts = fishTts,
            androidTts = androidTts,
            isFishAudioEnabled = { true },
            hasFishAudioKey = { true }
        )

        var started = false
        var completed = false
        composite.speak(
            text = "Testing voice, sir.",
            onStart = { started = true },
            onDone = { completed = true }
        )

        assertEquals(1, fishTts.speakCalls.size)
        assertEquals("Testing voice, sir.", fishTts.speakCalls[0])
        assertEquals(0, androidTts.speakCalls.size)
        assertTrue(started)
        assertTrue(completed)
    }

    @Test
    fun testDelegatesToAndroidTtsWhenFishAudioDisabled() {
        val fishTts = FakeTtsEngine()
        val androidTts = FakeTtsEngine()

        val composite = CompositeTextToSpeech(
            fishAudioTts = fishTts,
            androidTts = androidTts,
            isFishAudioEnabled = { false },
            hasFishAudioKey = { true }
        )

        composite.speak("Testing voice, sir.")

        assertEquals(0, fishTts.speakCalls.size)
        assertEquals(1, androidTts.speakCalls.size)
        assertEquals("Testing voice, sir.", androidTts.speakCalls[0])
    }

    @Test
    fun testDelegatesToAndroidTtsWhenKeyMissing() {
        val fishTts = FakeTtsEngine()
        val androidTts = FakeTtsEngine()

        val composite = CompositeTextToSpeech(
            fishAudioTts = fishTts,
            androidTts = androidTts,
            isFishAudioEnabled = { true },
            hasFishAudioKey = { false }
        )

        composite.speak("Testing voice, sir.")

        assertEquals(0, fishTts.speakCalls.size)
        assertEquals(1, androidTts.speakCalls.size)
    }

    @Test
    fun testFallsBackToAndroidTtsWhenFishAudioFails() {
        val fishTts = FakeTtsEngine().apply { shouldFail = true }
        val androidTts = FakeTtsEngine()

        val composite = CompositeTextToSpeech(
            fishAudioTts = fishTts,
            androidTts = androidTts,
            isFishAudioEnabled = { true },
            hasFishAudioKey = { true }
        )

        var started = false
        var completed = false
        composite.speak(
            text = "Battery level is 80%, sir.",
            onStart = { started = true },
            onDone = { completed = true }
        )

        assertEquals(1, fishTts.speakCalls.size)
        assertEquals(1, androidTts.speakCalls.size)
        assertEquals("Battery level is 80%, sir.", androidTts.speakCalls[0])
        assertTrue(started)
        assertTrue(completed)
    }
}
