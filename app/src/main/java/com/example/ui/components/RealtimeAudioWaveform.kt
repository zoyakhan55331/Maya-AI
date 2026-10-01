package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoiceState
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class WaveformStyle(val label: String) {
    LIQUID("Fluid Ribbon"),
    SPECTRUM("Cyber Spectrum"),
    HOLOGRAPHIC("Holo Waves")
}

@Composable
fun RealtimeAudioWaveform(
    voiceState: VoiceState,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    var currentStyle by remember { mutableStateOf(WaveformStyle.HOLOGRAPHIC) }

    // Smooth physics dampened audio level
    val smoothedIntensity by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "smoothed_audio_intensity"
    )

    // Continuous wave phase motion
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val breathingFloat by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val effectiveIntensity = if (voiceState == VoiceState.DISCONNECTED) {
        breathingFloat
    } else if (voiceState == VoiceState.THINKING) {
        0.22f + (breathingFloat * 0.5f)
    } else {
        (smoothedIntensity * 0.9f + breathingFloat * 0.4f).coerceIn(0.12f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_audio_waveform_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Custom Canvas Waveform Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        // Cycle styles on tap
                        currentStyle = when (currentStyle) {
                            WaveformStyle.LIQUID -> WaveformStyle.SPECTRUM
                            WaveformStyle.SPECTRUM -> WaveformStyle.HOLOGRAPHIC
                            WaveformStyle.HOLOGRAPHIC -> WaveformStyle.LIQUID
                        }
                    }
                )
                .testTag("realtime_waveform_canvas_box")
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("realtime_audio_canvas")
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                when (currentStyle) {
                    WaveformStyle.LIQUID -> {
                        drawLiquidWaveform(
                            width = width,
                            height = height,
                            centerY = centerY,
                            intensity = effectiveIntensity,
                            phase = phase,
                            voiceState = voiceState
                        )
                    }
                    WaveformStyle.SPECTRUM -> {
                        drawCyberSpectrum(
                            width = width,
                            height = height,
                            centerY = centerY,
                            intensity = effectiveIntensity,
                            phase = phase,
                            voiceState = voiceState
                        )
                    }
                    WaveformStyle.HOLOGRAPHIC -> {
                        // Multi-layered Holo Waves with dynamic crest particles
                        drawHolographicWaveform(
                            width = width,
                            height = height,
                            centerY = centerY,
                            intensity = effectiveIntensity,
                            phase = phase,
                            voiceState = voiceState
                        )
                    }
                }
            }
        }

        // Style Indicator Tag
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Waveform: ${currentStyle.label} (tap to cycle)",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Draws the Holographic Multi-Wave Ribbon with primary, secondary, and tertiary harmonic paths.
 */
private fun DrawScope.drawHolographicWaveform(
    width: Float,
    height: Float,
    centerY: Float,
    intensity: Float,
    phase: Float,
    voiceState: VoiceState
) {
    val maxAmplitude = (height * 0.45f) * intensity
    val samplePoints = 64
    val step = width / samplePoints

    // 1. Subtle horizontal glowing baseline
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                NeonCyan.copy(alpha = 0.35f * intensity),
                NeonPink.copy(alpha = 0.45f * intensity),
                Color.Transparent
            )
        ),
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 1.2.dp.toPx()
    )

    // 2. Tertiary Ambient Translucent Fill (Depth Layer)
    val tertiaryPath = Path().apply {
        moveTo(0f, centerY)
        for (i in 0..samplePoints) {
            val x = i * step
            val normX = (i.toFloat() / samplePoints)
            val envelope = sin(normX * PI).toFloat()
            val waveVal = sin(phase * 0.8f + (normX * 3.5f * PI).toFloat())
            val y = centerY + (waveVal * maxAmplitude * 0.5f * envelope)
            lineTo(x, y)
        }
        lineTo(width, centerY)
        close()
    }
    drawPath(
        path = tertiaryPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                ElectricViolet.copy(alpha = 0.18f * intensity),
                Color.Transparent
            ),
            startY = centerY - maxAmplitude * 0.6f,
            endY = centerY + maxAmplitude * 0.6f
        ),
        style = Fill
    )

    // 3. Secondary Harmonic Wave (Cyan & Violet)
    val secondaryPath = Path().apply {
        moveTo(0f, centerY)
        for (i in 0..samplePoints) {
            val x = i * step
            val normX = (i.toFloat() / samplePoints)
            val envelope = sin(normX * PI).toFloat()
            val waveVal = sin(phase * 1.5f + (normX * 5.0f * PI).toFloat() + 1.2f)
            val y = centerY + (waveVal * maxAmplitude * 0.72f * envelope)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(
        path = secondaryPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                NeonCyan.copy(alpha = 0.5f),
                ElectricCyan,
                NeonPurple.copy(alpha = 0.7f),
                NeonCyan.copy(alpha = 0.5f)
            )
        ),
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 4. Primary Resonant Voice Wave (Neon Pink & Magenta)
    val primaryPath = Path().apply {
        moveTo(0f, centerY)
        for (i in 0..samplePoints) {
            val x = i * step
            val normX = (i.toFloat() / samplePoints)
            val envelope = sin(normX * PI).toFloat()
            val waveVal = sin(phase * 2f + (normX * 4.2f * PI).toFloat())
            val y = centerY + (waveVal * maxAmplitude * envelope)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    val primaryBrush = Brush.horizontalGradient(
        colors = when (voiceState) {
            VoiceState.SPEAKING -> listOf(NeonPink, NeonMagenta, ElectricViolet, NeonPink)
            VoiceState.LISTENING -> listOf(NeonCyan, NeonPink, ElectricViolet, NeonCyan)
            VoiceState.THINKING -> listOf(ElectricViolet, NeonAmber, NeonCyan)
            else -> listOf(Color(0xFF64748B), NeonPink.copy(alpha = 0.6f), Color(0xFF64748B))
        }
    )

    drawPath(
        path = primaryPath,
        brush = primaryBrush,
        style = Stroke(
            width = 3.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 5. Dynamic Crest Energy Particles (ride along peaks)
    val particleCount = 6
    for (p in 0 until particleCount) {
        val norm = ((p.toFloat() + 0.5f) / particleCount)
        val px = norm * width
        val envelope = sin(norm * PI).toFloat()
        val waveVal = sin(phase * 2f + (norm * 4.2f * PI).toFloat())
        val py = centerY + (waveVal * maxAmplitude * envelope)

        val nodeRadius = (2.5f + (intensity * 2.5f)).dp.toPx()
        drawCircle(
            color = Color.White,
            radius = nodeRadius * 0.65f,
            center = Offset(px, py)
        )
        drawCircle(
            color = NeonCyan.copy(alpha = 0.65f * intensity),
            radius = nodeRadius * 1.5f,
            center = Offset(px, py),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/**
 * Draws the Liquid Waveform style with smooth flowing sinusoidal ribbons.
 */
private fun DrawScope.drawLiquidWaveform(
    width: Float,
    height: Float,
    centerY: Float,
    intensity: Float,
    phase: Float,
    voiceState: VoiceState
) {
    val maxAmplitude = (height * 0.42f) * intensity
    val points = 48
    val dx = width / points

    // Gradient filled area
    val ribbonPath = Path().apply {
        moveTo(0f, centerY)
        for (i in 0..points) {
            val x = i * dx
            val norm = i.toFloat() / points
            val envelope = sin(norm * PI).toFloat()
            val y = centerY + (sin(phase + norm * 3 * PI).toFloat() * maxAmplitude * envelope)
            lineTo(x, y)
        }
        for (i in points downTo 0) {
            val x = i * dx
            val norm = i.toFloat() / points
            val envelope = sin(norm * PI).toFloat()
            val y = centerY - (cos(phase * 1.2f + norm * 3.5 * PI).toFloat() * maxAmplitude * 0.7f * envelope)
            lineTo(x, y)
        }
        close()
    }

    drawPath(
        path = ribbonPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                NeonPink.copy(alpha = 0.25f * intensity),
                ElectricViolet.copy(alpha = 0.45f * intensity),
                NeonCyan.copy(alpha = 0.35f * intensity)
            )
        ),
        style = Fill
    )

    // Outline stroke
    drawPath(
        path = ribbonPath,
        brush = Brush.horizontalGradient(
            colors = listOf(NeonPink, ElectricViolet, NeonCyan, NeonPink)
        ),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Draws the Cyber Spectrum style with digital audio frequency bars.
 */
private fun DrawScope.drawCyberSpectrum(
    width: Float,
    height: Float,
    centerY: Float,
    intensity: Float,
    phase: Float,
    voiceState: VoiceState
) {
    val barCount = 32
    val barWidth = (width / (barCount * 1.5f)).coerceIn(4f, 12f)
    val gap = barWidth * 0.5f
    val startX = (width - (barCount * (barWidth + gap))) / 2f

    for (i in 0 until barCount) {
        val norm = i.toFloat() / barCount
        val bell = sin(norm * PI).toFloat()
        val sineVar = abs(sin(phase * 1.8f + (i * 0.38f))).toFloat()
        val barHeight = ((height * 0.88f) * (sineVar * 0.65f + 0.35f) * intensity * bell).coerceAtLeast(4f)
        val x = startX + i * (barWidth + gap)
        val top = centerY - (barHeight / 2f)

        val brush = Brush.verticalGradient(
            colors = listOf(NeonPink, NeonCyan),
            startY = top,
            endY = top + barHeight
        )

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}
