package com.setons.trackrep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold

data class SpeedDialOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: () -> Unit
)

/**
 * Expandable Floating Action Button (Speed Dial) in signature TrackRep Black & Gold.
 * Floats at the bottom right and smoothly extends upwards with all essential quick options:
 * Exercise Library with Stickman Demos, AI Vision Coach, TrackAI, and Workout History.
 */
@Composable
fun TrackRepSpeedDial(
    onNavigateToLibrary: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToTrackAi: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    val options = remember(onNavigateToLibrary, onNavigateToCoach, onNavigateToTrackAi, onNavigateToHistory) {
        listOf(
            SpeedDialOption(
                title = "Exercise Library",
                subtitle = "35 Movements & Stickman Demos",
                icon = Icons.Default.MenuBook,
                badge = "FEATURED",
                onClick = onNavigateToLibrary
            ),
            SpeedDialOption(
                title = "AI Vision Coach",
                subtitle = "Live Camera Rep & Form Tracker",
                icon = Icons.Default.FitnessCenter,
                onClick = onNavigateToCoach
            ),
            SpeedDialOption(
                title = "TrackAI Assistant",
                subtitle = "Ask Workout Questions & Advice",
                icon = Icons.Default.AutoAwesome,
                onClick = onNavigateToTrackAi
            ),
            SpeedDialOption(
                title = "Workout History",
                subtitle = "Session Logs, Volume & PRs",
                icon = Icons.Default.History,
                onClick = onNavigateToHistory
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimming backdrop scrim when menu is open
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = false
                    }
            )
        }

        // Floating Action Button & Expanding Menu Stack
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Expanding Options List (Slides up from the FAB)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ),
                exit = fadeOut() + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEach { option ->
                        SpeedDialOptionRow(
                            option = option,
                            onSelect = {
                                isExpanded = false
                                option.onClick()
                            }
                        )
                    }
                }
            }

            // Main Trigger Floating Circle Button
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                shape = CircleShape,
                containerColor = DarkPrimaryGold,
                contentColor = Color.Black,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.size(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close Menu" else "Quick Actions Menu",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

@Composable
private fun SpeedDialOptionRow(
    option: SpeedDialOption,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onSelect
        )
    ) {
        // Label Card with Gold Accent
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xF51E1E1E),
            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.6f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (option.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkPrimaryGold.copy(alpha = 0.2f),
                                border = BorderStroke(0.5.dp, DarkPrimaryGold)
                            ) {
                                Text(
                                    text = option.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DarkPrimaryGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Mini Circular Action Button
        Surface(
            shape = CircleShape,
            color = Color(0xFF242424),
            border = BorderStroke(1.5.dp, DarkPrimaryGold),
            shadowElevation = 6.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = DarkPrimaryGold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
