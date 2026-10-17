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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Google Gemini-style floating bottom capsule (pill).
 * Appears when Track is awakened, displaying live soundwave bars, status,
 * transcribed speech, and spoken voice responses.
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CapsuleWaveAnimation")

    // 4 individual bar height animations for organic voice wave
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b4"
    )

    // Border shimmer gradient animation
    val borderOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderShimmer"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            Color(0xFFFFF099).copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.5f),
                            Color(0xFFFFB300).copy(alpha = 0.85f)
                        ),
                        startX = borderOffset * 400f,
                        endX = (borderOffset * 400f) + 400f
                    )
                ),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = primaryColor.copy(alpha = 0.4f),
                        spotColor = primaryColor.copy(alpha = 0.6f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Waveform Indicator / Spinner
                    Row(
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                            .clickable { onTapMic() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        when (phase) {
                            VoiceAssistantPhase.THINKING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = primaryColor
                                )
                            }
                            VoiceAssistantPhase.SPEAKING -> {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                // 4 Gemini-style animated sound bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(bar1Height.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(primaryColor)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(bar2Height.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFFFF099))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(bar3Height.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFFFB300))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(bar4Height.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(primaryColor)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Center Content (Headline + Transcript)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        when (phase) {
                                            VoiceAssistantPhase.THINKING -> Color(0xFF80D8FF)
                                            VoiceAssistantPhase.SPEAKING -> primaryColor
                                            else -> Color(0xFF00E676)
                                        },
                                        CircleShape
                                    )
                                    .shadow(4.dp, CircleShape)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = speechText.ifBlank {
                                when (phase) {
                                    VoiceAssistantPhase.LISTENING -> "Say a command, e.g. \"Start workout\""
                                    VoiceAssistantPhase.THINKING -> "Consulting Track AI..."
                                    VoiceAssistantPhase.SPEAKING -> "Speaking response..."
                                    else -> "Say \"Track\" anytime"
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }

                    // Right Actions (Mic + Dismiss)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onTapMic,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listen again",
                                tint = if (phase == VoiceAssistantPhase.LISTENING) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
