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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top Left: Big Rep Counter & Phase Pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${telemetry.validRepCount}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkPrimaryGold,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Phase Badge
                val phaseColor = when (telemetry.currentPhase) {
                    PushUpPhase.BOTTOM_DEPTH -> SuccessGreen
                    PushUpPhase.DESCENDING, PushUpPhase.ASCENDING -> DarkSecondaryGold
                    PushUpPhase.PLANK_READY -> DarkPrimaryGold
                    PushUpPhase.UNKNOWN -> Color.White.copy(alpha = 0.6f)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = phaseColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = telemetry.currentPhase.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Cadence readout if at least 1 rep completed
                if (telemetry.averageCadenceSec > 0) {
                    Text(
                        text = String.format("%.1fs / rep", telemetry.averageCadenceSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Top Center / Floating: Real-Time Depth Meter
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .fillMaxWidth(0.42f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                        Text(
                            text = angleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.depthProgress >= 0.95f) SuccessGreen else DarkPrimaryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { telemetry.depthProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (telemetry.depthProgress >= 0.95f) SuccessGreen else DarkPrimaryGold,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // Bottom Center Warning Toast Pill
        AnimatedVisibility(
            visible = telemetry.activeWarning != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF2E1111),
                border = BorderStroke(1.dp, Color(0xFFFF5252))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = telemetry.activeWarning ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
