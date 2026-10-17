package com.setons.trackrep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.setons.trackrep.theme.DarkPrimaryGold

enum class VoiceAssistantPhase {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * Google Gemini-style screen-perimeter edge glow.
 * Wraps around the entire screen edges with a luminous flowing gold gradient beam
 * and ambient atmospheric bloom, dynamically responsive to voice state.
 */
@Composable
fun GeminiEdgeGlow(
    isActive: Boolean,
    phase: VoiceAssistantPhase,
    primaryColor: Color = DarkPrimaryGold,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GeminiEdgeGlow")

    // Continuous chromatic sweep phase along the screen perimeter
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    VoiceAssistantPhase.THINKING -> 1800
                    VoiceAssistantPhase.LISTENING -> 3200
                    VoiceAssistantPhase.SPEAKING -> 2400
                    else -> 3600
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    // Breathing pulse for organic assistant presence
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(350)),
        exit = fadeOut(tween(450)),
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 14.dp.toPx()
            val coreWidthPx = 5.dp.toPx()
            val cornerRadiusPx = 32.dp.toPx()

            val w = size.width
            val h = size.height

            // Calculate gradient colors around gold/primary
            val goldBright = Color(0xFFFFF099)
            val goldPure = primaryColor
            val goldDeep = Color(0xFFE6A100)
            val champagne = Color(0xFFFFE082)
            val amberAccent = Color(0xFFFFB300)

            // Dynamic color shift depending on state
            val baseColors = when (phase) {
                VoiceAssistantPhase.THINKING -> listOf(
                    goldBright,
                    Color(0xFF80D8FF), // Subtle cyan spark in thinking
                    goldPure,
                    champagne,
                    amberAccent,
                    goldBright
                )
                VoiceAssistantPhase.SPEAKING -> listOf(
                    goldBright,
                    goldPure,
                    champagne,
                    goldDeep,
                    goldBright
                )
                else -> listOf(
                    goldBright,
                    goldPure,
                    amberAccent,
                    champagne,
                    goldDeep,
                    goldBright
                )
            }

            // Rotating sweep gradient brush
            val sweepBrush = Brush.sweepGradient(
                colors = baseColors,
                center = Offset(w / 2f, h / 2f)
            )

            val glowBrush = Brush.sweepGradient(
                colors = baseColors.map { it.copy(alpha = 0.35f * pulseAlpha) },
                center = Offset(w / 2f, h / 2f)
            )

            val outerBounds = RoundRect(
                left = strokeWidthPx / 2f,
                top = strokeWidthPx / 2f,
                right = w - strokeWidthPx / 2f,
                bottom = h - strokeWidthPx / 2f,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            val outerPath = Path().apply {
                addRoundRect(outerBounds)
            }

            // Layer 1: Ambient outer bloom
            drawPath(
                path = outerPath,
                brush = glowBrush,
                style = Stroke(
                    width = strokeWidthPx * 1.8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Layer 2: Main radiant gradient border
            drawPath(
                path = outerPath,
                brush = sweepBrush,
                alpha = 0.85f * pulseAlpha,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Layer 3: High-intensity inner luminous core beam
            drawPath(
                path = outerPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        goldBright.copy(alpha = 0.95f),
                        champagne.copy(alpha = 0.85f),
                        goldBright.copy(alpha = 0.95f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                alpha = pulseAlpha,
                style = Stroke(
                    width = coreWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
