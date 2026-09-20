package com.setons.trackrep.exercise.plank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen

@Composable
fun PlankLiveOverlay(
    telemetry: PlankLiveTelemetry,
    modifier: Modifier = Modifier,
    setNumber: Int = 1,
    elapsedSeconds: Int = 0
) {
    val minutes = telemetry.holdDurationSeconds / 60
    val seconds = telemetry.holdDurationSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Box(modifier = modifier.fillMaxSize()) {
        // UNIFIED ATHLETIC TOP HUD STRIP: Left Pill (Hold Timer & Status) + Right Pill (Spine Alignment Angle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PILL: Hold Timer & Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = DarkPrimaryGold,
                        fontSize = 24.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PLANK HOLD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )

                        val (statusColor, statusLabel) = when (telemetry.currentStatus) {
                            PlankStatus.SOLID_PLANK -> SuccessGreen to "SOLID CORE"
                            PlankStatus.HIP_SAG -> Color(0xFFFF5722) to "HIPS SAGGING"
                            PlankStatus.HIP_PIKE -> Color(0xFFFFB74D) to "HIPS TOO HIGH"
                            PlankStatus.CALIBRATING -> DarkPrimaryGold to "CALIBRATING"
                        }

                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            fontSize = 9.sp
                        )

                        Text(
                            text = "SET $setNumber • Quality: ${telemetry.alignmentScorePercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // RIGHT PILL: Spine Alignment Angle Meter
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.45f)),
                modifier = Modifier.widthIn(min = 120.dp, max = 150.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ALIGNMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        )

                        val angle = telemetry.hipAlignmentAngle
                        val angleText = if (angle != null) "${angle.toInt()}°" else "--"
                        val isSolid = telemetry.currentStatus == PlankStatus.SOLID_PLANK

                        Text(
                            text = angleText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isSolid) SuccessGreen else Color(0xFFFF5722),
                            fontSize = 13.sp
                        )
                    }

                    // Progress mapped to straight line (180 deg = 1.0, 140 deg = 0.0)
                    val alignmentRatio = if (telemetry.hipAlignmentAngle != null) {
                        ((telemetry.hipAlignmentAngle - 140.0) / 40.0).coerceIn(0.0, 1.0).toFloat()
                    } else 1f

                    LinearProgressIndicator(
                        progress = { alignmentRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (telemetry.currentStatus == PlankStatus.SOLID_PLANK) SuccessGreen else Color(0xFFFF5722),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: 180°",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                        Text(
                            text = "Straight Line",
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkSecondaryGold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        // Live Warning Banner
        AnimatedVisibility(
            visible = telemetry.activeWarning != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E100A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = telemetry.activeWarning ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
