package com.example.jarvis.ui.main.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.theme.JarvisBlue
import com.example.jarvis.theme.JarvisCyan
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    rmsLevel: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val width = size.width
        val centerY = size.height / 2f
        val numBars = 32
        val barSpacing = width / numBars
        val baseAmp = if (isSpeaking) 16.dp.toPx() else (rmsLevel.coerceIn(0f, 10f) * 2.5.dp.toPx())

        for (i in 0 until numBars) {
            val x = i * barSpacing + barSpacing / 2f
            val normX = (i.toFloat() / numBars) * (2 * Math.PI).toFloat()
            val waveHeight = (sin(normX + phase) * baseAmp).coerceAtLeast(3.dp.toPx())

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(JarvisCyan, JarvisBlue),
                    startY = centerY - waveHeight,
                    endY = centerY + waveHeight
                ),
                start = Offset(x, centerY - waveHeight),
                end = Offset(x, centerY + waveHeight),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
