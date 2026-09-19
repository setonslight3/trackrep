package com.setons.trackrep.pose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen

@Composable
fun SkeletonOverlay(
    pose: TrackedPose?,
    isFrontCamera: Boolean = false,
    isSuccessFlash: Boolean = false,
    modifier: Modifier = Modifier
) {
    val goldColor = DarkPrimaryGold
    val flawColor = Color(0xFFFF5252) // Warning red for incorrect form
    val electricGreen = Color(0xFF00E676) // Radiant electric neon green for completed action/rep
    val flashGlowGreen = Color(0xFF69F0AE) // Soft luminous halo

    // Animate green burst when a rep/action completes
    val flashAnim = remember { Animatable(0f) }

    LaunchedEffect(isSuccessFlash) {
        if (isSuccessFlash) {
            flashAnim.snapTo(1f)
            flashAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 700,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val flashAlpha = flashAnim.value
    val isFlashing = flashAlpha > 0.01f
    val activeLimbColor = if (isFlashing) lerp(goldColor, electricGreen, flashAlpha) else goldColor

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pose == null || !pose.isTrackingValid) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            // 0. Soft Screen Peripheral Flash Pulse on completed action
            if (isFlashing) {
                drawRect(
                    color = electricGreen.copy(alpha = 0.12f * flashAlpha),
                    size = size
                )
            }

            fun getScreenPoint(landmark: TrackedLandmark?): Offset? {
                if (landmark == null || landmark.inFrameLikelihood < 0.40f) return null
                // Account for horizontal mirror when using front camera
                val finalX = if (isFrontCamera) (1f - landmark.x) * canvasW else landmark.x * canvasW
                val finalY = landmark.y * canvasH
                return Offset(finalX, finalY)
            }

            fun drawLimb(startType: Int, endType: Int, color: Color, strokeWidth: Float = (6f + 2.5f * flashAlpha).dp.toPx()) {
                val start = getScreenPoint(pose.landmarks[startType])
                val end = getScreenPoint(pose.landmarks[endType])
                if (start != null && end != null) {
                    val glowMultiplier = 1.8f + 1.2f * flashAlpha
                    val glowAlpha = (0.35f + 0.50f * flashAlpha).coerceAtMost(0.90f)
                    val glowColor = if (isFlashing) flashGlowGreen.copy(alpha = glowAlpha) else color.copy(alpha = 0.35f)

                    // Luminous Bloom / Halo Line
                    drawLine(
                        color = glowColor,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth * glowMultiplier,
                        cap = StrokeCap.Round
                    )
                    // Solid Core Motion Stick
                    drawLine(
                        color = color,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            val hasFlaw = pose.formIssues.isNotEmpty()
            val armColor = if (isFlashing) {
                activeLimbColor
            } else if (hasFlaw && pose.formIssues.any { it.contains("Elbow", ignoreCase = true) }) {
                flawColor
            } else {
                goldColor
            }

            val hipColor = if (isFlashing) {
                activeLimbColor
            } else if (hasFlaw && pose.formIssues.any { it.contains("hip", ignoreCase = true) }) {
                flawColor
            } else {
                goldColor
            }

            val torsoLimbColor = if (isFlashing) activeLimbColor else goldColor

            // 1. Draw Upper Body / Arms
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, armColor)
            drawLimb(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, armColor)
            drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, armColor)
            drawLimb(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, armColor)

            // 2. Draw Torso Frame
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, torsoLimbColor)
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, hipColor)
            drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, hipColor)
            drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, hipColor)

            // 3. Draw Legs
            drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, torsoLimbColor)
            drawLimb(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, torsoLimbColor)
            drawLimb(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, torsoLimbColor)
            drawLimb(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, torsoLimbColor)

            // 4. Draw Joint Nodes
            val jointTypes = listOf(
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
            )

            for (jt in jointTypes) {
                val pt = getScreenPoint(pose.landmarks[jt])
                if (pt != null) {
                    val nodeColor = when (jt) {
                        PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW -> armColor
                        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP -> hipColor
                        else -> torsoLimbColor
                    }
                    val outerRadius = (10f + 5f * flashAlpha).dp.toPx()
                    val outerAlpha = (0.40f + 0.45f * flashAlpha).coerceAtMost(0.90f)

                    // Outer glowing node ring
                    drawCircle(
                        color = if (isFlashing) flashGlowGreen.copy(alpha = outerAlpha) else nodeColor.copy(alpha = 0.4f),
                        radius = outerRadius,
                        center = pt
                    )
                    // Inner solid high-contrast core
                    drawCircle(
                        color = if (isFlashing) Color.White else nodeColor,
                        radius = (4f + 1f * flashAlpha).dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}
