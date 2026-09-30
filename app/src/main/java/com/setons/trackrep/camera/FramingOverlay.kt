package com.setons.trackrep.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
                ExerciseFramingMode.SQUAT, ExerciseFramingMode.PULL_UP, ExerciseFramingMode.CARDIO -> {
                    // Taller vertical aspect for standing squat, pull-up, and cardio
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

            // Draw subtle corner accents (sleek camera viewfinder styling)
            val cornerLen = 20.dp.toPx()
            val strokeW = 2.dp.toPx()
            val guideColor = activeColor.copy(alpha = 0.45f)

            // Top-Left
            drawLine(guideColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), strokeW)
            drawLine(guideColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), strokeW)

            // Top-Right
            drawLine(guideColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth - cornerLen, boxTop), strokeW)
            drawLine(guideColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth, boxTop + cornerLen), strokeW)

            // Bottom-Left
            drawLine(guideColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft + cornerLen, boxTop + boxHeight), strokeW)
            drawLine(guideColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft, boxTop + boxHeight - cornerLen), strokeW)

            // Bottom-Right
            drawLine(guideColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth - cornerLen, boxTop + boxHeight), strokeW)
            drawLine(guideColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth, boxTop + boxHeight - cornerLen), strokeW)
        }
    }
}
