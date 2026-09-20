package com.setons.trackrep.coach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.setons.trackrep.exercise.pushup.FatigueLevel

/**
 * Data summary of a completed workout set.
 */
data class CompletedSetSummary(
    val setNumber: Int,
    val durationSeconds: Int,
    val validReps: Int,
    val partialReps: Int,
    val averageDepthDegrees: Float,
    val formConsistencyPercent: Int,
    val fatigueLevel: FatigueLevel
)

/**
 * Lifecycle states of a workout set session.
 */
enum class SetLifecycleState {
    IDLE,
    ACTIVE,
    PAUSED,
    RESTING
}

/**
 * Manages workout sets, elapsed timers, rest countdowns, and set statistics.
 */
class WorkoutSetManager {

    var setNumber by mutableIntStateOf(1)
        private set

    var state by mutableStateOf(SetLifecycleState.IDLE)
        private set

    var activeElapsedSeconds by mutableIntStateOf(0)
        private set

    var restRemainingSeconds by mutableIntStateOf(60)
        private set

    var lastCompletedSummary by mutableStateOf<CompletedSetSummary?>(null)
        private set

    fun startSet() {
        state = SetLifecycleState.ACTIVE
        activeElapsedSeconds = 0
    }

    fun pauseForTrackingLost() {
        if (state == SetLifecycleState.ACTIVE) {
            state = SetLifecycleState.PAUSED
        }
    }

    fun resumeFromTrackingLost() {
        if (state == SetLifecycleState.PAUSED) {
            state = SetLifecycleState.ACTIVE
        }
    }

    fun completeSet(
        validReps: Int,
        partialReps: Int,
        averageDepthDegrees: Float,
        formConsistencyPercent: Int,
        fatigueLevel: FatigueLevel
    ): CompletedSetSummary {
        val summary = CompletedSetSummary(
            setNumber = setNumber,
            durationSeconds = activeElapsedSeconds,
            validReps = validReps,
            partialReps = partialReps,
            averageDepthDegrees = averageDepthDegrees,
            formConsistencyPercent = formConsistencyPercent,
            fatigueLevel = fatigueLevel
        )
        lastCompletedSummary = summary
        state = SetLifecycleState.IDLE
        return summary
    }

    fun startRest(restSeconds: Int = 60) {
        restRemainingSeconds = restSeconds
        state = SetLifecycleState.RESTING
    }

    fun addRestSeconds(seconds: Int = 30) {
        restRemainingSeconds += seconds
    }

    fun skipRest() {
        restRemainingSeconds = 0
        state = SetLifecycleState.IDLE
        setNumber++
    }

    fun tickTimer(): Boolean {
        return when (state) {
            SetLifecycleState.ACTIVE -> {
                activeElapsedSeconds++
                false
            }
            SetLifecycleState.RESTING -> {
                if (restRemainingSeconds > 0) {
                    restRemainingSeconds--
                }
                if (restRemainingSeconds == 0) {
                    state = SetLifecycleState.IDLE
                    setNumber++
                    true // Rest period ended!
                } else {
                    false
                }
            }
            else -> false
        }
    }

    fun dismissSummary() {
        lastCompletedSummary = null
    }

    fun reset() {
        setNumber = 1
        state = SetLifecycleState.IDLE
        activeElapsedSeconds = 0
        restRemainingSeconds = 60
        lastCompletedSummary = null
    }
}
