package com.setons.trackrep.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.setons.trackrep.exercise.pushup.FatigueLevel
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen

/**
 * Adaptive difficulty feedback rating for a completed workout set.
 */
enum class AdaptiveSetRating(val label: String, val color: Color) {
    TOO_EASY("Too Easy", Color(0xFF64B5F6)),
    JUST_RIGHT("Just Right", SuccessGreen),
    DIFFICULT("Difficult", DarkPrimaryGold),
    COULD_NOT_COMPLETE("Could Not Complete", Color(0xFFEF5350))
}

@Composable
fun SetSummaryDialog(
    summary: CompletedSetSummary,
    onStartRest: (rating: AdaptiveSetRating) -> Unit,
    onSkipToNextSet: (rating: AdaptiveSetRating) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(AdaptiveSetRating.JUST_RIGHT) }

    val formattedDuration = remember(summary.durationSeconds) {
        val minutes = summary.durationSeconds / 60
        val seconds = summary.durationSeconds % 60
        "%02d:%02d".format(minutes, seconds)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, DarkSecondaryGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkPrimaryGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "SET ${summary.setNumber} COMPLETE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimaryGold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Performance Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = Icons.Default.FitnessCenter,
                        value = "${summary.validReps}",
                        unit = "VALID REPS",
                        subtext = if (summary.partialReps > 0) "${summary.partialReps} partial" else "Full ROM",
                        accentColor = DarkPrimaryGold,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        icon = Icons.Default.CheckCircle,
                        value = "${summary.formConsistencyPercent}%",
                        unit = "FORM ACCURACY",
                        subtext = if (summary.formConsistencyPercent >= 85) "Excellent alignment" else "Form degraded",
                        accentColor = if (summary.formConsistencyPercent >= 80) SuccessGreen else Color(0xFFFFB74D),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = Icons.Default.Speed,
                        value = "%.0f°".format(summary.averageDepthDegrees),
                        unit = "AVG DEPTH",
                        subtext = if (summary.averageDepthDegrees <= 95f) "Chest to floor" else "Higher depth",
                        accentColor = Color(0xFF64B5F6),
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        icon = Icons.Default.Schedule,
                        value = formattedDuration,
                        unit = "ACTIVE TIME",
                        subtext = summary.fatigueLevel.label,
                        accentColor = when (summary.fatigueLevel) {
                            FatigueLevel.FRESH -> SuccessGreen
                            FatigueLevel.MODERATE -> DarkPrimaryGold
                            FatigueLevel.HIGH -> Color(0xFFFF7043)
                            FatigueLevel.EXHAUSTED -> Color(0xFFEF5350)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Adaptive difficulty survey
                Text(
                    text = "How did this set feel?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdaptiveSetRating.values().forEach { rating ->
                        val isSelected = selectedRating == rating
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) rating.color.copy(alpha = 0.2f) else Color(0xFF1E1E1E),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) rating.color else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRating = rating }
                        ) {
                            Text(
                                text = rating.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) rating.color else Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Rest Button
                Button(
                    onClick = { onStartRest(selectedRating) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimaryGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Start 60s Rest Period",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Skip to Next Set Outlined Button
                OutlinedButton(
                    onClick = { onSkipToNextSet(selectedRating) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "Skip Rest • Ready Now",
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    value: String,
    unit: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1C1C1C),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 22.sp
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = accentColor.copy(alpha = 0.9f)
            )
        }
    }
}
