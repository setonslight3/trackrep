package com.setons.trackrep.ui.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Stickman Animation Archetypes for all 35 catalog exercises.
 */
enum class StickmanArchetype {
    PUSH_UP,
    SQUAT,
    LUNGE,
    PLANK,
    HOLLOW_BODY,
    SUPERMAN,
    GLUTE_BRIDGE,
    PIKE_INVERTED,
    PULL_HANG,
    CORE_V_UP,
    MOUNTAIN_CLIMBER,
    BURPEE,
    CALF_RAISE,
    GOOD_MORNING,
    NORDIC_CURL,
    SCAPULAR_CIRCLES;

    companion object {
        fun fromExerciseId(id: String): StickmanArchetype {
            return when {
                id.startsWith("push_up") || id == "close_grip_push_up" || id == "bench_dips" -> PUSH_UP
                id == "lunge_reverse" -> LUNGE
                id.startsWith("squat") -> SQUAT
                id == "calf_raise_double" || id == "calf_raise_single" -> CALF_RAISE
                id == "good_mornings" -> GOOD_MORNING
                id == "nordic_curl" -> NORDIC_CURL
                id == "arm_circles_scapular" -> SCAPULAR_CIRCLES
                id.startsWith("plank") || id == "handstand_hold" -> PLANK
                id == "hollow_body_hold" -> HOLLOW_BODY
                id == "superman_hold" || id == "bird_dog" -> SUPERMAN
                id == "glute_bridge" || id == "single_leg_bridge" -> GLUTE_BRIDGE
                id == "pike_push_up" -> PIKE_INVERTED
                id == "pull_up_standard" || id == "inverted_row" -> PULL_HANG
                id == "v_ups" || id == "dragon_flag" -> CORE_V_UP
                id == "mountain_climber" -> MOUNTAIN_CLIMBER
                id == "burpee_standard" -> BURPEE
                else -> PUSH_UP
            }
        }
    }
}

/**
 * Luxury 2D Animated Stickman Demonstration Canvas.
 * Demonstrates exact biomechanical form with smooth cyclical keyframe interpolation.
 * 100% on-device vector rendering with zero video overhead.
 */
@Composable
fun StickmanDemoPlayer(
    exerciseId: String,
    modifier: Modifier = Modifier,
    heightDp: Int = 220,
    showControls: Boolean = true
) {
    val exercise = remember(exerciseId) { ExerciseCatalog.getById(exerciseId) }
    val archetype = remember(exerciseId) { StickmanArchetype.fromExerciseId(exerciseId) }

    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }

    val baseDurationMs = remember(archetype) {
        when (archetype) {
            StickmanArchetype.PLANK, StickmanArchetype.HOLLOW_BODY, StickmanArchetype.SUPERMAN -> 4000
            StickmanArchetype.MOUNTAIN_CLIMBER -> 1400
            StickmanArchetype.BURPEE -> 3200
            else -> 2600
        }
    }

    val animatedDuration = (baseDurationMs / speedMultiplier).toInt()

    val transition = rememberInfiniteTransition(label = "stickman_cycle")
    val cycleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animatedDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle_progress"
    )

    val currentProgress = if (isPlaying) cycleProgress else 0.5f

    // Calculate movement phase string
    val phaseLabel = remember(currentProgress, archetype) {
        when (archetype) {
            StickmanArchetype.PLANK, StickmanArchetype.HOLLOW_BODY, StickmanArchetype.SUPERMAN -> {
                "ISOMETRIC HOLD • SOLID CORE"
            }
            StickmanArchetype.MOUNTAIN_CLIMBER -> {
                "RAPID CADENCE • ALTERNATING DRIVE"
            }
            StickmanArchetype.BURPEE -> {
                when {
                    currentProgress < 0.25f -> "1. DROP TO PLANK"
                    currentProgress < 0.5f -> "2. CHEST TO FLOOR"
                    currentProgress < 0.75f -> "3. SNAP FEET IN"
                    else -> "4. EXPLOSIVE JUMP"
                }
            }
            else -> {
                when {
                    currentProgress < 0.45f -> "DESCENT (ECCENTRIC)"
                    currentProgress < 0.55f -> "PEAK CONTRACTION"
                    currentProgress < 0.90f -> "ASCENT (CONCENTRIC)"
                    else -> "FULL LOCKOUT"
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF141414),
        border = BorderStroke(1.5.dp, DarkPrimaryGold.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar: Exercise Name & Phase Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(DarkPrimaryGold, CircleShape)
                    )
                    Text(
                        text = "FORM DEMO • AI VIRTUAL MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkPrimaryGold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF262114),
                    border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = phaseLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimaryGold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Stickman Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1A1A1A), Color(0xFF101010))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    drawStickmanDemonstration(
                        archetype = archetype,
                        t = currentProgress,
                        width = size.width,
                        height = size.height
                    )
                }
            }

            if (showControls) {
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Controls: Play/Pause, Speed Chip, Pro Tip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF2A2A2A), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkPrimaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                speedMultiplier = when (speedMultiplier) {
                                    1f -> 0.6f
                                    0.6f -> 1.4f
                                    else -> 1f
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A2A2A)
                        ) {
                            Text(
                                text = "${speedMultiplier}x Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Key Biomechanical Hint
                    exercise?.proTip?.let { tip ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = DarkSecondaryGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mathematical Keyframe Interpolation & Vector Drawing.
 */
private fun DrawScope.drawStickmanDemonstration(
    archetype: StickmanArchetype,
    t: Float,
    width: Float,
    height: Float
) {
    val gold = DarkPrimaryGold
    val lightGold = DarkSecondaryGold
    val jointWhite = Color.White
    val groundY = height * 0.84f

    // Draw Ground / Floor reference
    drawLine(
        color = gold.copy(alpha = 0.35f),
        start = Offset(0f, groundY),
        end = Offset(width, groundY),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    )

    // Symmetric cycle sine wave: 0.0 -> 1.0 -> 0.0 (smooth eccentric/concentric curve)
    val repCycle = sin(t * PI.toFloat()).coerceIn(0f, 1f)

    when (archetype) {
        StickmanArchetype.PUSH_UP -> {
            drawPushUpAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SQUAT -> {
            drawSquatAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.LUNGE -> {
            drawLungeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PLANK -> {
            drawPlankAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.HOLLOW_BODY -> {
            drawHollowBodyAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SUPERMAN -> {
            drawSupermanAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.GLUTE_BRIDGE -> {
            drawGluteBridgeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PIKE_INVERTED -> {
            drawPikeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PULL_HANG -> {
            drawPullHangAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.CORE_V_UP -> {
            drawVUpAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.MOUNTAIN_CLIMBER -> {
            drawMountainClimberAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.BURPEE -> {
            drawBurpeeAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.CALF_RAISE -> {
            drawCalfRaiseAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.GOOD_MORNING -> {
            drawGoodMorningAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.NORDIC_CURL -> {
            drawNordicCurlAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SCAPULAR_CIRCLES -> {
            drawScapularCirclesAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
    }
}

// -------------------------------------------------------------
// SPECIFIC EXERCISE ANIMATORS
// -------------------------------------------------------------

private fun DrawScope.drawPushUpAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val handX = width * 0.32f
    val handY = groundY
    val feetX = width * 0.78f
    val feetY = groundY

    // In push-up: descent lowers shoulder & hip
    val topShoulderY = groundY - height * 0.40f
    val bottomShoulderY = groundY - height * 0.12f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = handX + 8.dp.toPx()

    val topHipY = groundY - height * 0.32f
    val bottomHipY = groundY - height * 0.10f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = feetX - (feetX - shoulderX) * 0.45f

    val headX = shoulderX - width * 0.12f
    val headY = shoulderY - height * 0.05f

    // Elbow flares outward & backward on descent
    val elbowX = handX + (shoulderX - handX) * 0.5f + width * 0.08f * cycle
    val elbowY = (shoulderY + handY) / 2f + height * 0.04f * cycle

    // Draw Head
    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)

    // Spine: Head -> Shoulder -> Hip -> Feet
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)

    // Arm: Shoulder -> Elbow -> Hand
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    // Joints
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawSquatAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.5f

    // Standing vs Bottom Squat
    val topHipY = groundY - height * 0.52f
    val bottomHipY = groundY - height * 0.22f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = centerX - width * 0.08f * cycle // Hips hinge back

    val topShoulderY = groundY - height * 0.80f
    val bottomShoulderY = groundY - height * 0.44f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = centerX + width * 0.03f * cycle // Slight forward torso lean

    val headX = shoulderX + width * 0.02f * cycle
    val headY = shoulderY - height * 0.09f

    // Knee pushes forward over toes
    val kneeX = centerX + width * 0.12f * cycle
    val kneeY = groundY - height * 0.24f + height * 0.06f * cycle

    val footX = centerX + width * 0.02f
    val footY = groundY

    // Arms reach forward for balance
    val handX = shoulderX - width * 0.22f * cycle - width * 0.04f
    val handY = shoulderY + height * 0.05f * (1f - cycle)
    val elbowX = (shoulderX + handX) / 2f
    val elbowY = (shoulderY + handY) / 2f

    // Draw Head
    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)

    // Spine
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Leg: Hip -> Knee -> Foot
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), gold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    // Arm: Shoulder -> Elbow -> Hand
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    // Joints
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

private fun DrawScope.drawLungeAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.45f

    // Hip lowers straight down
    val topHipY = groundY - height * 0.52f
    val bottomHipY = groundY - height * 0.24f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = centerX

    val shoulderY = hipY - height * 0.28f
    val shoulderX = hipX
    val headY = shoulderY - height * 0.09f
    val headX = shoulderX

    // Front leg (planted, bends to 90 deg)
    val frontFootX = centerX + width * 0.16f
    val frontFootY = groundY
    val frontKneeX = frontFootX - width * 0.02f
    val frontKneeY = groundY - height * 0.22f + height * 0.04f * cycle

    // Back leg (steps back, knee drops to floor)
    val backFootX = centerX - width * 0.24f * cycle
    val backFootY = groundY
    val backKneeX = (hipX + backFootX) / 2f
    val backKneeY = hipY + (groundY - height * 0.04f - hipY) * cycle

    // Hands on hips
    val elbowX = hipX - width * 0.08f
    val elbowY = hipY - height * 0.08f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Front Leg
    drawLimb(Offset(hipX, hipY), Offset(frontKneeX, frontKneeY), gold, 7f)
    drawLimb(Offset(frontKneeX, frontKneeY), Offset(frontFootX, frontFootY), lightGold, 6f)

    // Back Leg
    drawLimb(Offset(hipX, hipY), Offset(backKneeX, backKneeY), gold.copy(alpha = 0.7f), 6f)
    drawLimb(Offset(backKneeX, backKneeY), Offset(backFootX, backFootY), lightGold.copy(alpha = 0.7f), 5f)

    // Arm (hand on hip)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(hipX, hipY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(frontKneeX, frontKneeY), gold, jointWhite)
    drawJoint(Offset(frontFootX, frontFootY), gold, jointWhite)
    drawJoint(Offset(backKneeX, backKneeY), gold, jointWhite)
    drawJoint(Offset(backFootX, backFootY), gold, jointWhite)
}

private fun DrawScope.drawPlankAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val elbowX = width * 0.30f
    val elbowY = groundY
    val feetX = width * 0.76f
    val feetY = groundY

    val shoulderX = elbowX + 4.dp.toPx()
    val shoulderY = groundY - height * 0.22f
    val hipX = feetX - (feetX - shoulderX) * 0.48f
    val hipY = shoulderY + (groundY - shoulderY) * 0.12f

    val headX = shoulderX - width * 0.12f
    val headY = shoulderY - height * 0.04f

    // Core pulsing energy ring
    val pulseAlpha = (0.3f + 0.35f * sin(t * 2 * PI.toFloat())).coerceIn(0.1f, 0.7f)
    drawCircle(
        color = gold.copy(alpha = pulseAlpha),
        radius = 24.dp.toPx(),
        center = Offset(hipX, hipY),
        style = Stroke(width = 2.dp.toPx())
    )

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 8f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 7f)

    // Forearm & Upper Arm
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 6f)
    drawLimb(Offset(elbowX, elbowY), Offset(elbowX - width * 0.06f, elbowY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawHollowBodyAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.5f
    val hipY = groundY - height * 0.04f // Pinned to floor
    val hipX = centerX

    val shoulderX = hipX - width * 0.22f
    val shoulderY = groundY - height * 0.18f - height * 0.05f * cycle

    val feetX = hipX + width * 0.24f
    val feetY = groundY - height * 0.16f - height * 0.05f * cycle

    val armX = shoulderX - width * 0.14f
    val armY = shoulderY - height * 0.08f

    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.04f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 7f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(armX, armY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
    drawJoint(Offset(armX, armY), gold, jointWhite)
}

private fun DrawScope.drawSupermanAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val hipX = width * 0.5f
    val hipY = groundY - height * 0.04f

    val shoulderX = hipX - width * 0.22f
    val shoulderY = groundY - height * 0.08f - height * 0.16f * cycle

    val feetX = hipX + width * 0.24f
    val feetY = groundY - height * 0.06f - height * 0.14f * cycle

    val armX = shoulderX - width * 0.14f
    val armY = shoulderY - height * 0.06f

    val headX = shoulderX - width * 0.07f
    val headY = shoulderY - height * 0.06f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 7f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(armX, armY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
    drawJoint(Offset(armX, armY), gold, jointWhite)
}

private fun DrawScope.drawGluteBridgeAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val feetX = width * 0.32f
    val feetY = groundY

    val shoulderX = width * 0.72f
    val shoulderY = groundY - height * 0.06f
    val headX = shoulderX + width * 0.10f
    val headY = shoulderY

    // Knee angle
    val kneeX = feetX + width * 0.04f
    val kneeY = groundY - height * 0.26f

    // Hip rises from floor to align straight between knee & shoulder
    val bottomHipY = groundY - height * 0.06f
    val topHipY = (kneeY + shoulderY) / 2f
    val hipY = bottomHipY + (topHipY - bottomHipY) * cycle
    val hipX = (kneeX + shoulderX) / 2f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(feetX, feetY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawPikeAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val handX = width * 0.35f
    val handY = groundY
    val feetX = width * 0.68f
    val feetY = groundY

    val apexHipX = (handX + feetX) / 2f
    val apexHipY = groundY - height * 0.60f

    // On dip: shoulder/head dips forward and down toward hands
    val topShoulderX = handX + width * 0.08f
    val topShoulderY = groundY - height * 0.38f
    val bottomShoulderX = handX - width * 0.02f
    val bottomShoulderY = groundY - height * 0.12f

    val shoulderX = topShoulderX + (bottomShoulderX - topShoulderX) * cycle
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle

    val elbowX = handX + (shoulderX - handX) * 0.5f - width * 0.08f * cycle
    val elbowY = (shoulderY + handY) / 2f + height * 0.05f * cycle

    val headX = shoulderX - width * 0.06f
    val headY = shoulderY + height * 0.06f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(apexHipX, apexHipY), gold, 7f)
    drawLimb(Offset(apexHipX, apexHipY), Offset(feetX, feetY), lightGold, 7f)

    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(apexHipX, apexHipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawPullHangAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val barY = height * 0.16f
    val leftHandX = width * 0.40f
    val rightHandX = width * 0.60f

    // Draw Pullup Bar
    drawLine(
        color = lightGold,
        start = Offset(width * 0.20f, barY),
        end = Offset(width * 0.80f, barY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Pull up: body elevates toward bar
    val bottomShoulderY = barY + height * 0.40f
    val topShoulderY = barY + height * 0.12f
    val shoulderY = bottomShoulderY - (bottomShoulderY - topShoulderY) * cycle
    val centerX = width * 0.50f

    val hipY = shoulderY + height * 0.25f
    val feetY = hipY + height * 0.26f

    val headX = centerX
    val headY = shoulderY - height * 0.08f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(centerX, shoulderY), gold, 6f)
    drawLimb(Offset(centerX, shoulderY), Offset(centerX, hipY), gold, 7f)
    drawLimb(Offset(centerX, hipY), Offset(centerX, feetY), lightGold, 7f)

    // Left Arm
    val leftElbowX = leftHandX - width * 0.06f * cycle
    val leftElbowY = (barY + shoulderY) / 2f
    drawLimb(Offset(centerX, shoulderY), Offset(leftElbowX, leftElbowY), gold, 5f)
    drawLimb(Offset(leftElbowX, leftElbowY), Offset(leftHandX, barY), lightGold, 5f)

    // Right Arm
    val rightElbowX = rightHandX + width * 0.06f * cycle
    val rightElbowY = (barY + shoulderY) / 2f
    drawLimb(Offset(centerX, shoulderY), Offset(rightElbowX, rightElbowY), gold, 5f)
    drawLimb(Offset(rightElbowX, rightElbowY), Offset(rightHandX, barY), lightGold, 5f)

    drawJoint(Offset(centerX, shoulderY), gold, jointWhite)
    drawJoint(Offset(centerX, hipY), gold, jointWhite)
    drawJoint(Offset(centerX, feetY), gold, jointWhite)
    drawJoint(Offset(leftHandX, barY), gold, jointWhite)
    drawJoint(Offset(rightHandX, barY), gold, jointWhite)
}

private fun DrawScope.drawVUpAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val hipX = width * 0.50f
    val hipY = groundY - height * 0.05f

    // Flat vs Folding V
    val shoulderX = hipX - width * 0.28f + width * 0.12f * cycle
    val shoulderY = groundY - height * 0.06f - height * 0.40f * cycle

    val feetX = hipX + width * 0.28f - width * 0.12f * cycle
    val feetY = groundY - height * 0.06f - height * 0.44f * cycle

    val handX = feetX - width * 0.02f
    val handY = feetY

    val headX = shoulderX - width * 0.06f
    val headY = shoulderY - height * 0.06f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 7f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

private fun DrawScope.drawMountainClimberAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val handX = width * 0.32f
    val handY = groundY
    val shoulderX = handX + 8.dp.toPx()
    val shoulderY = groundY - height * 0.36f
    val hipX = width * 0.60f
    val hipY = groundY - height * 0.30f
    val headX = shoulderX - width * 0.10f
    val headY = shoulderY - height * 0.04f

    val backFootX = width * 0.80f
    val backFootY = groundY

    // Alternating knee drive
    val legPhase = sin(t * 2 * PI.toFloat())
    val drivenKneeX = hipX - width * 0.15f * (legPhase.coerceAtLeast(0f))
    val drivenKneeY = groundY - height * 0.16f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Back leg extended
    drawLimb(Offset(hipX, hipY), Offset(backFootX, backFootY), lightGold, 6f)

    // Driven leg
    drawLimb(Offset(hipX, hipY), Offset(drivenKneeX, drivenKneeY), gold, 6f)
    drawLimb(Offset(drivenKneeX, drivenKneeY), Offset(drivenKneeX + width * 0.04f, groundY), lightGold, 5f)

    // Arms
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(drivenKneeX, drivenKneeY), gold, jointWhite)
}

private fun DrawScope.drawBurpeeAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    when {
        t < 0.25f -> {
            // Drop to squat
            val cycle = t / 0.25f
            drawSquatAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        t < 0.50f -> {
            // Push-up on floor
            val cycle = (t - 0.25f) / 0.25f
            drawPushUpAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        t < 0.75f -> {
            // Snap feet in
            val cycle = 1f - ((t - 0.50f) / 0.25f)
            drawSquatAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        else -> {
            // Explosive Jump
            val jumpCycle = sin(((t - 0.75f) / 0.25f) * PI.toFloat())
            val centerX = width * 0.50f
            val jumpOffsetY = height * 0.18f * jumpCycle
            val footY = groundY - jumpOffsetY
            val hipY = footY - height * 0.48f
            val shoulderY = hipY - height * 0.26f
            val headY = shoulderY - height * 0.09f

            // Overhead hands
            val leftHandX = centerX - width * 0.08f
            val rightHandX = centerX + width * 0.08f
            val handY = headY - height * 0.10f

            drawStickmanHead(Offset(centerX, headY), 13.dp.toPx(), gold)
            drawLimb(Offset(centerX, headY), Offset(centerX, shoulderY), gold, 6f)
            drawLimb(Offset(centerX, shoulderY), Offset(centerX, hipY), gold, 7f)
            drawLimb(Offset(centerX, hipY), Offset(centerX, footY), lightGold, 7f)

            drawLimb(Offset(centerX, shoulderY), Offset(leftHandX, handY), gold, 5f)
            drawLimb(Offset(centerX, shoulderY), Offset(rightHandX, handY), gold, 5f)

            drawJoint(Offset(centerX, shoulderY), gold, jointWhite)
            drawJoint(Offset(centerX, hipY), gold, jointWhite)
            drawJoint(Offset(centerX, footY), gold, jointWhite)
            drawJoint(Offset(leftHandX, handY), gold, jointWhite)
            drawJoint(Offset(rightHandX, handY), gold, jointWhite)
        }
    }
}

private fun DrawScope.drawCalfRaiseAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.50f
    val heelRise = height * 0.08f * cycle
    val ankleY = groundY - heelRise
    val kneeY = groundY - height * 0.36f - heelRise
    val hipY = groundY - height * 0.62f - heelRise
    val shoulderY = groundY - height * 0.84f - heelRise
    val headY = shoulderY - height * 0.09f

    drawStickmanHead(Offset(centerX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(centerX, headY), Offset(centerX, shoulderY), gold, 6f)
    drawLimb(Offset(centerX, shoulderY), Offset(centerX, hipY), gold, 7f)
    drawLimb(Offset(centerX, hipY), Offset(centerX, kneeY), lightGold, 7f)
    drawLimb(Offset(centerX, kneeY), Offset(centerX, ankleY), lightGold, 6f)
    drawLimb(Offset(centerX, ankleY), Offset(centerX + width * 0.06f, groundY), lightGold, 5f)

    // Hands on hips
    val elbowX = centerX - width * 0.08f
    val elbowY = hipY - height * 0.06f
    drawLimb(Offset(centerX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(centerX, hipY), lightGold, 5f)

    drawJoint(Offset(centerX, shoulderY), gold, jointWhite)
    drawJoint(Offset(centerX, hipY), gold, jointWhite)
    drawJoint(Offset(centerX, kneeY), gold, jointWhite)
    drawJoint(Offset(centerX, ankleY), gold, jointWhite)
}

private fun DrawScope.drawGoodMorningAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.50f
    val feetX = centerX
    val feetY = groundY
    val kneeX = centerX - width * 0.03f * cycle
    val kneeY = groundY - height * 0.32f
    val hipX = centerX - width * 0.12f * cycle
    val hipY = groundY - height * 0.55f

    // Torso hinges forward 90 degrees
    val shoulderX = hipX + width * 0.28f * (1f - cycle) + width * 0.24f * cycle
    val shoulderY = hipY - height * 0.30f * (1f - cycle)
    val headX = shoulderX + width * 0.06f
    val headY = shoulderY - height * 0.04f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(feetX, feetY), lightGold, 6f)

    // Hands behind head
    drawLimb(Offset(shoulderX, shoulderY), Offset(headX, headY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawNordicCurlAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val kneeX = width * 0.40f
    val kneeY = groundY
    val feetX = width * 0.22f
    val feetY = groundY

    // Ankle lock bar
    drawLine(
        color = lightGold,
        start = Offset(feetX - 10.dp.toPx(), groundY - 8.dp.toPx()),
        end = Offset(feetX + 10.dp.toPx(), groundY - 8.dp.toPx()),
        strokeWidth = 3.dp.toPx()
    )

    // Torso leans forward from knees
    val topHipX = kneeX
    val topHipY = groundY - height * 0.35f
    val bottomHipX = kneeX + width * 0.28f
    val bottomHipY = groundY - height * 0.12f

    val hipX = topHipX + (bottomHipX - topHipX) * cycle
    val hipY = topHipY + (bottomHipY - topHipY) * cycle

    val shoulderX = hipX + width * 0.22f * cycle
    val shoulderY = hipY - height * 0.28f * (1f - cycle)
    val headX = shoulderX + width * 0.08f
    val headY = shoulderY - height * 0.04f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(feetX, feetY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

private fun DrawScope.drawScapularCirclesAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.50f
    val shoulderY = groundY - height * 0.70f
    val hipY = groundY - height * 0.44f
    val feetY = groundY
    val headY = shoulderY - height * 0.09f

    // Arms extended with circular motion
    val angle = t * 2 * PI.toFloat()
    val circleRadius = width * 0.12f
    val handX = centerX + circleRadius * cos(angle)
    val handY = shoulderY + circleRadius * sin(angle)

    drawStickmanHead(Offset(centerX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(centerX, headY), Offset(centerX, shoulderY), gold, 6f)
    drawLimb(Offset(centerX, shoulderY), Offset(centerX, hipY), gold, 7f)
    drawLimb(Offset(centerX, hipY), Offset(centerX, feetY), lightGold, 7f)

    drawLimb(Offset(centerX, shoulderY), Offset(handX, handY), gold, 5f)

    drawJoint(Offset(centerX, shoulderY), gold, jointWhite)
    drawJoint(Offset(centerX, hipY), gold, jointWhite)
    drawJoint(Offset(centerX, feetY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

// -------------------------------------------------------------
// HELPER DRAW FUNCTIONS
// -------------------------------------------------------------

private fun DrawScope.drawStickmanHead(center: Offset, radius: Float, color: Color) {
    // Halo glow outer
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = radius * 1.35f,
        center = center
    )
    // Head circle outline
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )
    // Visor eye line
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(center.x - radius * 0.5f, center.y),
        end = Offset(center.x + radius * 0.5f, center.y),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLimb(start: Offset, end: Offset, color: Color, strokeDp: Float) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeDp.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawJoint(center: Offset, glowColor: Color, centerColor: Color) {
    // Outer joint aura
    drawCircle(
        color = glowColor.copy(alpha = 0.4f),
        radius = 5.dp.toPx(),
        center = center
    )
    // Solid joint dot
    drawCircle(
        color = centerColor,
        radius = 2.5.dp.toPx(),
        center = center
    )
}
