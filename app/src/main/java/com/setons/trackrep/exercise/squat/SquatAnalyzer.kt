package com.setons.trackrep.exercise.squat

import com.setons.trackrep.pose.TrackedPose
import com.setons.trackrep.review.FormFlaw
import kotlin.math.max
import kotlin.math.min

enum class SquatPhase {
    UNKNOWN,
    STANDING_LOCKOUT,
    DESCENDING,
    BOTTOM_DEPTH,
    ASCENDING
}

data class SquatRep(
    val repNumber: Int,
    val startTimestampMs: Long,
    val bottomTimestampMs: Long,
    val endTimestampMs: Long,
    val minKneeAngle: Double,
    val maxKneeAngle: Double,
    val minTorsoAngle: Double,
    val isValid: Boolean,
    val flawReason: String? = null
) {
    val durationSec: Double
        get() = (endTimestampMs - startTimestampMs) / 1000.0

    val concentricDurationMs: Long
        get() = endTimestampMs - bottomTimestampMs
}

data class SquatLiveTelemetry(
    val validRepCount: Int = 0,
    val partialRepCount: Int = 0,
    val currentPhase: SquatPhase = SquatPhase.UNKNOWN,
    val depthProgress: Float = 0f,
    val currentKneeAngle: Double? = null,
    val currentTorsoAngle: Double? = null,
    val averageCadenceSec: Double = 0.0,
    val activeWarning: String? = null,
    val lastCompletedRep: SquatRep? = null
)

/**
 * Biomechanical state machine for Squats.
 * Analyzes real-time knee flexion, torso posture, parallel depth, and rep cadence on-device.
 */
class SquatAnalyzer {

    companion object {
        const val STANDING_LOCKOUT_ANGLE = 160.0    // Full standing lockout
        const val DESCENT_TRIGGER_ANGLE = 152.0     // Knee flexion initiates
        const val PARALLEL_DEPTH_THRESHOLD = 95.0   // Parallel depth limit (thigh parallel to ground)
        const val DEEP_SQUAT_TARGET = 85.0          // Below parallel
        const val MIN_TORSO_ANGLE = 65.0            // Severe forward torso lean threshold
        const val MIN_REP_DURATION_MS = 700L        // Glitch rejection window
    }

    private var currentPhase = SquatPhase.UNKNOWN
    private var repStartTimeMs = 0L
    private var bottomTimestampMs = 0L
    private var minKneeAngleInRep = 180.0
    private var maxKneeAngleInRep = 0.0
    private var minTorsoAngleInRep = 180.0

    private val completedRepsList = mutableListOf<SquatRep>()
    private val recordedFlawsList = mutableListOf<FormFlaw>()

    var liveTelemetry = SquatLiveTelemetry()
        private set

    fun reset() {
        currentPhase = SquatPhase.UNKNOWN
        repStartTimeMs = 0L
        bottomTimestampMs = 0L
        minKneeAngleInRep = 180.0
        maxKneeAngleInRep = 0.0
        minTorsoAngleInRep = 180.0
        completedRepsList.clear()
        recordedFlawsList.clear()
        liveTelemetry = SquatLiveTelemetry()
    }

    fun processPose(pose: TrackedPose, timestampMs: Long): SquatLiveTelemetry {
        if (!pose.isTrackingValid) {
            return liveTelemetry
        }

        val kneeAngle = pose.leftKneeAngle ?: pose.rightKneeAngle
        val torsoAngle = pose.torsoLeanAngle ?: 90.0

        if (kneeAngle == null) {
            return liveTelemetry
        }

        var warningMessage: String? = null

        // 1. Torso uprightness check: prevent excessive forward lean
        if (torsoAngle < MIN_TORSO_ANGLE) {
            warningMessage = "Keep chest up • Excessive forward lean"
        }

        // 2. Depth Progress (0.0 standing at 160° -> 1.0 parallel at 90°)
        val depthProgress = ((STANDING_LOCKOUT_ANGLE - kneeAngle) / (STANDING_LOCKOUT_ANGLE - 90.0))
            .coerceIn(0.0, 1.2)
            .toFloat()

        // 3. Squat Finite State Machine (FSM)
        when (currentPhase) {
            SquatPhase.UNKNOWN -> {
                if (kneeAngle >= STANDING_LOCKOUT_ANGLE - 5.0) {
                    currentPhase = SquatPhase.STANDING_LOCKOUT
                }
            }

            SquatPhase.STANDING_LOCKOUT -> {
                if (kneeAngle < DESCENT_TRIGGER_ANGLE) {
                    currentPhase = SquatPhase.DESCENDING
                    repStartTimeMs = timestampMs
                    minKneeAngleInRep = kneeAngle
                    maxKneeAngleInRep = kneeAngle
                    minTorsoAngleInRep = torsoAngle
                }
            }

            SquatPhase.DESCENDING -> {
                minKneeAngleInRep = min(minKneeAngleInRep, kneeAngle)
                maxKneeAngleInRep = max(maxKneeAngleInRep, kneeAngle)
                minTorsoAngleInRep = min(minTorsoAngleInRep, torsoAngle)

                if (kneeAngle <= PARALLEL_DEPTH_THRESHOLD) {
                    currentPhase = SquatPhase.BOTTOM_DEPTH
                    bottomTimestampMs = timestampMs
                } else if (kneeAngle > minKneeAngleInRep + 8.0) {
                    // Reversed up without hitting parallel depth (Partial Squat)
                    currentPhase = SquatPhase.ASCENDING
                    bottomTimestampMs = timestampMs
                }
            }

            SquatPhase.BOTTOM_DEPTH -> {
                minKneeAngleInRep = min(minKneeAngleInRep, kneeAngle)
                minTorsoAngleInRep = min(minTorsoAngleInRep, torsoAngle)

                // Initiated ascent
                if (kneeAngle > minKneeAngleInRep + 6.0) {
                    currentPhase = SquatPhase.ASCENDING
                }
            }

            SquatPhase.ASCENDING -> {
                maxKneeAngleInRep = max(maxKneeAngleInRep, kneeAngle)
                minTorsoAngleInRep = min(minTorsoAngleInRep, torsoAngle)

                // Returned to standing lockout
                if (kneeAngle >= STANDING_LOCKOUT_ANGLE - 6.0) {
                    val repDuration = timestampMs - repStartTimeMs

                    if (repDuration >= MIN_REP_DURATION_MS) {
                        val isParallel = minKneeAngleInRep <= PARALLEL_DEPTH_THRESHOLD
                        val isUpright = minTorsoAngleInRep >= MIN_TORSO_ANGLE
                        val isValid = isParallel && isUpright

                        val flawReason = when {
                            !isParallel -> "Incomplete depth (${minKneeAngleInRep.toInt()}°)"
                            !isUpright -> "Chest pitched forward (${minTorsoAngleInRep.toInt()}°)"
                            else -> null
                        }

                        val repIndex = completedRepsList.size + 1
                        val newRep = SquatRep(
                            repNumber = repIndex,
                            startTimestampMs = repStartTimeMs,
                            bottomTimestampMs = bottomTimestampMs,
                            endTimestampMs = timestampMs,
                            minKneeAngle = minKneeAngleInRep,
                            maxKneeAngle = maxKneeAngleInRep,
                            minTorsoAngle = minTorsoAngleInRep,
                            isValid = isValid,
                            flawReason = flawReason
                        )
                        completedRepsList.add(newRep)

                        if (!isValid && flawReason != null) {
                            val tip = if (!isParallel) {
                                "Lower hips until thighs are parallel to the floor before driving up."
                            } else {
                                "Keep your chest high and eyes forward to maintain a neutral spine."
                            }
                            recordedFlawsList.add(
                                FormFlaw(
                                    timestampMs = bottomTimestampMs,
                                    title = "Rep $repIndex: $flawReason",
                                    description = "During Rep $repIndex, $flawReason was detected.",
                                    correctionTip = tip,
                                    isSevere = !isUpright
                                )
                            )
                        }
                    }

                    // Reset for next rep
                    currentPhase = SquatPhase.STANDING_LOCKOUT
                    minKneeAngleInRep = 180.0
                    maxKneeAngleInRep = 0.0
                    minTorsoAngleInRep = 180.0
                }
            }
        }

        val validReps = completedRepsList.filter { it.isValid }
        val avgCadence = if (validReps.isNotEmpty()) {
            validReps.map { it.durationSec }.average()
        } else 0.0

        val validCount = validReps.size
        val partialCount = completedRepsList.size - validCount

        liveTelemetry = SquatLiveTelemetry(
            validRepCount = validCount,
            partialRepCount = partialCount,
            currentPhase = currentPhase,
            depthProgress = depthProgress,
            currentKneeAngle = kneeAngle,
            currentTorsoAngle = torsoAngle,
            averageCadenceSec = avgCadence,
            activeWarning = warningMessage,
            lastCompletedRep = completedRepsList.lastOrNull()
        )

        return liveTelemetry
    }

    fun getCompletedReps(): List<SquatRep> = completedRepsList.toList()

    fun getAverageDepthDegrees(): Float {
        if (completedRepsList.isEmpty()) return 90f
        return completedRepsList.map { it.minKneeAngle }.average().toFloat()
    }

    fun getSummaryFlaws(): List<FormFlaw> {
        if (recordedFlawsList.isNotEmpty()) {
            return recordedFlawsList.toList()
        }
        return listOf(
            FormFlaw(
                timestampMs = 5000,
                title = "Squat Depth Opportunity",
                description = "Knees flexed to 98°, just above parallel.",
                correctionTip = "Push knees outward and sit deeper into the hole to reach 90° parallel depth."
            )
        )
    }
}
