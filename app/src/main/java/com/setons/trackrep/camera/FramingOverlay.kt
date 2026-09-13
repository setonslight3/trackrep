package com.setons.trackrep.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen

@Composable
fun FramingOverlay(
    exerciseMode: ExerciseFramingMode,
    framingStatus: FramingStatus,
    modifier: Modifier = Modifier
) {
    val goldColor = DarkPrimaryGold
    val activeColor = if (framingStatus.isPassing) SuccessGreen else goldColor

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate framing box based on exercise mode
            val boxLeft: Float
            val boxTop: Float
            val boxWidth: Float
            val boxHeight: Float

            when (exerciseMode) {
                ExerciseFramingMode.PUSH_UP, ExerciseFramingMode.PLANK -> {
                    // Wide horizontal aspect for push-up / plank lying down
                    boxWidth = width * 0.88f
                    boxHeight = height * 0.46f
                    boxLeft = (width - boxWidth) / 2f
                    boxTop = (height - boxHeight) / 2f
                }
                ExerciseFramingMode.SQUAT -> {
                    // Taller vertical aspect for standing squat
                    boxWidth = width * 0.75f
                    boxHeight = height * 0.72f
                    boxLeft = (width - boxWidth) / 2f
                    boxTop = (height - boxHeight) / 2f
                }
            }

            // Draw bounding guide with dashed luxury gold line
            drawRoundRect(
                color = activeColor.copy(alpha = 0.55f),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            )

            // Draw corner accents (Solid luxury gold brackets)
            val cornerLen = 32.dp.toPx()
            val strokeW = 4.dp.toPx()

            // Top-Left
            drawLine(activeColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), strokeW)
            drawLine(activeColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), strokeW)

            // Top-Right
            drawLine(activeColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth - cornerLen, boxTop), strokeW)
            drawLine(activeColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth, boxTop + cornerLen), strokeW)

            // Bottom-Left
            drawLine(activeColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft + cornerLen, boxTop + boxHeight), strokeW)
            drawLine(activeColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft, boxTop + boxHeight - cornerLen), strokeW)

            // Bottom-Right
            drawLine(activeColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth - cornerLen, boxTop + boxHeight), strokeW)
            drawLine(activeColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth, boxTop + boxHeight - cornerLen), strokeW)

            // Horizontal center level guide line
            val centerY = boxTop + (boxHeight / 2f)
            drawLine(
                color = activeColor.copy(alpha = 0.25f),
                start = Offset(boxLeft + 20f, centerY),
                end = Offset(boxLeft + boxWidth - 20f, centerY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Live Framing Guidance Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Text(
                text = framingStatus.message,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (framingStatus.isPassing) SuccessGreen else DarkSecondaryGold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Distance & Angle Hint Banner at Bottom
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.70f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "📏 Distance: ${exerciseMode.recommendedDistance}  •  📐 Angle: ${exerciseMode.angleTip}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
