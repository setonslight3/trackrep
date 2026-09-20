package com.setons.trackrep.exercise.pushup

/**
 * Categorization of muscular fatigue during an exercise set.
 */
enum class FatigueLevel(val label: String) {
    FRESH("Fresh"),
    MODERATE("Fatigue Building"),
    HIGH("High Fatigue"),
    EXHAUSTED("Max Effort")
}

/**
 * Analyzes movement velocity and form consistency across multiple repetitions
 * to detect muscular fatigue, velocity loss, and form breakdown.
 *
 * Implements non-negotiable rule: "Detect sustained performance degradation
 * rather than treating a single noisy frame as fatigue."
 */
class FatigueDetector {

    private val concentricDurations = mutableListOf<Long>()
    private var baselineDurationMs: Float = 0f

    private var totalCompletedReps: Int = 0
    private var repsWithFormErrors: Int = 0

    var currentFatigueLevel: FatigueLevel = FatigueLevel.FRESH
        private set

    var velocityLossPercent: Float = 0f
        private set

    val formConsistencyScore: Int
        get() {
            if (totalCompletedReps == 0) return 100
            val validRatio = (totalCompletedReps - repsWithFormErrors).coerceAtLeast(0).toFloat() / totalCompletedReps
            return (validRatio * 100f).toInt().coerceIn(0, 100)
        }

    /**
     * Records a completed rep's concentric duration (ascent phase from depth to lockout).
     *
     * @param concentricDurationMs Milliseconds spent in the ascending phase
     * @param hadFormFault True if this rep exhibited core sag, incomplete depth, or misalignment
     * @return Pair of updated FatigueLevel and an optional verbal coaching cue
     */
    fun onRepCompleted(concentricDurationMs: Long, hadFormFault: Boolean): Pair<FatigueLevel, String?> {
        totalCompletedReps++
        if (hadFormFault) {
            repsWithFormErrors++
        }

        concentricDurations.add(concentricDurationMs)

        // Establish baseline from initial 2 reps
        if (concentricDurations.size <= 2) {
            baselineDurationMs = concentricDurations.average().toFloat()
            currentFatigueLevel = FatigueLevel.FRESH
            return Pair(currentFatigueLevel, null)
        }

        // Calculate moving average of recent 2 reps to smooth out noise
        val recentAverage = concentricDurations.takeLast(2).average().toFloat()
        
        // Velocity Loss = percentage slowdown in concentric ascent speed
        if (baselineDurationMs > 0f) {
            velocityLossPercent = ((recentAverage - baselineDurationMs) / baselineDurationMs * 100f).coerceAtLeast(0f)
        }

        var voiceCue: String? = null

        when {
            velocityLossPercent >= 65f -> {
                currentFatigueLevel = FatigueLevel.EXHAUSTED
                voiceCue = "Max effort, push through!"
            }
            velocityLossPercent >= 45f -> {
                currentFatigueLevel = FatigueLevel.HIGH
                voiceCue = "High fatigue, keep form tight"
            }
            velocityLossPercent >= 25f -> {
                currentFatigueLevel = FatigueLevel.MODERATE
                voiceCue = if (hadFormFault) "Brace core as fatigue builds" else null
            }
            else -> {
                currentFatigueLevel = FatigueLevel.FRESH
            }
        }

        return Pair(currentFatigueLevel, voiceCue)
    }

    fun reset() {
        concentricDurations.clear()
        baselineDurationMs = 0f
        totalCompletedReps = 0
        repsWithFormErrors = 0
        currentFatigueLevel = FatigueLevel.FRESH
        velocityLossPercent = 0f
    }
}
