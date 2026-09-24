package com.setons.trackrep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

            // Main Trigger Floating Circle Button — Gold Stickman One-Arm Push-Up
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                shape = CircleShape,
                containerColor = Color(0xFF141414),
                contentColor = DarkPrimaryGold,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier.size(62.dp)
            ) {
                Crossfade(
                    targetState = isExpanded,
                    animationSpec = tween(durationMillis = 250),
                    label = "fab_crossfade"
                ) { expanded ->
                    if (expanded) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Canvas(
                            modifier = Modifier.size(38.dp)
                        ) {
                            drawOneArmPushUpStickman(
                                width = size.width,
                                height = size.height,
                                gold = DarkPrimaryGold,
                                lightGold = DarkSecondaryGold
                            )
                        }
                    }
                }
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

// -------------------------------------------------------------
// GOLD STICKMAN ONE-ARM PUSH-UP — FAB ICON RENDERER
// Matches the motion-stick style from StickmanDemoPlayer:
// Halo-glow head with visor eye, thick limb strokes, glowing joints.
// -------------------------------------------------------------

/**
 * Draws a gold stickman performing a one-arm push-up.
 * Side view: head left, feet right, one arm supporting from the ground.
 * The free arm is tucked behind the back for the classic one-arm pose.
 */
private fun DrawScope.drawOneArmPushUpStickman(
    width: Float,
    height: Float,
    gold: Color,
    lightGold: Color
) {
    val jointWhite = Color.White
    val groundY = height * 0.88f

    // Key body points — one-arm push-up at the "up" position
    val handX = width * 0.24f
    val handY = groundY

    val feetX = width * 0.82f
    val feetY = groundY

    val shoulderX = handX + width * 0.10f
    val shoulderY = groundY - height * 0.42f

    val hipX = feetX - (feetX - shoulderX) * 0.42f
    val hipY = shoulderY + height * 0.06f  // Slight body angle downward to feet

    val headX = shoulderX - width * 0.14f
    val headY = shoulderY - height * 0.06f
    val headRadius = width * 0.085f

    // Elbow bends slightly (supporting arm)
    val elbowX = handX + (shoulderX - handX) * 0.45f + width * 0.04f
    val elbowY = (shoulderY + handY) / 2f

    // Free arm tucked behind back (from shoulder curving to hip area)
    val freeArmX = shoulderX + width * 0.08f
    val freeArmY = shoulderY + height * 0.14f

    // ── Head with halo glow ──
    // Outer halo glow
    drawCircle(
        color = gold.copy(alpha = 0.25f),
        radius = headRadius * 1.4f,
        center = Offset(headX, headY)
    )
    // Head circle outline
    drawCircle(
        color = gold,
        radius = headRadius,
        center = Offset(headX, headY),
        style = Stroke(width = width * 0.04f)
    )
    // Visor eye line
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(headX - headRadius * 0.45f, headY),
        end = Offset(headX + headRadius * 0.45f, headY),
        strokeWidth = width * 0.032f,
        cap = StrokeCap.Round
    )

    // ── Limbs ──
    val thickStroke = width * 0.055f
    val mediumStroke = width * 0.045f

    // Neck: Head → Shoulder
    drawLine(
        color = gold,
        start = Offset(headX, headY + headRadius * 0.6f),
        end = Offset(shoulderX, shoulderY),
        strokeWidth = mediumStroke,
        cap = StrokeCap.Round
    )

    // Spine/Torso: Shoulder → Hip
    drawLine(
        color = gold,
        start = Offset(shoulderX, shoulderY),
        end = Offset(hipX, hipY),
        strokeWidth = thickStroke,
        cap = StrokeCap.Round
    )

    // Legs: Hip → Feet (straight plank line)
    drawLine(
        color = lightGold,
        start = Offset(hipX, hipY),
        end = Offset(feetX, feetY),
        strokeWidth = mediumStroke,
        cap = StrokeCap.Round
    )

    // Supporting arm: Shoulder → Elbow → Hand
    drawLine(
        color = gold,
        start = Offset(shoulderX, shoulderY),
        end = Offset(elbowX, elbowY),
        strokeWidth = mediumStroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = lightGold,
        start = Offset(elbowX, elbowY),
        end = Offset(handX, handY),
        strokeWidth = mediumStroke,
        cap = StrokeCap.Round
    )

    // Free arm tucked behind back: Shoulder → behind-back point
    drawLine(
        color = gold.copy(alpha = 0.7f),
        start = Offset(shoulderX, shoulderY),
        end = Offset(freeArmX, freeArmY),
        strokeWidth = mediumStroke * 0.85f,
        cap = StrokeCap.Round
    )

    // ── Joints (glowing dots) ──
    val jointRadius = width * 0.035f
    val jointGlowRadius = width * 0.06f

    listOf(
        Offset(shoulderX, shoulderY),
        Offset(elbowX, elbowY),
        Offset(handX, handY),
        Offset(hipX, hipY),
        Offset(feetX, feetY),
        Offset(freeArmX, freeArmY)
    ).forEach { joint ->
        // Outer joint aura
        drawCircle(
            color = gold.copy(alpha = 0.35f),
            radius = jointGlowRadius,
            center = joint
        )
        // Solid joint dot
        drawCircle(
            color = jointWhite,
            radius = jointRadius,
            center = joint
        )
    }
}
