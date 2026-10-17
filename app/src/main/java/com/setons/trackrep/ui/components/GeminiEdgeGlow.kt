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
import androidx.compose.foundation.layout.fillMaxSize
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
 * Soft, elegant, all-around rim glow that gracefully fades inward toward transparent.
 * Never bold or thick.
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
                    VoiceAssistantPhase.THINKING -> 2000
                    VoiceAssistantPhase.LISTENING -> 3400
                    VoiceAssistantPhase.SPEAKING -> 2600
                    else -> 3600
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    // Gentle breathing pulse for organic presence
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(500)),
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Inward fade depth: delicate 9dp fade inward
            val fadePx = 10.dp.toPx()
            val cornerRadiusPx = 32.dp.toPx()

            val goldBright = Color(0xFFFFF1A8)
            val goldPure = primaryColor
            val amberSoft = Color(0xFFFFB300)
            val champagne = Color(0xFFFFE082)

            val baseColors = when (phase) {
                VoiceAssistantPhase.THINKING -> listOf(
                    goldBright,
                    Color(0xFF80D8FF), // Subtle cyan spark during reasoning
                    goldPure,
                    champagne,
                    amberSoft,
                    goldBright
                )
                VoiceAssistantPhase.SPEAKING -> listOf(
                    goldBright,
                    goldPure,
                    champagne,
                    amberSoft,
                    goldBright
                )
                else -> listOf(
                    goldBright,
                    goldPure,
                    amberSoft,
                    champagne,
                    goldBright
                )
            }

            val rimAlpha = 0.42f * pulseAlpha

            // 1. Soft Inward Edge Fades on all 4 sides (Top, Bottom, Left, Right)
            // Top rim fade
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(goldPure.copy(alpha = rimAlpha), Color.Transparent),
                    startY = 0f,
                    endY = fadePx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, fadePx)
            )

            // Bottom rim fade
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, goldPure.copy(alpha = rimAlpha)),
                    startY = h - fadePx,
                    endY = h
                ),
                topLeft = Offset(0f, h - fadePx),
                size = Size(w, fadePx)
            )

            // Left rim fade
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(goldPure.copy(alpha = rimAlpha), Color.Transparent),
                    startX = 0f,
                    endX = fadePx
                ),
                topLeft = Offset(0f, 0f),
                size = Size(fadePx, h)
            )

            // Right rim fade
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, goldPure.copy(alpha = rimAlpha)),
                    startX = w - fadePx,
                    endX = w
                ),
                topLeft = Offset(w - fadePx, 0f),
                size = Size(fadePx, h)
            )

            // 2. Corner Radial Inward Fades (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
            val cornerGlowBrushTL = Brush.radialGradient(
                colors = listOf(goldBright.copy(alpha = rimAlpha * 1.1f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = fadePx * 2.2f
            )
            drawRect(brush = cornerGlowBrushTL, topLeft = Offset(0f, 0f), size = Size(fadePx * 2.2f, fadePx * 2.2f))

            val cornerGlowBrushTR = Brush.radialGradient(
                colors = listOf(goldBright.copy(alpha = rimAlpha * 1.1f), Color.Transparent),
                center = Offset(w, 0f),
                radius = fadePx * 2.2f
            )
            drawRect(brush = cornerGlowBrushTR, topLeft = Offset(w - fadePx * 2.2f, 0f), size = Size(fadePx * 2.2f, fadePx * 2.2f))

            val cornerGlowBrushBL = Brush.radialGradient(
                colors = listOf(goldBright.copy(alpha = rimAlpha * 1.1f), Color.Transparent),
                center = Offset(0f, h),
                radius = fadePx * 2.2f
            )
            drawRect(brush = cornerGlowBrushBL, topLeft = Offset(0f, h - fadePx * 2.2f), size = Size(fadePx * 2.2f, fadePx * 2.2f))

            val cornerGlowBrushBR = Brush.radialGradient(
                colors = listOf(goldBright.copy(alpha = rimAlpha * 1.1f), Color.Transparent),
                center = Offset(w, h),
                radius = fadePx * 2.2f
            )
            drawRect(brush = cornerGlowBrushBR, topLeft = Offset(w - fadePx * 2.2f, h - fadePx * 2.2f), size = Size(fadePx * 2.2f, fadePx * 2.2f))

            // 3. Delicate, Ultra-Thin 1.5dp Rim Sweep Beam hugging the phone bezel perimeter
            val thinStrokePx = 1.8.dp.toPx()
            val outerBounds = RoundRect(
                left = thinStrokePx / 2f,
                top = thinStrokePx / 2f,
                right = w - thinStrokePx / 2f,
                bottom = h - thinStrokePx / 2f,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            val sweepBrush = Brush.sweepGradient(
                colors = baseColors,
                center = Offset(w / 2f, h / 2f)
            )

            val outerPath = Path().apply {
                addRoundRect(outerBounds)
            }

            drawPath(
                path = outerPath,
                brush = sweepBrush,
                alpha = 0.55f * pulseAlpha,
                style = Stroke(
                    width = thinStrokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
