package com.setons.trackrep.exercise.pushup

/**
 * Stages of a complete push-up rep cycle.
 */
enum class PushUpPhase(val displayName: String) {
    UNKNOWN("Calibrating"),
    PLANK_READY("Plank / Top"),
    DESCENDING("Lowering ↓"),
    BOTTOM_DEPTH("Full Depth ✓"),
    ASCENDING("Pressing Up ↑")
}

/**
 * Records individual rep telemetry for biomechanical analysis.
 */
data class PushUpRep(
    val repNumber: Int,
    val startTimestampMs: Long,
    val bottomTimestampMs: Long,
    val endTimestampMs: Long,
    val minElbowAngle: Double,
    val maxElbowAngle: Double,
    val minHipAngle: Double,
    val isValid: Boolean,
    val flawReason: String? = null
) {
    val durationMs: Long get() = (endTimestampMs - startTimestampMs).coerceAtLeast(0)
    val eccentricMs: Long get() = (bottomTimestampMs - startTimestampMs).coerceAtLeast(0)
    val concentricMs: Long get() = (endTimestampMs - bottomTimestampMs).coerceAtLeast(0)
    val durationSec: Double get() = durationMs / 1000.0
}

/**
 * Real-time state exposed by PushUpAnalyzer for UI overlays and audio/haptic feedback.
 */
data class PushUpLiveTelemetry(
    val validRepCount: Int = 0,
    val partialRepCount: Int = 0,
    val currentPhase: PushUpPhase = PushUpPhase.UNKNOWN,
    val depthProgress: Float = 0f, // 0.0 = top lockout, 1.0 = bottom depth (<=90 deg)
    val currentElbowAngle: Double? = null,
    val currentHipAngle: Double? = null,
    val averageCadenceSec: Double = 0.0,
    val activeWarning: String? = null,
    val lastCompletedRep: PushUpRep? = null
)
