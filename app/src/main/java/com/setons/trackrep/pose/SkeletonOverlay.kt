package com.setons.trackrep.pose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
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
    modifier: Modifier = Modifier
) {
    val goldColor = DarkPrimaryGold
    val flawColor = Color(0xFFFF5252) // Warning red for incorrect form
    val goodColor = SuccessGreen

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pose == null || !pose.isTrackingValid) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            fun getScreenPoint(landmark: TrackedLandmark?): Offset? {
                if (landmark == null || landmark.inFrameLikelihood < 0.40f) return null
                // Account for horizontal mirror when using front camera
                val finalX = if (isFrontCamera) (1f - landmark.x) * canvasW else landmark.x * canvasW
                val finalY = landmark.y * canvasH
                return Offset(finalX, finalY)
            }

            fun drawLimb(startType: Int, endType: Int, color: Color, strokeWidth: Float = 6.dp.toPx()) {
                val start = getScreenPoint(pose.landmarks[startType])
                val end = getScreenPoint(pose.landmarks[endType])
                if (start != null && end != null) {
                    // Glow background line
                    drawLine(
                        color = color.copy(alpha = 0.35f),
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth * 1.8f,
                        cap = StrokeCap.Round
                    )
                    // Core line
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
            val armColor = if (hasFlaw && pose.formIssues.any { it.contains("Elbow", ignoreCase = true) }) flawColor else goldColor
            val hipColor = if (hasFlaw && pose.formIssues.any { it.contains("hip", ignoreCase = true) }) flawColor else goldColor

            // 1. Draw Upper Body / Arms
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, armColor)
            drawLimb(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, armColor)
            drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, armColor)
            drawLimb(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, armColor)

            // 2. Draw Torso Frame
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, goldColor)
            drawLimb(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, hipColor)
            drawLimb(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, hipColor)
            drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, hipColor)

            // 3. Draw Legs
            drawLimb(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, goldColor)
            drawLimb(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, goldColor)
            drawLimb(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, goldColor)
            drawLimb(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, goldColor)

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
                        else -> goldColor
                    }
                    // Outer glow ring
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.4f),
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                    // Inner solid core
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // Live Dynamic Angle & Form Correction HUD
        if (pose != null && pose.isTrackingValid) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "POSE TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkSecondaryGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    if (pose.leftElbowAngle != null || pose.rightElbowAngle != null) {
                        val elbowAngle = pose.leftElbowAngle ?: pose.rightElbowAngle ?: 0.0
                        Text(
                            text = "Elbow Angle: ${elbowAngle.toInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (pose.hipAlignmentAngle != null) {
                        Text(
                            text = "Hip Line: ${pose.hipAlignmentAngle.toInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (pose.formIssues.isNotEmpty()) {
                        Text(
                            text = "⚠️ " + pose.formIssues.first(),
                            style = MaterialTheme.typography.labelSmall,
                            color = flawColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
