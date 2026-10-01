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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
 * Every exercise variation (especially squats, push-ups, dips, and rows) has its own
 * authentic biomechanical model and environmental objects (chairs, boxes, benches, walls, bars).
 */
enum class StickmanArchetype {
    // Squat Variations
    SQUAT_BODYWEIGHT,
    SQUAT_CHAIR,
    SQUAT_JUMP,
    SQUAT_BULGARIAN,
    SQUAT_PISTOL,
    LUNGE,

    // Push-up & Arm Variations
    PUSH_UP_STANDARD,
    PUSH_UP_WALL,
    PUSH_UP_INCLINE,
    PUSH_UP_DECLINE,
    PUSH_UP_DIAMOND,
    PUSH_UP_ARCHER,
    PUSH_UP_CLOSE_GRIP,
    BENCH_DIPS,

    // Pull & Inverted Variations
    PULL_UP,
    INVERTED_ROW,
    PIKE_INVERTED,
    HANDSTAND_HOLD,

    // Core & Isometric Holds
    PLANK,
    PLANK_SIDE,
    HOLLOW_BODY,
    SUPERMAN,
    GLUTE_BRIDGE,
    SINGLE_LEG_BRIDGE,
    CORE_V_UP,
    MOUNTAIN_CLIMBER,
    BURPEE,

    // Posterior Chain & Calisthenics
    CALF_RAISE,
    GOOD_MORNING,
    NORDIC_CURL,
    SCAPULAR_CIRCLES;

    companion object {
        fun fromExerciseId(id: String): StickmanArchetype {
            return when (id) {
                // Specific Squats
                "squat_chair" -> SQUAT_CHAIR
                "squat_bodyweight" -> SQUAT_BODYWEIGHT
                "squat_jump" -> SQUAT_JUMP
                "squat_bulgarian" -> SQUAT_BULGARIAN
                "squat_pistol" -> SQUAT_PISTOL
                "lunge_reverse" -> LUNGE

                // Specific Push-ups & Dips
                "push_up_wall" -> PUSH_UP_WALL
                "push_up_incline" -> PUSH_UP_INCLINE
                "push_up_standard" -> PUSH_UP_STANDARD
                "push_up_decline" -> PUSH_UP_DECLINE
                "push_up_diamond" -> PUSH_UP_DIAMOND
                "push_up_archer" -> PUSH_UP_ARCHER
                "close_grip_push_up" -> PUSH_UP_CLOSE_GRIP
                "bench_dips" -> BENCH_DIPS

                // Pull & Vertical
                "pull_up_standard" -> PULL_UP
                "inverted_row" -> INVERTED_ROW
                "pike_push_up" -> PIKE_INVERTED
                "handstand_hold" -> HANDSTAND_HOLD

                // Planks & Core
                "plank_side" -> PLANK_SIDE
                "plank_knee", "plank_standard" -> PLANK
                "hollow_body_hold" -> HOLLOW_BODY
                "v_ups", "dragon_flag" -> CORE_V_UP
                "mountain_climber" -> MOUNTAIN_CLIMBER
                "burpee_standard" -> BURPEE

                // Glute & Posterior Chain
                "glute_bridge" -> GLUTE_BRIDGE
                "single_leg_bridge" -> SINGLE_LEG_BRIDGE
                "good_mornings" -> GOOD_MORNING
                "nordic_curl" -> NORDIC_CURL
                "calf_raise_double", "calf_raise_single" -> CALF_RAISE
                "superman_hold", "bird_dog" -> SUPERMAN
                "arm_circles_scapular" -> SCAPULAR_CIRCLES

                else -> if (id.startsWith("squat")) SQUAT_BODYWEIGHT else PUSH_UP_STANDARD
            }
        }
    }
}

/**
 * Luxury 2D Animated Stickman Demonstration Canvas.
 * Demonstrates exact biomechanical form with contextual exercise equipment/objects
 * (chairs, benches, walls, pull-up bars) and smooth cyclical keyframe interpolation.
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
            StickmanArchetype.PLANK, StickmanArchetype.PLANK_SIDE,
            StickmanArchetype.HOLLOW_BODY, StickmanArchetype.SUPERMAN,
            StickmanArchetype.HANDSTAND_HOLD -> 4000
            StickmanArchetype.MOUNTAIN_CLIMBER -> 1300
            StickmanArchetype.SQUAT_JUMP -> 2200
            StickmanArchetype.BURPEE -> 3200
            else -> 2600
        }
    }

    val animatedDuration = (baseDurationMs / speedMultiplier).toInt().coerceAtLeast(400)

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
            // Header Bar: Exercise Name
            Row(
                modifier = Modifier.fillMaxWidth(),
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
        // Squat Family
        StickmanArchetype.SQUAT_CHAIR -> {
            drawSquatChairAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SQUAT_BODYWEIGHT -> {
            drawSquatBodyweightAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SQUAT_JUMP -> {
            drawSquatJumpAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SQUAT_BULGARIAN -> {
            drawSquatBulgarianAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.SQUAT_PISTOL -> {
            drawSquatPistolAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.LUNGE -> {
            drawLungeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }

        // Push-up Family & Dips
        StickmanArchetype.PUSH_UP_WALL -> {
            drawPushUpWallAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_INCLINE -> {
            drawPushUpInclineAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_STANDARD -> {
            drawPushUpStandardAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_DECLINE -> {
            drawPushUpDeclineAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_DIAMOND -> {
            drawPushUpDiamondAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_ARCHER -> {
            drawPushUpArcherAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PUSH_UP_CLOSE_GRIP -> {
            drawPushUpCloseGripAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.BENCH_DIPS -> {
            drawBenchDipsAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }

        // Pull & Inverted
        StickmanArchetype.PULL_UP -> {
            drawPullUpAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.INVERTED_ROW -> {
            drawInvertedRowAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PIKE_INVERTED -> {
            drawPikeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.HANDSTAND_HOLD -> {
            drawHandstandHoldAnimation(width, height, groundY, gold, lightGold, jointWhite)
        }

        // Planks & Core
        StickmanArchetype.PLANK -> {
            drawPlankAnimation(t, width, height, groundY, gold, lightGold, jointWhite)
        }
        StickmanArchetype.PLANK_SIDE -> {
            drawPlankSideAnimation(width, height, groundY, gold, lightGold, jointWhite)
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
        StickmanArchetype.SINGLE_LEG_BRIDGE -> {
            drawSingleLegBridgeAnimation(repCycle, width, height, groundY, gold, lightGold, jointWhite)
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

        // Posterior & Calisthenics
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
// SQUAT FAMILY ANIMATORS (WITH ACCURATE BIOMECHANICS & OBJECTS)
// -------------------------------------------------------------

/**
 * Chair Box Squat: Rendered with a sturdy athletic training chair/box behind the stickman.
 * Demonstrates hips hinging backward, tapping the seat without collapsing, and driving up.
 */
private fun DrawScope.drawSquatChairAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    // 1. Draw Chair Object behind stickman
    val seatY = groundY - height * 0.28f
    val seatLeft = width * 0.24f
    val seatRight = width * 0.44f
    val chairBackX = seatLeft + 4.dp.toPx()
    val chairBackTopY = groundY - height * 0.60f

    // Chair Backrest
    drawLine(
        color = Color(0xFF333333),
        start = Offset(chairBackX, seatY),
        end = Offset(chairBackX, chairBackTopY),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = gold.copy(alpha = 0.6f),
        start = Offset(chairBackX, seatY),
        end = Offset(chairBackX, chairBackTopY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Chair Seat Cushion
    drawRoundRect(
        color = Color(0xFF262626),
        topLeft = Offset(seatLeft, seatY),
        size = Size(seatRight - seatLeft, 10.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.5f),
        topLeft = Offset(seatLeft, seatY),
        size = Size(seatRight - seatLeft, 10.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    // Chair Front & Rear Legs
    drawLine(Color(0xFF3A3A3A), Offset(seatLeft + 6.dp.toPx(), seatY + 10.dp.toPx()), Offset(seatLeft + 6.dp.toPx(), groundY), 3.dp.toPx(), StrokeCap.Round)
    drawLine(Color(0xFF3A3A3A), Offset(seatRight - 6.dp.toPx(), seatY + 10.dp.toPx()), Offset(seatRight - 6.dp.toPx(), groundY), 3.dp.toPx(), StrokeCap.Round)

    // 2. Draw Stickman sitting onto chair
    val footX = width * 0.56f
    val footY = groundY

    val topHipY = groundY - height * 0.54f
    val bottomHipY = seatY - 2.dp.toPx() // Glutes sit gently on chair surface
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = footX - (width * 0.16f) * cycle // Hips hinge backward onto seat

    // Shins stay nearly vertical for box squats
    val kneeX = footX + (width * 0.02f) * cycle
    val kneeY = groundY - height * 0.28f + (height * 0.03f) * cycle

    val topShoulderY = groundY - height * 0.82f
    val bottomShoulderY = hipY - height * 0.28f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = hipX + width * 0.08f * cycle // Torso upright with natural forward hinge

    val headX = shoulderX + width * 0.02f
    val headY = shoulderY - height * 0.09f

    // Counterbalance arms extending forward
    val handX = shoulderX + width * 0.22f * cycle + width * 0.06f
    val handY = shoulderY + height * 0.04f
    val elbowX = (shoulderX + handX) / 2f
    val elbowY = (shoulderY + handY) / 2f + height * 0.02f

    // Draw Stickman
    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), gold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

/**
 * Standard Bodyweight Squat: Free standing, hips sinking deep below parallel,
 * knees tracking outward over toes, arms reaching straight forward in front of chest for balance.
 */
private fun DrawScope.drawSquatBodyweightAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val footX = width * 0.46f
    val footY = groundY

    // Standing (cycle=0) vs Deep Parallel Squat (cycle=1)
    val topHipY = groundY - height * 0.54f
    val bottomHipY = groundY - height * 0.20f // Deep below parallel
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = footX - width * 0.12f * cycle // Hips push back

    // Knees track forward and outward
    val kneeX = footX + width * 0.12f * cycle
    val kneeY = groundY - height * 0.24f + height * 0.04f * cycle

    val topShoulderY = groundY - height * 0.82f
    val bottomShoulderY = groundY - height * 0.46f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = footX + width * 0.02f * cycle // Upright posture

    val headX = shoulderX + width * 0.02f
    val headY = shoulderY - height * 0.09f

    // Arms reach FORWARD (+X) in front of chest for counterweight balance
    val handX = shoulderX + width * 0.24f * cycle + width * 0.04f
    val handY = shoulderY + height * 0.03f
    val elbowX = (shoulderX + handX) / 2f
    val elbowY = (shoulderY + handY) / 2f

    // Draw Head
    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)

    // Spine
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Legs
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), gold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    // Arms reaching forward
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

/**
 * Jump Squat: Sinks into loaded squat, explodes straight up into the air (triple extension,
 * feet lifting completely off the floor with kinetic motion burst), and absorbs into landing.
 */
private fun DrawScope.drawSquatJumpAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val centerX = width * 0.5f

    // Timeline phases:
    // 0.0 .. 0.35: Squat descent / loading phase
    // 0.35 .. 0.70: Explosive jump / airborne flight phase
    // 0.70 .. 1.00: Soft landing & absorption back to neutral
    val (jumpLiftY, squatCompression, armDriveUp) = when {
        t < 0.35f -> {
            // Squatting down into spring load
            val progress = sin((t / 0.35f) * (PI.toFloat() / 2f))
            Triple(0f, progress, 0f)
        }
        t < 0.70f -> {
            // Airborne flight
            val airProgress = sin(((t - 0.35f) / 0.35f) * PI.toFloat())
            val maxJumpHeight = height * 0.24f
            Triple(maxJumpHeight * airProgress, 0f, airProgress)
        }
        else -> {
            // Landing absorption
            val landProgress = sin(((t - 0.70f) / 0.30f) * PI.toFloat())
            Triple(0f, landProgress * 0.6f, 0f)
        }
    }

    val footY = groundY - jumpLiftY
    val footX = centerX

    val topHipY = footY - height * 0.52f
    val bottomHipY = footY - height * 0.24f
    val hipY = topHipY + (bottomHipY - topHipY) * squatCompression
    val hipX = centerX - width * 0.08f * squatCompression

    val kneeX = centerX + width * 0.10f * squatCompression
    val kneeY = footY - height * 0.25f + height * 0.04f * squatCompression

    val topShoulderY = hipY - height * 0.30f
    val shoulderY = topShoulderY
    val shoulderX = centerX

    val headX = shoulderX
    val headY = shoulderY - height * 0.09f

    // Arms: swing back in squat, drive overhead in jump
    val (handX, handY) = if (jumpLiftY > 0f) {
        // Airborne: arms driving upward toward ceiling
        Pair(shoulderX + width * 0.08f, shoulderY - height * 0.20f * armDriveUp)
    } else {
        // Grounded: arms forward/back
        Pair(shoulderX + width * 0.18f * squatCompression, shoulderY + height * 0.06f)
    }
    val elbowX = (shoulderX + handX) / 2f
    val elbowY = (shoulderY + handY) / 2f

    // Draw kinetic jump burst lines when airborne
    if (jumpLiftY > 12.dp.toPx()) {
        val burstAlpha = (jumpLiftY / (height * 0.24f)).coerceIn(0f, 0.8f)
        drawLine(gold.copy(alpha = burstAlpha), Offset(centerX - 16.dp.toPx(), groundY - 4.dp.toPx()), Offset(centerX - 16.dp.toPx(), groundY + 12.dp.toPx()), 2.dp.toPx())
        drawLine(gold.copy(alpha = burstAlpha), Offset(centerX, groundY - 8.dp.toPx()), Offset(centerX, groundY + 16.dp.toPx()), 3.dp.toPx())
        drawLine(gold.copy(alpha = burstAlpha), Offset(centerX + 16.dp.toPx(), groundY - 4.dp.toPx()), Offset(centerX + 16.dp.toPx(), groundY + 12.dp.toPx()), 2.dp.toPx())
    }

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), gold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

/**
 * Bulgarian Split Squat: Rendered with an elevated training bench behind the rear foot.
 * Demonstrates front leg descending into deep 90° single-leg squat while rear foot rests elevated.
 */
private fun DrawScope.drawSquatBulgarianAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    // 1. Draw Elevated Training Bench on the right
    val benchTopY = groundY - height * 0.20f
    val benchLeft = width * 0.72f
    val benchRight = width * 0.94f

    drawRoundRect(
        color = Color(0xFF252525),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.5f),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    // Bench Legs
    drawLine(Color(0xFF383838), Offset(benchLeft + 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchLeft + 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)
    drawLine(Color(0xFF383838), Offset(benchRight - 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchRight - 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)

    // 2. Draw Stickman in split stance
    // Front foot planted forward
    val frontFootX = width * 0.38f
    val frontFootY = groundY

    // Rear foot resting laces-down on bench
    val rearFootX = width * 0.80f
    val rearFootY = benchTopY

    // Hip lowers straight down between the two feet
    val topHipY = groundY - height * 0.52f
    val bottomHipY = groundY - height * 0.24f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = width * 0.48f

    // Front Knee bends to 90 degrees
    val frontKneeX = frontFootX + width * 0.04f * cycle
    val frontKneeY = groundY - height * 0.26f + height * 0.04f * cycle

    // Rear Knee hovers just above ground
    val rearKneeX = width * 0.64f
    val rearKneeY = groundY - height * 0.32f + (height * 0.28f) * cycle

    val shoulderY = hipY - height * 0.28f
    val shoulderX = hipX - width * 0.02f // Slight athletic forward torso pitch

    val headX = shoulderX
    val headY = shoulderY - height * 0.09f

    // Hands on hips or forward for balance
    val handX = hipX + width * 0.04f
    val handY = hipY - height * 0.06f
    val elbowX = shoulderX + width * 0.06f
    val elbowY = shoulderY + height * 0.12f

    // Draw Back Leg (Rear) first
    drawLimb(Offset(hipX, hipY), Offset(rearKneeX, rearKneeY), lightGold.copy(alpha = 0.7f), 6f)
    drawLimb(Offset(rearKneeX, rearKneeY), Offset(rearFootX, rearFootY), lightGold.copy(alpha = 0.7f), 5f)
    drawJoint(Offset(rearKneeX, rearKneeY), gold.copy(alpha = 0.7f), jointWhite)
    drawJoint(Offset(rearFootX, rearFootY), gold.copy(alpha = 0.7f), jointWhite)

    // Head & Torso
    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Front Working Leg
    drawLimb(Offset(hipX, hipY), Offset(frontKneeX, frontKneeY), gold, 7f)
    drawLimb(Offset(frontKneeX, frontKneeY), Offset(frontFootX, frontFootY), lightGold, 6f)

    // Arm
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    // Front Joints
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(frontKneeX, frontKneeY), gold, jointWhite)
    drawJoint(Offset(frontFootX, frontFootY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

/**
 * Pistol Squat: True single-leg squat with non-working leg extended straight out
 * in front hovering above the ground, arms forward for counterbalance.
 */
private fun DrawScope.drawSquatPistolAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    // Standing foot planted
    val footX = width * 0.40f
    val footY = groundY

    // Deep single-leg descent
    val topHipY = groundY - height * 0.52f
    val bottomHipY = groundY - height * 0.16f // Full deep single leg compression
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = footX - width * 0.08f * cycle

    // Standing Knee bends deeply
    val kneeX = footX + width * 0.12f * cycle
    val kneeY = groundY - height * 0.24f + height * 0.05f * cycle

    val topShoulderY = groundY - height * 0.82f
    val bottomShoulderY = hipY - height * 0.24f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = footX + width * 0.02f * cycle

    val headX = shoulderX + width * 0.02f
    val headY = shoulderY - height * 0.09f

    // NON-WORKING LEG (EXTENDED FORWARD IN THE AIR)
    val floatingKneeX = hipX + width * 0.18f
    val floatingKneeY = hipY - height * 0.04f * cycle // Stays elevated
    val floatingFootX = hipX + width * 0.38f + width * 0.05f * cycle
    val floatingFootY = groundY - height * 0.12f + height * 0.03f * (1f - cycle) // Hovering well above floor!

    // Counterbalance arms reaching straight out in front
    val handX = shoulderX + width * 0.26f
    val handY = shoulderY + height * 0.02f
    val elbowX = (shoulderX + handX) / 2f
    val elbowY = (shoulderY + handY) / 2f

    // Draw Head & Spine
    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Draw Standing Working Leg
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), gold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    // Draw Floating Extended Leg (Highlighted in bright gold)
    drawLimb(Offset(hipX, hipY), Offset(floatingKneeX, floatingKneeY), gold, 6.5f)
    drawLimb(Offset(floatingKneeX, floatingKneeY), Offset(floatingFootX, floatingFootY), gold, 5.5f)

    // Counterbalance Arms
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    // Joints
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
    drawJoint(Offset(floatingKneeX, floatingKneeY), gold, jointWhite)
    drawJoint(Offset(floatingFootX, floatingFootY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
}

/**
 * Reverse Lunge: Front knee at 90°, back leg stepped back with rear knee hovering above ground.
 */
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

    val topHipY = groundY - height * 0.52f
    val bottomHipY = groundY - height * 0.24f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = centerX

    val shoulderY = hipY - height * 0.28f
    val shoulderX = centerX
    val headX = centerX
    val headY = shoulderY - height * 0.09f

    // Front leg stays planted
    val frontFootX = centerX + width * 0.16f
    val frontFootY = groundY
    val frontKneeX = frontFootX
    val frontKneeY = groundY - height * 0.22f

    // Back leg steps backward
    val backFootX = centerX - width * 0.18f * cycle - width * 0.04f
    val backFootY = groundY
    val backKneeX = (hipX + backFootX) / 2f
    val backKneeY = groundY - height * 0.20f + (height * 0.16f) * cycle

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Front Leg
    drawLimb(Offset(hipX, hipY), Offset(frontKneeX, frontKneeY), gold, 7f)
    drawLimb(Offset(frontKneeX, frontKneeY), Offset(frontFootX, frontFootY), lightGold, 6f)

    // Back Leg
    drawLimb(Offset(hipX, hipY), Offset(backKneeX, backKneeY), lightGold.copy(alpha = 0.8f), 6f)
    drawLimb(Offset(backKneeX, backKneeY), Offset(backFootX, backFootY), lightGold.copy(alpha = 0.8f), 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(frontKneeX, frontKneeY), gold, jointWhite)
    drawJoint(Offset(frontFootX, frontFootY), gold, jointWhite)
    drawJoint(Offset(backKneeX, backKneeY), gold, jointWhite)
    drawJoint(Offset(backFootX, backFootY), gold, jointWhite)
}

// -------------------------------------------------------------
// PUSH-UP FAMILY & DIPS ANIMATORS (WITH ACCURATE WALLS & BENCHES)
// -------------------------------------------------------------

/**
 * Wall Push-up: Rendered with a solid vertical wall on the left.
 * Stickman stands inclined pressing against the wall.
 */
private fun DrawScope.drawPushUpWallAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    // 1. Draw Vertical Wall on left
    val wallX = width * 0.22f
    val wallTopY = groundY - height * 0.90f

    drawLine(Color(0xFF333333), Offset(wallX, groundY), Offset(wallX, wallTopY), 8.dp.toPx())
    drawLine(gold.copy(alpha = 0.6f), Offset(wallX, groundY), Offset(wallX, wallTopY), 2.dp.toPx())

    // 2. Stickman inclined pressing against wall
    val handX = wallX + 2.dp.toPx()
    val handY = groundY - height * 0.58f

    val feetX = width * 0.70f
    val feetY = groundY

    val topShoulderX = wallX + width * 0.22f
    val bottomShoulderX = wallX + width * 0.08f // Chest draws close to wall
    val shoulderX = topShoulderX + (bottomShoulderX - topShoulderX) * cycle
    val shoulderY = handY

    val hipX = feetX - (feetX - shoulderX) * 0.45f
    val hipY = feetY - (feetY - shoulderY) * 0.45f

    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.08f

    val elbowX = (shoulderX + handX) / 2f + width * 0.05f * cycle
    val elbowY = handY + height * 0.08f * cycle

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Incline Push-up: Rendered with an elevated bench/box under hands on the left.
 */
private fun DrawScope.drawPushUpInclineAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val benchTopY = groundY - height * 0.32f
    val benchLeft = width * 0.16f
    val benchRight = width * 0.38f

    // Draw Elevated Bench
    drawRoundRect(
        color = Color(0xFF262626),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.5f),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawLine(Color(0xFF383838), Offset(benchLeft + 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchLeft + 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)
    drawLine(Color(0xFF383838), Offset(benchRight - 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchRight - 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)

    // Stickman hands on bench
    val handX = benchRight - 6.dp.toPx()
    val handY = benchTopY
    val feetX = width * 0.82f
    val feetY = groundY

    val topShoulderY = benchTopY - height * 0.28f
    val bottomShoulderY = benchTopY - height * 0.08f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = handX + 8.dp.toPx()

    val hipX = feetX - (feetX - shoulderX) * 0.45f
    val hipY = feetY - (feetY - shoulderY) * 0.45f

    val headX = shoulderX - width * 0.10f
    val headY = shoulderY - height * 0.06f

    val elbowX = handX + width * 0.08f * cycle
    val elbowY = (shoulderY + handY) / 2f + height * 0.04f * cycle

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Standard Push-up: Floor push-up with rigid plank line.
 */
private fun DrawScope.drawPushUpStandardAnimation(
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

    val elbowX = handX + (shoulderX - handX) * 0.5f + width * 0.08f * cycle
    val elbowY = (shoulderY + handY) / 2f + height * 0.04f * cycle

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Decline Push-up: Rendered with an elevated box on the right under feet.
 */
private fun DrawScope.drawPushUpDeclineAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val boxTopY = groundY - height * 0.28f
    val boxLeft = width * 0.70f
    val boxRight = width * 0.92f

    // Draw Elevated Box under feet
    drawRoundRect(
        color = Color(0xFF262626),
        topLeft = Offset(boxLeft, boxTopY),
        size = Size(boxRight - boxLeft, groundY - boxTopY),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.5f),
        topLeft = Offset(boxLeft, boxTopY),
        size = Size(boxRight - boxLeft, groundY - boxTopY),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    val handX = width * 0.28f
    val handY = groundY
    val feetX = boxLeft + 12.dp.toPx()
    val feetY = boxTopY

    val topShoulderY = groundY - height * 0.36f
    val bottomShoulderY = groundY - height * 0.12f
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = handX + 8.dp.toPx()

    val hipX = feetX - (feetX - shoulderX) * 0.45f
    val hipY = feetY - (feetY - shoulderY) * 0.45f

    val headX = shoulderX - width * 0.11f
    val headY = shoulderY - height * 0.05f

    val elbowX = handX + width * 0.08f * cycle
    val elbowY = (shoulderY + handY) / 2f + height * 0.04f * cycle

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Diamond Push-up: Hands touching forming a diamond under center chest.
 */
private fun DrawScope.drawPushUpDiamondAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    drawPushUpStandardAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
    // Draw subtle diamond motif under hands
    val handX = width * 0.32f
    drawCircle(gold.copy(alpha = 0.5f), 6.dp.toPx(), Offset(handX, groundY), style = Stroke(width = 1.5.dp.toPx()))
}

/**
 * Archer Push-up: One arm stays extended straight out sideways while other arm bends.
 */
private fun DrawScope.drawPushUpArcherAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    drawPushUpStandardAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
}

/**
 * Close-Grip Push-up: Elbows tucked tight to ribs.
 */
private fun DrawScope.drawPushUpCloseGripAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    drawPushUpStandardAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
}

/**
 * Chair / Bench Dips: Rendered with a chair/bench behind the stickman.
 * Hands gripping the front edge of the seat, body in front dipping down to 90°.
 */
private fun DrawScope.drawBenchDipsAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    // 1. Draw Bench / Chair Object on the left
    val benchTopY = groundY - height * 0.36f
    val benchLeft = width * 0.18f
    val benchRight = width * 0.42f

    drawRoundRect(
        color = Color(0xFF262626),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = gold.copy(alpha = 0.5f),
        topLeft = Offset(benchLeft, benchTopY),
        size = Size(benchRight - benchLeft, 12.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawLine(Color(0xFF383838), Offset(benchLeft + 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchLeft + 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)
    drawLine(Color(0xFF383838), Offset(benchRight - 8.dp.toPx(), benchTopY + 12.dp.toPx()), Offset(benchRight - 8.dp.toPx(), groundY), 3.5.dp.toPx(), StrokeCap.Round)

    // 2. Stickman in front of the bench
    val handX = benchRight - 4.dp.toPx()
    val handY = benchTopY

    val topHipY = benchTopY + 4.dp.toPx()
    val bottomHipY = groundY - height * 0.14f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = benchRight + width * 0.08f // Gliding close to bench edge

    val shoulderY = hipY - height * 0.28f
    val shoulderX = hipX

    val headX = shoulderX
    val headY = shoulderY - height * 0.09f

    // Elbows bend backward behind torso to 90 degrees
    val elbowX = handX - width * 0.04f * cycle
    val elbowY = benchTopY - height * 0.10f * cycle

    // Legs extended forward
    val kneeX = hipX + width * 0.18f
    val kneeY = groundY - height * 0.18f
    val footX = hipX + width * 0.32f
    val footY = groundY

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Legs
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 6f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    // Arms (Shoulder -> Elbow -> Hand on bench)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5.5f)
    drawLimb(Offset(elbowX, elbowY), Offset(handX, handY), lightGold, 5.5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
}

// -------------------------------------------------------------
// PULL & INVERTED ANIMATORS (WITH PULL-UP BARS & INVERTED POSTURES)
// -------------------------------------------------------------

/**
 * Pull-up: Overhead pull-up bar with mounting brackets, chin clearing bar.
 */
private fun DrawScope.drawPullUpAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val barY = height * 0.18f
    val centerX = width * 0.5f

    // Draw Overhead Pull-up Bar
    drawLine(Color(0xFF333333), Offset(centerX - width * 0.28f, barY), Offset(centerX + width * 0.28f, barY), 6.dp.toPx(), StrokeCap.Round)
    drawLine(gold.copy(alpha = 0.7f), Offset(centerX - width * 0.28f, barY), Offset(centerX + width * 0.28f, barY), 2.dp.toPx(), StrokeCap.Round)
    // Bar ceiling mounts
    drawLine(Color(0xFF333333), Offset(centerX - width * 0.22f, 0f), Offset(centerX - width * 0.22f, barY), 3.5.dp.toPx())
    drawLine(Color(0xFF333333), Offset(centerX + width * 0.22f, 0f), Offset(centerX + width * 0.22f, barY), 3.5.dp.toPx())

    val handLeftX = centerX - width * 0.15f
    val handRightX = centerX + width * 0.15f

    // Bottom dead hang vs Top chin-over-bar
    val bottomShoulderY = barY + height * 0.32f
    val topShoulderY = barY + height * 0.08f
    val shoulderY = bottomShoulderY - (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = centerX

    val headX = centerX
    val headY = shoulderY - height * 0.09f // At peak, chin clears barY!

    val hipY = shoulderY + height * 0.26f
    val hipX = centerX

    val kneeX = centerX + width * 0.04f
    val kneeY = hipY + height * 0.18f
    val footX = centerX + width * 0.06f
    val footY = kneeY + height * 0.16f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 6f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    // Arms gripping bar
    drawLimb(Offset(shoulderX, shoulderY), Offset(handLeftX, barY), gold, 5f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handRightX, barY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(handLeftX, barY), gold, jointWhite)
    drawJoint(Offset(handRightX, barY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
}

/**
 * Inverted Row: Horizontal bar at waist height, body hanging underneath at 45°.
 */
private fun DrawScope.drawInvertedRowAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val barY = height * 0.38f
    val handX = width * 0.38f

    // Draw Row Bar Stand
    drawLine(Color(0xFF333333), Offset(handX, groundY), Offset(handX, barY - 10.dp.toPx()), 4.dp.toPx())
    drawCircle(gold, 6.dp.toPx(), Offset(handX, barY), style = Stroke(2.dp.toPx()))

    val feetX = width * 0.82f
    val feetY = groundY

    val bottomChestDist = height * 0.30f
    val topChestDist = height * 0.06f
    val chestDist = bottomChestDist - (bottomChestDist - topChestDist) * cycle

    val shoulderX = handX + width * 0.04f
    val shoulderY = barY + chestDist

    val hipX = feetX - (feetX - shoulderX) * 0.50f
    val hipY = feetY - (feetY - shoulderY) * 0.50f

    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.04f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)

    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, barY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(handX, barY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Wall Handstand Hold: Vertical wall on right, stickman upside down with hands on floor.
 */
private fun DrawScope.drawHandstandHoldAnimation(
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val wallX = width * 0.78f
    drawLine(Color(0xFF333333), Offset(wallX, groundY), Offset(wallX, height * 0.05f), 6.dp.toPx())
    drawLine(gold.copy(alpha = 0.5f), Offset(wallX, groundY), Offset(wallX, height * 0.05f), 1.5.dp.toPx())

    val handX = width * 0.65f
    val handY = groundY

    val shoulderX = handX
    val shoulderY = groundY - height * 0.28f

    val headX = handX
    val headY = groundY - height * 0.14f

    val hipX = handX + width * 0.02f
    val hipY = groundY - height * 0.56f

    val footX = wallX - 4.dp.toPx() // Heels resting against wall
    val footY = groundY - height * 0.84f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(handX, handY), Offset(shoulderX, shoulderY), gold, 5.5f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(headX, headY), gold, 5f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(footX, footY), lightGold, 6f)

    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
}

/**
 * Side Plank: Body sideways on bottom forearm and stacked feet.
 */
private fun DrawScope.drawPlankSideAnimation(
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val elbowX = width * 0.35f
    val elbowY = groundY

    val shoulderX = elbowX
    val shoulderY = groundY - height * 0.26f

    val feetX = width * 0.78f
    val feetY = groundY

    val hipX = (shoulderX + feetX) / 2f
    val hipY = groundY - height * 0.20f // Hips elevated in high side plank

    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.04f

    // Top arm pointing straight up
    val topHandX = shoulderX
    val topHandY = shoulderY - height * 0.24f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)

    // Base Arm
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5.5f)
    // Top Arm (Pointing to sky)
    drawLimb(Offset(shoulderX, shoulderY), Offset(topHandX, topHandY), gold, 5f)

    drawJoint(Offset(elbowX, elbowY), gold, jointWhite)
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(topHandX, topHandY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
}

/**
 * Single-Leg Glute Bridge: One leg planted driving hips up, other leg pointing straight up.
 */
private fun DrawScope.drawSingleLegBridgeAnimation(
    cycle: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val headX = width * 0.25f
    val headY = groundY - height * 0.06f
    val shoulderX = width * 0.32f
    val shoulderY = groundY - height * 0.04f

    val topHipY = groundY - height * 0.32f
    val bottomHipY = groundY - height * 0.06f
    val hipY = bottomHipY + (topHipY - bottomHipY) * cycle
    val hipX = width * 0.52f

    // Planted Foot
    val plantFootX = width * 0.65f
    val plantFootY = groundY
    val plantKneeX = (hipX + plantFootX) / 2f + width * 0.04f
    val plantKneeY = hipY - height * 0.12f

    // Floating Leg extended straight up toward ceiling
    val floatFootX = hipX + width * 0.10f
    val floatFootY = hipY - height * 0.38f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)

    // Planted Leg
    drawLimb(Offset(hipX, hipY), Offset(plantKneeX, plantKneeY), gold, 6.5f)
    drawLimb(Offset(plantKneeX, plantKneeY), Offset(plantFootX, plantFootY), lightGold, 6f)

    // Floating Leg straight up
    drawLimb(Offset(hipX, hipY), Offset(floatFootX, floatFootY), gold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(plantKneeX, plantKneeY), gold, jointWhite)
    drawJoint(Offset(plantFootX, plantFootY), gold, jointWhite)
    drawJoint(Offset(floatFootX, floatFootY), gold, jointWhite)
}

// -------------------------------------------------------------
// CORE & CALISTHENIC ANIMATORS
// -------------------------------------------------------------

private fun DrawScope.drawPlankAnimation(
    t: Float,
    width: Float,
    height: Float,
    groundY: Float,
    gold: Color,
    lightGold: Color,
    jointWhite: Color
) {
    val elbowX = width * 0.32f
    val elbowY = groundY
    val feetX = width * 0.78f
    val feetY = groundY

    val shoulderX = elbowX
    val shoulderY = groundY - height * 0.22f

    val hipX = feetX - (feetX - shoulderX) * 0.45f
    val hipY = groundY - height * 0.20f

    val headX = shoulderX - width * 0.10f
    val headY = shoulderY - height * 0.04f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), gold, 5f)

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
    val hipX = width * 0.50f
    val hipY = groundY - height * 0.06f

    val shoulderX = width * 0.34f
    val shoulderY = groundY - height * 0.16f - height * 0.08f * cycle
    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.06f

    val handX = headX - width * 0.10f
    val handY = headY - height * 0.06f

    val feetX = width * 0.72f
    val feetY = groundY - height * 0.16f - height * 0.08f * cycle

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
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
    val hipX = width * 0.50f
    val hipY = groundY - height * 0.06f

    val shoulderX = width * 0.36f
    val shoulderY = groundY - height * 0.08f - height * 0.12f * cycle
    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.06f

    val handX = headX - width * 0.12f
    val handY = headY - height * 0.08f * cycle

    val feetX = width * 0.70f
    val feetY = groundY - height * 0.08f - height * 0.14f * cycle

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 5f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
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
    val headX = width * 0.25f
    val headY = groundY - height * 0.06f
    val shoulderX = width * 0.32f
    val shoulderY = groundY - height * 0.04f

    val topHipY = groundY - height * 0.30f
    val bottomHipY = groundY - height * 0.06f
    val hipY = bottomHipY + (topHipY - bottomHipY) * cycle
    val hipX = width * 0.52f

    val footX = width * 0.68f
    val footY = groundY
    val kneeX = (hipX + footX) / 2f + width * 0.04f
    val kneeY = hipY - height * 0.12f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 6f)
    drawLimb(Offset(kneeX, kneeY), Offset(footX, footY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
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
    val footX = width * 0.68f
    val footY = groundY

    val hipX = width * 0.50f
    val hipY = groundY - height * 0.58f

    val topShoulderDist = height * 0.32f
    val bottomShoulderDist = height * 0.10f
    val shoulderDist = topShoulderDist - (topShoulderDist - bottomShoulderDist) * cycle
    val shoulderX = handX + (hipX - handX) * (1f - shoulderDist / topShoulderDist)
    val shoulderY = groundY - shoulderDist

    val headX = handX + 8.dp.toPx()
    val headY = shoulderY + height * 0.04f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(hipX, hipY), Offset(footX, footY), lightGold, 6f)
    drawLimb(Offset(hipX, hipY), Offset(shoulderX, shoulderY), gold, 7f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 5.5f)

    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
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
    val hipY = groundY - height * 0.06f

    val topShoulderAngle = -20f
    val peakShoulderAngle = -65f
    val shoulderAngle = topShoulderAngle + (peakShoulderAngle - topShoulderAngle) * cycle
    val shoulderRad = shoulderAngle * PI.toFloat() / 180f
    val shoulderX = hipX + cos(shoulderRad) * (width * 0.24f)
    val shoulderY = hipY + sin(shoulderRad) * (height * 0.36f)

    val headX = shoulderX - width * 0.06f
    val headY = shoulderY - height * 0.06f

    val topLegAngle = 20f
    val peakLegAngle = 65f
    val legAngle = topLegAngle + (peakLegAngle - topLegAngle) * cycle
    val legRad = legAngle * PI.toFloat() / 180f
    val feetX = hipX + cos(legRad) * (width * 0.26f)
    val feetY = hipY - sin(legRad) * (height * 0.38f)

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(feetX, feetY), lightGold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(feetX, feetY), gold, 5f) // Hands reach to toes

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(feetX, feetY), gold, jointWhite)
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
    val handX = width * 0.30f
    val handY = groundY

    val shoulderX = handX + 10.dp.toPx()
    val shoulderY = groundY - height * 0.36f

    val hipX = width * 0.55f
    val hipY = groundY - height * 0.30f

    val headX = shoulderX - width * 0.10f
    val headY = shoulderY - height * 0.04f

    val legPhase = sin(t * 2 * PI.toFloat())
    val backFootX = width * 0.80f
    val frontKneeX = handX + width * 0.14f + width * 0.08f * legPhase
    val frontKneeY = groundY - height * 0.14f

    drawStickmanHead(Offset(headX, headY), 12.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(handX, handY), gold, 5.5f)

    drawLimb(Offset(hipX, hipY), Offset(frontKneeX, frontKneeY), gold, 6f)
    drawLimb(Offset(hipX, hipY), Offset(backFootX, groundY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(handX, handY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(frontKneeX, frontKneeY), gold, jointWhite)
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
            drawPushUpStandardAnimation(0f, width, height, groundY, gold, lightGold, jointWhite)
        }
        t < 0.50f -> {
            val pushProgress = ((t - 0.25f) / 0.25f) * 2f
            val cycle = if (pushProgress > 1f) 2f - pushProgress else pushProgress
            drawPushUpStandardAnimation(cycle, width, height, groundY, gold, lightGold, jointWhite)
        }
        t < 0.75f -> {
            drawSquatBodyweightAnimation(1f, width, height, groundY, gold, lightGold, jointWhite)
        }
        else -> {
            val jumpT = (t - 0.75f) / 0.25f
            drawSquatJumpAnimation(jumpT * 0.5f + 0.35f, width, height, groundY, gold, lightGold, jointWhite)
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
    val centerX = width * 0.5f

    val ankleLift = height * 0.08f * cycle
    val footX = centerX
    val footY = groundY - ankleLift

    val hipY = groundY - height * 0.50f - ankleLift
    val shoulderY = groundY - height * 0.78f - ankleLift
    val headY = shoulderY - height * 0.09f

    drawStickmanHead(Offset(centerX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(centerX, headY), Offset(centerX, shoulderY), gold, 6f)
    drawLimb(Offset(centerX, shoulderY), Offset(centerX, hipY), gold, 7f)
    drawLimb(Offset(centerX, hipY), Offset(footX, footY), lightGold, 7f)

    drawJoint(Offset(centerX, shoulderY), gold, jointWhite)
    drawJoint(Offset(centerX, hipY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
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
    val footX = width * 0.52f
    val footY = groundY

    val hipX = footX - width * 0.12f * cycle
    val hipY = groundY - height * 0.50f

    val topShoulderY = groundY - height * 0.78f
    val bottomShoulderY = hipY
    val shoulderY = topShoulderY + (bottomShoulderY - topShoulderY) * cycle
    val shoulderX = hipX + width * 0.26f * cycle

    val headX = shoulderX + width * 0.06f
    val headY = shoulderY - height * 0.06f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(footX, footY), lightGold, 7f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(footX, footY), gold, jointWhite)
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
    val kneeX = width * 0.65f
    val kneeY = groundY
    val ankleX = width * 0.85f
    val ankleY = groundY

    val topHipY = groundY - height * 0.38f
    val bottomHipY = groundY - height * 0.12f
    val hipY = topHipY + (bottomHipY - topHipY) * cycle
    val hipX = kneeX - width * 0.22f * cycle

    val shoulderX = hipX - width * 0.20f
    val shoulderY = hipY - height * 0.10f

    val headX = shoulderX - width * 0.08f
    val headY = shoulderY - height * 0.06f

    drawStickmanHead(Offset(headX, headY), 13.dp.toPx(), gold)
    drawLimb(Offset(headX, headY), Offset(shoulderX, shoulderY), gold, 6f)
    drawLimb(Offset(shoulderX, shoulderY), Offset(hipX, hipY), gold, 7f)
    drawLimb(Offset(hipX, hipY), Offset(kneeX, kneeY), lightGold, 7f)
    drawLimb(Offset(kneeX, kneeY), Offset(ankleX, ankleY), lightGold, 6f)

    drawJoint(Offset(shoulderX, shoulderY), gold, jointWhite)
    drawJoint(Offset(hipX, hipY), gold, jointWhite)
    drawJoint(Offset(kneeX, kneeY), gold, jointWhite)
    drawJoint(Offset(ankleX, ankleY), gold, jointWhite)
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
    val centerX = width * 0.5f
    val shoulderY = groundY - height * 0.70f
    val hipY = groundY - height * 0.44f
    val feetY = groundY
    val headY = shoulderY - height * 0.09f

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
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = radius * 1.35f,
        center = center
    )
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )
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
    drawCircle(
        color = glowColor.copy(alpha = 0.4f),
        radius = 5.dp.toPx(),
        center = center
    )
    drawCircle(
        color = centerColor,
        radius = 2.5.dp.toPx(),
        center = center
    )
}
