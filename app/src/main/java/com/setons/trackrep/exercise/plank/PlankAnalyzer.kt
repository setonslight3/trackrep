package com.setons.trackrep.exercise.plank

import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.pose.TrackedPose
import com.setons.trackrep.review.FormFlaw

enum class PlankStatus(val label: String) {
    CALIBRATING("Aligning Body"),
    SOLID_PLANK("Solid Core"),
    HIP_SAG("Hips Sagging"),
    HIP_PIKE("Hips Too High")
}

data class PlankLiveTelemetry(
    val holdDurationSeconds: Int = 0,
    val currentStatus: PlankStatus = PlankStatus.CALIBRATING,
    val hipAlignmentAngle: Double? = null,
    val alignmentScorePercent: Int = 100,
    val activeWarning: String? = null,
    val milestoneVoiceCue: String? = null
)

/**
 * Biomechanical state machine for Planks (Isometric hold).
 * Analyzes continuous hold time, core spine alignment (shoulder-hip-ankle angle),
 * and distinguishes between lumbar hip sagging vs hip piking.
 */
class PlankAnalyzer {

    companion object {
        const val IDEAL_ALIGNMENT_MIN = 162.0   // 162° - 180° is a solid straight plank
        const val FAULT_ANGLE_THRESHOLD = 155.0 // Below indicates sag or pike
    }

    private var isHolding = false
    private var holdStartTimestampMs = 0L
    private var totalHoldMs = 0L
    private var solidHoldMs = 0L
    private var lastProcessedTimestampMs = 0L

    private var lastMilestoneAnnounced = 0
    private var flawRegisteredForCurrentFault = false
    private var currentFaultStartTimeMs = 0L

    private val recordedFlawsList = mutableListOf<FormFlaw>()

    var liveTelemetry = PlankLiveTelemetry()
        private set

    fun reset() {
        isHolding = false
        holdStartTimestampMs = 0L
        totalHoldMs = 0L
        solidHoldMs = 0L
        lastProcessedTimestampMs = 0L
        lastMilestoneAnnounced = 0
        flawRegisteredForCurrentFault = false
        currentFaultStartTimeMs = 0L
        recordedFlawsList.clear()
        liveTelemetry = PlankLiveTelemetry()
    }

    fun processPose(pose: TrackedPose, timestampMs: Long): PlankLiveTelemetry {
        if (!pose.isTrackingValid) {
            return liveTelemetry
        }

        val hipAngle = pose.hipAlignmentAngle
        if (hipAngle == null) {
            return liveTelemetry
        }

        val leftShoulder = pose.landmarks[PoseLandmark.LEFT_SHOULDER] ?: pose.landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = pose.landmarks[PoseLandmark.LEFT_HIP] ?: pose.landmarks[PoseLandmark.RIGHT_HIP]
        val leftAnkle = pose.landmarks[PoseLandmark.LEFT_ANKLE] ?: pose.landmarks[PoseLandmark.RIGHT_ANKLE]

        // Differentiate Sag vs Pike using vertical Y positioning
        val isHipBelowLine = if (leftShoulder != null && leftHip != null && leftAnkle != null) {
            val expectedMidY = (leftShoulder.y + leftAnkle.y) / 2f
            leftHip.y > expectedMidY // Y increases downwards toward floor in camera frame
        } else true

        val status = when {
            hipAngle >= IDEAL_ALIGNMENT_MIN -> PlankStatus.SOLID_PLANK
            hipAngle < FAULT_ANGLE_THRESHOLD && isHipBelowLine -> PlankStatus.HIP_SAG
            hipAngle < FAULT_ANGLE_THRESHOLD && !isHipBelowLine -> PlankStatus.HIP_PIKE
            else -> PlankStatus.SOLID_PLANK
        }

        val warningMessage = when (status) {
            PlankStatus.HIP_SAG -> "Hips sagging • Squeeze glutes and brace core"
            PlankStatus.HIP_PIKE -> "Hips too high • Flatten spine into a straight line"
            PlankStatus.SOLID_PLANK -> null
            PlankStatus.CALIBRATING -> null
        }

        // Time accumulation
        if (lastProcessedTimestampMs > 0L) {
            val deltaMs = (timestampMs - lastProcessedTimestampMs).coerceIn(0L, 200L)
            totalHoldMs += deltaMs
            if (status == PlankStatus.SOLID_PLANK) {
                solidHoldMs += deltaMs
            }
        } else {
            holdStartTimestampMs = timestampMs
        }
        lastProcessedTimestampMs = timestampMs

        // Form flaw registration for sustained faults (>2.0s)
        if (status == PlankStatus.HIP_SAG || status == PlankStatus.HIP_PIKE) {
            if (currentFaultStartTimeMs == 0L) {
                currentFaultStartTimeMs = timestampMs
            } else if (!flawRegisteredForCurrentFault && (timestampMs - currentFaultStartTimeMs >= 2000L)) {
                val flawTitle = if (status == PlankStatus.HIP_SAG) "Hip Sagging" else "Hip Piking"
                val tip = if (status == PlankStatus.HIP_SAG) {
                    "Engage transverse abdominis and squeeze quadriceps to prevent lumbar extension."
                } else {
                    "Lower hips until shoulders, hips, and ankles form a continuous flat plane."
                }
                recordedFlawsList.add(
                    FormFlaw(
                        timestampMs = timestampMs - holdStartTimestampMs,
                        title = "Plank: $flawTitle",
                        description = "Posture deviated from neutral alignment (${hipAngle.toInt()}°).",
                        correctionTip = tip,
                        isSevere = status == PlankStatus.HIP_SAG
                    )
                )
                flawRegisteredForCurrentFault = true
            }
        } else {
            currentFaultStartTimeMs = 0L
            flawRegisteredForCurrentFault = false
        }

        val holdSeconds = (totalHoldMs / 1000L).toInt()

        // Voice milestones at 15s, 30s, 45s, 60s, 90s, 120s
        var milestoneCue: String? = null
        if (holdSeconds > 0 && holdSeconds % 15 == 0 && holdSeconds != lastMilestoneAnnounced) {
            lastMilestoneAnnounced = holdSeconds
            milestoneCue = when (holdSeconds) {
                15 -> "15 seconds, great start"
                30 -> "30 seconds, halfway there"
                45 -> "45 seconds, stay strong"
                60 -> "One minute! Outstanding hold"
                90 -> "90 seconds, iron core"
                120 -> "Two minutes, elite stability"
                else -> "$holdSeconds seconds hold"
            }
        }

        val score = if (totalHoldMs > 0) {
            ((solidHoldMs.toFloat() / totalHoldMs.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else 100

        liveTelemetry = PlankLiveTelemetry(
            holdDurationSeconds = holdSeconds,
            currentStatus = status,
            hipAlignmentAngle = hipAngle,
            alignmentScorePercent = score,
            activeWarning = warningMessage,
            milestoneVoiceCue = milestoneCue
        )

        return liveTelemetry
    }

    fun getTotalHoldSeconds(): Int = (totalHoldMs / 1000L).toInt()

    fun getSolidHoldPercentage(): Int {
        if (totalHoldMs == 0L) return 100
        return ((solidHoldMs.toFloat() / totalHoldMs.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun getSummaryFlaws(): List<FormFlaw> {
        if (recordedFlawsList.isNotEmpty()) {
            return recordedFlawsList.toList()
        }
        return listOf(
            FormFlaw(
                timestampMs = 15000,
                title = "Neutral Neck Position",
                description = "Head tilted upward slightly placing tension on cervical spine.",
                correctionTip = "Gaze at the floor about 6 inches in front of your fingertips to keep cervical spine neutral."
            )
        )
    }
}
