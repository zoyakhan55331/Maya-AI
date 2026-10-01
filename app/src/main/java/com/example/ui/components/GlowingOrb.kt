package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingOrb(
    voiceState: VoiceState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animations")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_rotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Dynamic color sets based on voice state
    val (primaryGlow, secondaryGlow, accentGlow) = when (voiceState) {
        VoiceState.DISCONNECTED -> Triple(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
        VoiceState.CONNECTING -> Triple(NeonAmber, NeonPink, ElectricViolet)
        VoiceState.LISTENING -> Triple(NeonCyan, ElectricCyan, NeonMagenta)
        VoiceState.THINKING -> Triple(NeonPurple, NeonPink, NeonCyan)
        VoiceState.SPEAKING -> Triple(NeonPink, NeonMagenta, ElectricViolet)
    }

    val dynamicAudioBoost = (audioLevel * 0.35f).coerceIn(0f, 0.45f)
    val effectiveScale = when (voiceState) {
        VoiceState.SPEAKING -> 1f + dynamicAudioBoost
        VoiceState.LISTENING -> pulseScale + (dynamicAudioBoost * 0.7f)
        VoiceState.THINKING -> pulseScale * 1.02f
        VoiceState.CONNECTING -> pulseScale
        VoiceState.DISCONNECTED -> 0.96f
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = primaryGlow),
                onClick = onClick
            )
            .testTag("central_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.65f * effectiveScale

            // Outer Aura Radial Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (voiceState == VoiceState.DISCONNECTED) 0.15f else 0.45f),
                        secondaryGlow.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )

            // Dynamic ripples when active (listening or speaking)
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                for (i in 1..3) {
                    val rippleRadius = baseRadius + (i * 18.dp.toPx() * (audioLevel + 0.3f))
                    val rippleAlpha = ((0.5f - (i * 0.12f)) * (audioLevel + 0.4f)).coerceIn(0f, 0.6f)
                    drawCircle(
                        color = if (i % 2 == 0) primaryGlow.copy(alpha = rippleAlpha) else secondaryGlow.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = (2.5f - (i * 0.5f)).dp.toPx())
                    )
                }
            }

            // Rotating Electric Orbit Ring
            val activeRotation = if (voiceState == VoiceState.THINKING) fastRotation else rotationAngle
            rotate(activeRotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryGlow,
                            secondaryGlow,
                            accentGlow,
                            Color.Transparent,
                            primaryGlow
                        ),
                        center = center
                    ),
                    radius = baseRadius * 1.08f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Counter Rotating Dashed Ring for sci-fi look
            rotate(-activeRotation * 0.7f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accentGlow.copy(alpha = 0.8f),
                            Color.Transparent,
                            primaryGlow.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = baseRadius * 0.95f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Core Orb Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (voiceState == VoiceState.DISCONNECTED) 0.5f else 0.95f),
                        primaryGlow,
                        secondaryGlow,
                        accentGlow
                    ),
                    center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                    radius = baseRadius
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // Inner Core Glow Reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f),
                    radius = baseRadius * 0.35f
                ),
                radius = baseRadius * 0.35f,
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f)
            )

            // Orbiting holographic sparks when active
            if (voiceState != VoiceState.DISCONNECTED) {
                val sparkCount = 6
                for (s in 0 until sparkCount) {
                    val angle = (wavePhase + (s * (2 * Math.PI / sparkCount))).toFloat()
                    val sparkDist = baseRadius * (1.15f + (0.12f * sin(angle * 2)))
                    val sparkX = center.x + sparkDist * cos(angle)
                    val sparkY = center.y + sparkDist * sin(angle)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(sparkX, sparkY)
                    )
                }
            }
        }
    }
}
