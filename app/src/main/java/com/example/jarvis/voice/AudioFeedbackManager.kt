package com.example.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioFeedbackManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playWakeChime() {
        triggerHaptic(50)
        scope.launch {
            // Ascending dual harmonic chime (880Hz to 1320Hz)
            generateAndPlayTone(frequencies = doubleArrayOf(880.0, 1320.0), durationMs = 120)
        }
    }

    fun playSuccessChime() {
        triggerHaptic(30)
        scope.launch {
            generateAndPlayTone(frequencies = doubleArrayOf(1046.5, 1568.0), durationMs = 100)
        }
    }

    fun playErrorChime() {
        triggerHaptic(120)
        scope.launch {
            generateAndPlayTone(frequencies = doubleArrayOf(440.0, 330.0), durationMs = 180)
        }
    }

    fun triggerHaptic(durationMs: Long = 40) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Haptics optional if permission or hardware absent
        }
    }

    private fun generateAndPlayTone(frequencies: DoubleArray, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Envelope: ramp up 10ms, fade out last 30ms
            val progress = i.toDouble() / numSamples
            val envelope = when {
                progress < 0.1 -> progress / 0.1
                progress > 0.7 -> (1.0 - progress) / 0.3
                else -> 1.0
            }

            var sum = 0.0
            for (freq in frequencies) {
                sum += sin(2.0 * Math.PI * freq * t)
            }
            sum /= frequencies.size

            sample[i] = (sum * envelope * 0.4 * Short.MAX_VALUE).toInt().toShort()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sample, 0, numSamples)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 50)
            audioTrack.release()
        } catch (e: Exception) {
            // AudioTrack fallback
        }
    }
}
