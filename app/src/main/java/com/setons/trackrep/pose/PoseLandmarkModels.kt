package com.setons.trackrep.pose

import kotlin.math.atan2

data class TrackedLandmark(
    val type: Int,
    val x: Float, // Normalized 0.0 .. 1.0 relative to image frame
    val y: Float, // Normalized 0.0 .. 1.0 relative to image frame
    val inFrameLikelihood: Float
)

data class TrackedPose(
    val landmarks: Map<Int, TrackedLandmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val isTrackingValid: Boolean,
    val leftElbowAngle: Double? = null,
    val rightElbowAngle: Double? = null,
    val hipAlignmentAngle: Double? = null,
    val leftKneeAngle: Double? = null,
    val rightKneeAngle: Double? = null,
    val torsoLeanAngle: Double? = null,
    val formIssues: List<String> = emptyList()
)

object PoseAngleCalculator {

    /**
     * Calculates the internal angle at joint point B formed by segments AB and BC in degrees.
     */
    fun calculateAngle(
        a: TrackedLandmark?,
        b: TrackedLandmark?,
        c: TrackedLandmark?
    ): Double? {
        if (a == null || b == null || c == null) return null
        if (a.inFrameLikelihood < 0.45f || b.inFrameLikelihood < 0.45f || c.inFrameLikelihood < 0.45f) return null

        val angle = Math.toDegrees(
            (atan2((c.y - b.y).toDouble(), (c.x - b.x).toDouble()) -
             atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble()))
        )

        var absAngle = Math.abs(angle)
        if (absAngle > 180.0) {
            absAngle = 360.0 - absAngle
        }
        return absAngle
    }
}
