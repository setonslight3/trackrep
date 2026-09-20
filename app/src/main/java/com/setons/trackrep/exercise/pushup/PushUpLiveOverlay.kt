package com.setons.trackrep.exercise.pushup

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
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
fun PushUpLiveOverlay(
    telemetry: PushUpLiveTelemetry,
    modifier: Modifier = Modifier,
    setNumber: Int = 1,
    elapsedSeconds: Int = 0,
    fatigueLevel: FatigueLevel = FatigueLevel.FRESH
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTimer = "%02d:%02d".format(minutes, seconds)
    Box(modifier = modifier.fillMaxSize()) {
        // UNIFIED ATHLETIC TOP HUD STRIP: Left Pill (Reps & Phase) + Right Pill (Depth Meter & Angle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PILL: Big Bold Gold Rep Number & Phase Badge
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
                        text = "${telemetry.validRepCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = DarkPrimaryGold,
                        fontSize = 26.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "REPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                            if (telemetry.partialRepCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF3E2723),
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "+${telemetry.partialRepCount} partial",
                                        color = DarkSecondaryGold,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        val (phaseColor, phaseLabel) = when (telemetry.currentPhase) {
                            PushUpPhase.BOTTOM_DEPTH -> SuccessGreen to "FULL DEPTH"
                            PushUpPhase.DESCENDING -> DarkSecondaryGold to "LOWERING"
                            PushUpPhase.ASCENDING -> DarkSecondaryGold to "PRESSING"
                            PushUpPhase.PLANK_READY -> DarkPrimaryGold to "PLANK / READY"
                            PushUpPhase.UNKNOWN -> Color.White.copy(alpha = 0.6f) to "READY"
                        }

                        Text(
                            text = phaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = phaseColor,
                            fontSize = 9.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SET $setNumber • $formattedTimer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 8.sp
                            )
                            if (fatigueLevel != FatigueLevel.FRESH) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (fatigueLevel) {
                                        FatigueLevel.MODERATE -> DarkPrimaryGold.copy(alpha = 0.25f)
                                        FatigueLevel.HIGH -> Color(0xFFFF7043).copy(alpha = 0.25f)
                                        FatigueLevel.EXHAUSTED -> Color(0xFFEF5350).copy(alpha = 0.25f)
                                        else -> Color.Transparent
                                    }
                                ) {
                                    Text(
                                        text = fatigueLevel.label,
                                        color = when (fatigueLevel) {
                                            FatigueLevel.MODERATE -> DarkPrimaryGold
                                            FatigueLevel.HIGH -> Color(0xFFFF7043)
                                            FatigueLevel.EXHAUSTED -> Color(0xFFEF5350)
                                            else -> Color.White
                                        },
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT PILL: Target Range of Motion & Live Depth Meter
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
                            text = "DEPTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        val angleText = telemetry.currentElbowAngle?.let { "${it.toInt()}°" } ?: "--"
                        val isDeep = telemetry.depthProgress >= 0.95f
                        Text(
                            text = angleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDeep) SuccessGreen else DarkPrimaryGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { telemetry.depthProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = if (telemetry.depthProgress >= 0.95f) SuccessGreen else DarkPrimaryGold,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    if (telemetry.averageCadenceSec > 0) {
                        Text(
                            text = String.format("Tempo: %.1fs/rep", telemetry.averageCadenceSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 8.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Safe Floating Warning Banner (Above Bottom Guidance)
        AnimatedVisibility(
            visible = telemetry.activeWarning != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF2E1111).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color(0xFFFF5252))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = telemetry.activeWarning ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
