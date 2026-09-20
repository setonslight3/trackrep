package com.setons.trackrep

import com.setons.trackrep.coach.SetLifecycleState
import com.setons.trackrep.coach.TrackingRecoveryManager
import com.setons.trackrep.coach.TrackingState
import com.setons.trackrep.coach.WorkoutSetManager
import com.setons.trackrep.exercise.pushup.FatigueDetector
import com.setons.trackrep.exercise.pushup.FatigueLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {

    @Test
    fun testFatigueDetectorVelocityLossAndDegradation() {
        val detector = FatigueDetector()

        // Rep 1 & 2: Baseline established (~600ms concentric ascent)
        detector.onRepCompleted(concentricDurationMs = 600L, hadFormFault = false)
        val (level2, _) = detector.onRepCompleted(concentricDurationMs = 600L, hadFormFault = false)
        assertEquals(FatigueLevel.FRESH, level2)
        assertEquals(100, detector.formConsistencyScore)

        // Rep 3: Slight tempo slowing (650ms), still fresh
        val (level3, _) = detector.onRepCompleted(concentricDurationMs = 650L, hadFormFault = false)
        assertEquals(FatigueLevel.FRESH, level3)

        // Rep 4 & 5: Concentric slowing by ~40% (850ms) -> MODERATE fatigue
        detector.onRepCompleted(concentricDurationMs = 850L, hadFormFault = true)
        val (level5, _) = detector.onRepCompleted(concentricDurationMs = 860L, hadFormFault = false)
        assertEquals(FatigueLevel.MODERATE, level5)
        assertTrue(detector.velocityLossPercent > 30f)
        assertEquals(80, detector.formConsistencyScore) // 4/5 clean reps = 80%

        // Rep 6 & 7: Extreme slowing by >65% (1100ms) -> EXHAUSTED / Max effort
        detector.onRepCompleted(concentricDurationMs = 1100L, hadFormFault = true)
        val (level7, cue7) = detector.onRepCompleted(concentricDurationMs = 1150L, hadFormFault = false)
        assertEquals(FatigueLevel.EXHAUSTED, level7)
        assertNotNull(cue7)
        assertTrue(cue7!!.contains("Max effort") || cue7.contains("push through"))
    }

    @Test
    fun testWorkoutSetManagerLifecycleAndTimers() {
        val setManager = WorkoutSetManager()
        assertEquals(1, setManager.setNumber)
        assertEquals(SetLifecycleState.IDLE, setManager.state)

        // Start Set 1
        setManager.startSet()
        assertEquals(SetLifecycleState.ACTIVE, setManager.state)

        // Tick timer for 5 seconds
        repeat(5) { setManager.tickTimer() }
        assertEquals(5, setManager.activeElapsedSeconds)

        // Pause on tracking loss
        setManager.pauseForTrackingLost()
        assertEquals(SetLifecycleState.PAUSED, setManager.state)

        // Timer should not increment while paused
        setManager.tickTimer()
        assertEquals(5, setManager.activeElapsedSeconds)

        // Resume tracking
        setManager.resumeFromTrackingLost()
        assertEquals(SetLifecycleState.ACTIVE, setManager.state)

        // Complete set
        val summary = setManager.completeSet(
            validReps = 12,
            partialReps = 1,
            averageDepthDegrees = 88.5f,
            formConsistencyPercent = 92,
            fatigueLevel = FatigueLevel.MODERATE
        )
        assertEquals(1, summary.setNumber)
        assertEquals(12, summary.validReps)
        assertEquals(1, summary.partialReps)
        assertEquals(88.5f, summary.averageDepthDegrees, 0.01f)
        assertEquals(92, summary.formConsistencyPercent)
        assertEquals(FatigueLevel.MODERATE, summary.fatigueLevel)
        assertEquals(SetLifecycleState.IDLE, setManager.state)

        // Start Rest Period
        setManager.startRest(restSeconds = 60)
        assertEquals(SetLifecycleState.RESTING, setManager.state)
        assertEquals(60, setManager.restRemainingSeconds)

        // Add 30 seconds
        setManager.addRestSeconds(30)
        assertEquals(90, setManager.restRemainingSeconds)

        // Skip rest to advance set number
        setManager.skipRest()
        assertEquals(2, setManager.setNumber)
        assertEquals(SetLifecycleState.IDLE, setManager.state)
    }

    @Test
    fun testTrackingRecoveryManagerTransitions() {
        var lostCalled = false
        var recoveredCalled = false

        val manager = TrackingRecoveryManager(
            onTrackingLost = { lostCalled = true },
            onTrackingRecovered = { recoveredCalled = true }
        )

        // Continuous visibility maintains Tracking state
        val state1 = manager.processFrame(isVisible = true)
        assertTrue(state1 is TrackingState.Tracking)

        // Drop visibility for < 1500ms (should not trigger lost yet)
        manager.processFrame(isVisible = false)
        assertTrue(manager.currentState is TrackingState.Tracking)
    }
}
