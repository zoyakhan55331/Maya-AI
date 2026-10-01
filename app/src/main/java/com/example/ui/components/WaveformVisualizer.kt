package com.example.ui.components

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    voiceState: VoiceState,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform_motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 28
    val isActive = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("waveform_visualizer")
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(4f, 14f)
        val gap = barWidth * 0.6f
        val startX = (totalWidth - (barCount * (barWidth + gap))) / 2f

        for (i in 0 until barCount) {
            val normalizedIdx = (i.toFloat() / barCount)
            val bellCurve = sin(normalizedIdx * Math.PI).toFloat()

            val animatedFactor = if (isActive) {
                val waveVal = sin(phase + (i * 0.45f)).toFloat()
                val dynamicHeight = (abs(waveVal) * 0.5f + 0.5f) * (audioLevel * 1.2f + 0.25f)
                dynamicHeight * bellCurve
            } else if (voiceState == VoiceState.THINKING) {
                val waveVal = sin(phase * 1.5f + (i * 0.3f)).toFloat()
                (abs(waveVal) * 0.35f + 0.15f) * bellCurve
            } else {
                0.12f * bellCurve
            }

            val barHeight = (canvasHeight * 0.85f * animatedFactor).coerceIn(6f, canvasHeight * 0.95f)
            val x = startX + i * (barWidth + gap)
            val top = centerY - (barHeight / 2f)

            val gradientBrush = Brush.verticalGradient(
                colors = when (voiceState) {
                    VoiceState.SPEAKING -> listOf(NeonPink, ElectricViolet)
                    VoiceState.LISTENING -> listOf(NeonCyan, NeonPink)
                    VoiceState.THINKING -> listOf(ElectricViolet, NeonCyan)
                    VoiceState.CONNECTING -> listOf(NeonPink, Color.White)
                    VoiceState.DISCONNECTED -> listOf(Color(0xFF475569), Color(0xFF1E293B))
                },
                startY = top,
                endY = top + barHeight
            )

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
