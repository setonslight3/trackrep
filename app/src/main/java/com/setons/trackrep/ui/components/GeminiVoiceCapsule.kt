package com.setons.trackrep.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.SuccessGreen

/**
 * Authentic Google Gemini-style Assistant UI.
 * Floats at the bottom center of the screen above navigation bars.
 * Includes:
 * 1. Elevated floating response card (e.g. "Starting Push-ups set...")
 * 2. Dark pill with '+' action button, "Ask Track" prompt/transcript, circular mic button, and soundwave icon.
 */
@Composable
fun GeminiVoiceCapsule(
    isVisible: Boolean,
    phase: VoiceAssistantPhase,
    statusText: String,
    speechText: String,
    primaryColor: Color = DarkPrimaryGold,
    onDismiss: () -> Unit,
    onTapMic: () -> Unit,
    onExecuteAction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showActionMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "GeminiCapsuleWaves")

    // Dynamic wave bar heights for the soundwave icon
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(320, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        // Explicitly anchor at the bottom center of the display above the navigation bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Floating Response Card (Like Google Gemini's response pill in Screenshot 1)
                if (speechText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.35f)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = speechText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E1E24),
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEEEEE))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF616161),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Action Shortcut menu when '+' is clicked
                if (showActionMenu) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1C1C20),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth(0.94f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    showActionMenu = false
                                    onExecuteAction("start workout")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = primaryColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    Text("Start Workout", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = {
                                    showActionMenu = false
                                    onExecuteAction("torch")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2C2C32)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("Torch", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Surface(
                                onClick = {
                                    showActionMenu = false
                                    onExecuteAction("tempo 3 1 1")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2C2C32)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("3-1-1 Tempo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // 2. Google Gemini-Style Bottom Capsule Pill (Screenshot 1)
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0xFF141416),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF2E2E34)
                    ),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(68.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = Color.Black.copy(alpha = 0.6f),
                            spotColor = primaryColor.copy(alpha = 0.4f)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Far Left: '+' Button (Like Gemini in Screenshot 1)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { showActionMenu = !showActionMenu },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick Actions",
                                tint = Color(0xFFE0E0E6),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Center: "Ask Track" or Status / Transcript
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .clickable { onTapMic() },
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (phase) {
                                    VoiceAssistantPhase.LISTENING -> "Listening..."
                                    VoiceAssistantPhase.THINKING -> "Thinking..."
                                    VoiceAssistantPhase.SPEAKING -> "Track Speaking"
                                    else -> "Ask Track"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (phase) {
                                    VoiceAssistantPhase.LISTENING -> primaryColor
                                    VoiceAssistantPhase.THINKING -> Color(0xFF80D8FF)
                                    else -> Color.White
                                },
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            if (phase == VoiceAssistantPhase.LISTENING) {
                                Text(
                                    text = "Say a command, e.g. \"Start workout\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFAAAAAE),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Right Side: Circular Filled Mic Button + Soundwave Bars (Screenshot 1)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Circular Green/Gold Mic Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(
                                        if (phase == VoiceAssistantPhase.LISTENING) SuccessGreen else primaryColor,
                                        CircleShape
                                    )
                                    .clickable { onTapMic() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (phase == VoiceAssistantPhase.THINKING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Soundwave / Audio cadence bars icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(if (phase == VoiceAssistantPhase.LISTENING || phase == VoiceAssistantPhase.SPEAKING) wave1.dp else 8.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(if (phase == VoiceAssistantPhase.LISTENING || phase == VoiceAssistantPhase.SPEAKING) wave2.dp else 16.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color.White)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(if (phase == VoiceAssistantPhase.LISTENING || phase == VoiceAssistantPhase.SPEAKING) wave3.dp else 10.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
