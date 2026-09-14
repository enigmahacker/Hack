package com.example.jarvis.ui.main.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.agent.JarvisState
import com.example.jarvis.theme.JarvisAmber
import com.example.jarvis.theme.JarvisBlue
import com.example.jarvis.theme.JarvisCrimson
import com.example.jarvis.theme.JarvisCyan
import com.example.jarvis.theme.JarvisCyanGlow
import com.example.jarvis.theme.JarvisViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorOrb(
    state: JarvisState,
    rmsLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransition")

    // Continuous rotation for outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.THINKING -> 2000
                    JarvisState.EXECUTING -> 1500
                    JarvisState.LISTENING -> 4000
                    else -> 12000
                },
                easing = LinearEasing
            )
        ),
        label = "OuterRotation"
    )

    // Reverse rotation for inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.THINKING -> 3000
                    JarvisState.EXECUTING -> 2000
                    else -> 16000
                },
                easing = LinearEasing
            )
        ),
        label = "InnerRotation"
    )

    // Breathing pulse for IDLE and SPEAKING
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == JarvisState.SPEAKING) 500 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val primaryGlowColor = when (state) {
        JarvisState.ERROR -> JarvisCrimson
        JarvisState.THINKING -> JarvisViolet
        JarvisState.EXECUTING -> JarvisBlue
        JarvisState.LISTENING -> JarvisCyan
        JarvisState.SPEAKING -> JarvisCyan
        JarvisState.IDLE -> JarvisCyan
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val maxRadius = size.toPx() / 2f

            // Dynamic microphone audio reactivity for LISTENING state
            val audioScale = if (state == JarvisState.LISTENING) {
                1f + (rmsLevel.coerceIn(0f, 10f) / 25f)
            } else {
                pulseScale
            }

            // 1. Outermost Ambient Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlowColor.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = maxRadius * audioScale
                ),
                radius = maxRadius * audioScale,
                center = center
            )

            // 2. Concentric Acoustic Waves for LISTENING state
            if (state == JarvisState.LISTENING) {
                drawListeningWaves(center, maxRadius, rmsLevel, primaryGlowColor)
            }

            // 3. Segmented Outer Mechanical Ring
            drawOuterSegmentedRing(center, maxRadius * 0.88f, outerRotation, primaryGlowColor, state)

            // 4. Tick Marks Circle
            drawTicks(center, maxRadius * 0.74f, primaryGlowColor.copy(alpha = 0.5f))

            // 5. Counter-rotating Middle Energy Ring
            drawMiddleEnergyRing(center, maxRadius * 0.62f, innerRotation, primaryGlowColor, state)

            // 6. Inner Glowing Arc Core
            drawArcCore(center, maxRadius * 0.44f * audioScale, primaryGlowColor, state)
        }
    }
}

private fun DrawScope.drawListeningWaves(
    center: Offset,
    maxRadius: Float,
    rmsLevel: Float,
    color: Color
) {
    val waveIntensity = (rmsLevel.coerceIn(0f, 10f) / 10f)
    for (i in 1..3) {
        val r = maxRadius * (0.85f + i * 0.05f * waveIntensity)
        drawCircle(
            color = color.copy(alpha = 0.2f / i),
            radius = r,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

private fun DrawScope.drawOuterSegmentedRing(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    color: Color,
    state: JarvisState
) {
    val segments = 8
    val sweepAngle = 28f
    val gap = 360f / segments

    for (i in 0 until segments) {
        val startAngle = (i * gap) + rotationDeg
        drawArc(
            color = color.copy(alpha = 0.75f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawTicks(center: Offset, radius: Float, color: Color) {
    val numTicks = 24
    for (i in 0 until numTicks) {
        val angleRad = Math.toRadians((i * (360f / numTicks)).toDouble())
        val startR = radius - 3.dp.toPx()
        val endR = radius + 3.dp.toPx()
        val start = Offset((center.x + startR * cos(angleRad)).toFloat(), (center.y + startR * sin(angleRad)).toFloat())
        val end = Offset((center.x + endR * cos(angleRad)).toFloat(), (center.y + endR * sin(angleRad)).toFloat())
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawMiddleEnergyRing(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    color: Color,
    state: JarvisState
) {
    val segments = 3
    val sweepAngle = 80f
    val gap = 360f / segments

    for (i in 0 until segments) {
        val startAngle = (i * gap) + rotationDeg
        drawArc(
            color = color.copy(alpha = 0.9f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawArcCore(
    center: Offset,
    radius: Float,
    color: Color,
    state: JarvisState
) {
    // Solid Core Glow Gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                color,
                color.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Inner Ring
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = radius * 0.4f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}
