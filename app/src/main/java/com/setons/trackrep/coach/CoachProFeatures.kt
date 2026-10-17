package com.setons.trackrep.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.SuccessGreen
import com.setons.trackrep.ui.demo.StickmanArchetype
import com.setons.trackrep.ui.demo.drawStickmanDemonstration
import kotlinx.coroutines.delay

/**
 * Preset configurations for the Tempo & Eccentric Cadence Metronome (Hypertrophy TUT Booster).
 */
enum class TempoPreset(
    val id: String,
    val title: String,
    val shortBadge: String,
    val eccentricSec: Int,
    val bottomHoldSec: Int,
    val concentricSec: Int,
    val description: String
) {
    OFF(
        id = "OFF",
        title = "Off (Free Cadence)",
        shortBadge = "Tempo Off",
        eccentricSec = 0,
        bottomHoldSec = 0,
        concentricSec = 0,
        description = "Rep at your natural rhythm"
    ),
    BEGINNER_2_1_2(
        id = "2-1-2",
        title = "Beginner Control (2-1-2)",
        shortBadge = "2-1-2 Tempo",
        eccentricSec = 2,
        bottomHoldSec = 1,
        concentricSec = 2,
        description = "2s Down • 1s Pause • 2s Smooth Up"
    ),
    HYPERTROPHY_3_1_1(
        id = "3-1-1",
        title = "Hypertrophy TUT (3-1-1)",
        shortBadge = "3-1-1 TUT",
        eccentricSec = 3,
        bottomHoldSec = 1,
        concentricSec = 1,
        description = "3s Slow Lower • 1s Hold • 1s Drive"
    ),
    EXPLOSIVE_2_0_1(
        id = "2-0-1",
        title = "Athletic Power (2-0-1)",
        shortBadge = "2-0-1 Power",
        eccentricSec = 2,
        bottomHoldSec = 0,
        concentricSec = 1,
        description = "2s Controlled Down • 1s Explosive Up"
    ),
    SLOW_BURN_4_2_1(
        id = "4-2-1",
        title = "Slow Burn TUT (4-2-1)",
        shortBadge = "4-2-1 Burn",
        eccentricSec = 4,
        bottomHoldSec = 2,
        concentricSec = 1,
        description = "4s Deep Negative • 2s Bottom Hold • 1s Up"
    );

    val totalRepDurationMs: Int
        get() = (eccentricSec + bottomHoldSec + concentricSec) * 1000

    companion object {
        fun fromId(id: String): TempoPreset {
            return entries.firstOrNull { it.id == id } ?: OFF
        }

        fun nextPreset(currentId: String): TempoPreset {
            val all = entries
            val idx = all.indexOfFirst { it.id == currentId }
            return if (idx == -1 || idx + 1 >= all.size) OFF else all[idx + 1]
        }
    }
}

enum class TempoPhase(val label: String, val color: Color) {
    ECCENTRIC_DOWN("LOWER SLOWLY", Color(0xFFFFB300)),
    BOTTOM_HOLD("HOLD DEPTH", Color(0xFF00E676)),
    CONCENTRIC_UP("DRIVE UP!", Color(0xFF00E5FF))
}

data class LiveTempoTick(
    val phase: TempoPhase = TempoPhase.ECCENTRIC_DOWN,
    val remainingSecInPhase: Int = 3,
    val phaseProgress: Float = 0f,
    val repDepthCycle: Float = 0f // 0f = top lockout, 1f = full bottom depth
)

/**
 * Calculates live Tempo Metronome state and optional voice cadence cues.
 */
@Composable
fun rememberTempoMetronomeState(
    presetId: String,
    isActiveWorkout: Boolean,
    voiceManager: CoachVoiceManager
): LiveTempoTick? {
    val preset = remember(presetId) { TempoPreset.fromId(presetId) }
    if (preset == TempoPreset.OFF) return null

    var currentTick by remember(preset) {
        mutableStateOf(
            LiveTempoTick(
                phase = TempoPhase.ECCENTRIC_DOWN,
                remainingSecInPhase = preset.eccentricSec,
                phaseProgress = 0f,
                repDepthCycle = 0f
            )
        )
    }

    LaunchedEffect(preset, isActiveWorkout) {
        val stepMs = 50L
        val eccMs = preset.eccentricSec * 1000L
        val holdMs = preset.bottomHoldSec * 1000L
        val conMs = preset.concentricSec * 1000L
        val pauseTopMs = 450L // Brief lockout breath between reps
        val cycleTotalMs = eccMs + holdMs + conMs + pauseTopMs

        var elapsedInRepMs = 0L
        var lastSpokenPhase: TempoPhase? = null

        while (true) {
            when {
                elapsedInRepMs < eccMs -> {
                    val phaseElapsed = elapsedInRepMs
                    val frac = (phaseElapsed.toFloat() / eccMs.toFloat()).coerceIn(0f, 1f)
                    val remSec = ((eccMs - phaseElapsed + 999L) / 1000L).toInt().coerceAtLeast(1)
                    currentTick = LiveTempoTick(
                        phase = TempoPhase.ECCENTRIC_DOWN,
                        remainingSecInPhase = remSec,
                        phaseProgress = frac,
                        repDepthCycle = frac // Smoothly descends 0.0 -> 1.0
                    )
                    if (isActiveWorkout && lastSpokenPhase != TempoPhase.ECCENTRIC_DOWN) {
                        lastSpokenPhase = TempoPhase.ECCENTRIC_DOWN
                        voiceManager.speakStatus("Down", isUrgent = false)
                    }
                }
                elapsedInRepMs < eccMs + holdMs -> {
                    val phaseElapsed = elapsedInRepMs - eccMs
                    val frac = if (holdMs > 0) (phaseElapsed.toFloat() / holdMs.toFloat()).coerceIn(0f, 1f) else 1f
                    val remSec = ((holdMs - phaseElapsed + 999L) / 1000L).toInt().coerceAtLeast(1)
                    currentTick = LiveTempoTick(
                        phase = TempoPhase.BOTTOM_HOLD,
                        remainingSecInPhase = remSec,
                        phaseProgress = frac,
                        repDepthCycle = 1f // Held at full bottom depth 1.0
                    )
                    if (isActiveWorkout && lastSpokenPhase != TempoPhase.BOTTOM_HOLD) {
                        lastSpokenPhase = TempoPhase.BOTTOM_HOLD
                        voiceManager.speakStatus("Hold", isUrgent = false)
                    }
                }
                elapsedInRepMs < eccMs + holdMs + conMs -> {
                    val phaseElapsed = elapsedInRepMs - eccMs - holdMs
                    val frac = (phaseElapsed.toFloat() / conMs.toFloat()).coerceIn(0f, 1f)
                    val remSec = ((conMs - phaseElapsed + 999L) / 1000L).toInt().coerceAtLeast(1)
                    currentTick = LiveTempoTick(
                        phase = TempoPhase.CONCENTRIC_UP,
                        remainingSecInPhase = remSec,
                        phaseProgress = frac,
                        repDepthCycle = (1f - frac).coerceIn(0f, 1f) // Smoothly ascends 1.0 -> 0.0
                    )
                    if (isActiveWorkout && lastSpokenPhase != TempoPhase.CONCENTRIC_UP) {
                        lastSpokenPhase = TempoPhase.CONCENTRIC_UP
                        voiceManager.speakStatus("Push", isUrgent = false)
                    }
                }
                else -> {
                    // Brief lockout reset at top
                    currentTick = LiveTempoTick(
                        phase = TempoPhase.CONCENTRIC_UP,
                        remainingSecInPhase = 0,
                        phaseProgress = 1f,
                        repDepthCycle = 0f
                    )
                }
            }

            delay(stepMs)
            elapsedInRepMs = (elapsedInRepMs + stepMs) % cycleTotalMs
            if (elapsedInRepMs < stepMs) {
                lastSpokenPhase = null
            }
        }
    }

    return currentTick
}

/**
 * Beginner "Ghost Shadow" Form Overlay (AR Silhouette).
 * Renders a translucent holographic green/cyan biomechanical guide directly on the camera feed.
 * Synchronizes automatically with the Tempo Metronome when active!
 */
@Composable
fun GhostShadowOverlay(
    exerciseId: String,
    isEnabled: Boolean,
    tempoTick: LiveTempoTick?,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val archetype = remember(exerciseId) { StickmanArchetype.fromExerciseId(exerciseId) }

    val baseDurationMs = remember(archetype) {
        when (archetype) {
            StickmanArchetype.PLANK, StickmanArchetype.PLANK_KNEE,
            StickmanArchetype.PLANK_SIDE, StickmanArchetype.HOLLOW_BODY,
            StickmanArchetype.SUPERMAN, StickmanArchetype.HANDSTAND_HOLD -> 4000
            StickmanArchetype.BIRD_DOG -> 3400
            StickmanArchetype.DRAGON_FLAG -> 3000
            StickmanArchetype.MOUNTAIN_CLIMBER -> 1400
            StickmanArchetype.SQUAT_JUMP -> 2200
            StickmanArchetype.BURPEE -> 3200
            else -> 2800
        }
    }

    val transition = rememberInfiniteTransition(label = "ghost_shadow_cycle")
    val naturalCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = baseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ghost_natural_progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            drawStickmanDemonstration(
                archetype = archetype,
                t = naturalCycle,
                width = size.width,
                height = size.height,
                primaryColor = Color(0xFF00E676).copy(alpha = 0.45f),
                secondaryColor = Color(0xFF00E5FF).copy(alpha = 0.35f),
                jointColor = Color.White.copy(alpha = 0.65f),
                customRepCycle = tempoTick?.repDepthCycle,
                showFloorLine = false
            )
        }

        // Subtle floating indicator pill at bottom-start
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.55f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 52.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00E676), CircleShape)
                )
                Text(
                    text = if (tempoTick != null) "AR GHOST • TEMPO SYNCED" else "AR GHOST FORM GUIDE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Audio-Visual Tempo & Eccentric Cadence Metronome HUD Bar.
 */
@Composable
fun TempoMetronomeHudBar(
    presetId: String,
    tempoTick: LiveTempoTick?,
    onCyclePreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tempoTick == null || presetId == "OFF") return

    Surface(
        onClick = onCyclePreset,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xD9101010),
        border = BorderStroke(1.5.dp, tempoTick.phase.color.copy(alpha = 0.8f)),
        modifier = modifier
            .fillMaxWidth(0.76f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = tempoTick.phase.color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "TEMPO $presetId • ${tempoTick.phase.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tempoTick.phase.color,
                        fontSize = 10.sp
                    )
                }

                if (tempoTick.remainingSecInPhase > 0) {
                    Text(
                        text = "${tempoTick.remainingSecInPhase}s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            LinearProgressIndicator(
                progress = { tempoTick.phaseProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = tempoTick.phase.color,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * Hands-Free Pose Stabilization Countdown to Start ("3... 2... 1... Go!").
 * Detects when the athlete steps into camera view and holds ready position,
 * then counts down hands-free and starts recording the set automatically.
 */
@Composable
fun HandsFreeStartController(
    isHandsFreeEnabled: Boolean,
    isCameraEnabled: Boolean,
    isRecording: Boolean,
    setLifecycleState: SetLifecycleState,
    isPoseTrackingValid: Boolean,
    voiceManager: CoachVoiceManager,
    onStartSetTriggered: () -> Unit,
    onDisableHandsFree: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only arm when not recording and not in between-set rest timer
    val isArmed = isHandsFreeEnabled && isCameraEnabled && !isRecording && setLifecycleState != SetLifecycleState.RESTING

    var countdownSec by remember { mutableIntStateOf(-1) }
    var statusLabel by remember { mutableStateOf("Step into frame to auto-start") }
    var cooldownUntilMs by remember { mutableStateOf(0L) }

    // Reset cooldown when recording stops so user has 4 seconds after finishing a set before re-arming
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            cooldownUntilMs = System.currentTimeMillis() + 3500L
            countdownSec = -1
        }
    }

    LaunchedEffect(isArmed, isPoseTrackingValid) {
        if (!isArmed || !isPoseTrackingValid) {
            countdownSec = -1
            statusLabel = "Step into frame to auto-start"
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        if (now < cooldownUntilMs) {
            delay(cooldownUntilMs - now)
        }

        // Stabilize pose for 900ms before starting countdown
        statusLabel = "Pose locked • Get Ready..."
        delay(900L)

        // 3... 2... 1... Countdown
        for (sec in 3 downTo 1) {
            countdownSec = sec
            statusLabel = "Starting set hands-free..."
            voiceManager.speakCountdown(sec)
            delay(1000L)
        }

        countdownSec = 0
        onStartSetTriggered()
        countdownSec = -1
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Center Countdown Ring when actively counting down 3..2..1
        AnimatedVisibility(
            visible = isArmed && countdownSec in 1..3,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.88f),
                border = BorderStroke(2.5.dp, SuccessGreen),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "HANDS-FREE START",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$countdownSec",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 52.sp
                    )

                    Text(
                        text = "Hold position • Set starting!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            countdownSec = -1
                            onDisableHandsFree()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
