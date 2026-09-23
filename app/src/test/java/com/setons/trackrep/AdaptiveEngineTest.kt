package com.setons.trackrep

import com.setons.trackrep.adaptive.AdaptiveEngine
import com.setons.trackrep.adaptive.ProgressionAction
import com.setons.trackrep.adaptive.RecoveryPhase
import com.setons.trackrep.coach.CompletedSetSummary
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.pushup.FatigueLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveEngineTest {

    private fun createSampleProgression(
        exerciseId: String = "push_up_standard",
        rank: Int = 3,
        reps: Int = 10,
        sets: Int = 3,
        hold: Int = 0,
        successes: Int = 0,
        failures: Int = 0
    ) = ExerciseProgressionEntity(
        exerciseId = exerciseId,
        currentDifficultyRank = rank,
        targetReps = reps,
        targetSets = sets,
        targetHoldSeconds = hold,
        consecutiveSuccesses = successes,
        consecutiveFailures = failures
    )

    private fun createSetSummary(
        validReps: Int = 10,
        partialReps: Int = 0,
        duration: Int = 25,
        formPct: Int = 90,
        fatigue: FatigueLevel = FatigueLevel.FRESH
    ) = CompletedSetSummary(
        setNumber = 1,
        durationSeconds = duration,
        validReps = validReps,
        partialReps = partialReps,
        averageDepthDegrees = 88f,
        formConsistencyPercent = formPct,
        fatigueLevel = fatigue
    )

    @Test
    fun testProgressionOverloadOnTooEasyWithHighForm() {
        val current = createSampleProgression(reps = 10)
        val summary = createSetSummary(formPct = 92)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "TOO_EASY")

        assertEquals(ProgressionAction.OVERLOAD_INCREMENT, eval.action)
        assertEquals(12, eval.newTargetReps) // +2 reps progressive overload
        assertEquals(1, eval.consecutiveSuccesses)
        assertEquals(0, eval.consecutiveFailures)
        assertTrue(eval.coachExplanation.contains("+2 reps"))
    }

    @Test
    fun testProgressionOverloadOnJustRightWithHighForm() {
        val current = createSampleProgression(reps = 10)
        val summary = createSetSummary(formPct = 88)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "JUST_RIGHT")

        assertEquals(ProgressionAction.OVERLOAD_INCREMENT, eval.action)
        assertEquals(11, eval.newTargetReps) // +1 rep progressive overload
        assertEquals(1, eval.consecutiveSuccesses)
    }

    @Test
    fun testConsolidationOnDifficultRating() {
        val current = createSampleProgression(reps = 12)
        val summary = createSetSummary(formPct = 78, fatigue = FatigueLevel.HIGH)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "DIFFICULT")

        assertEquals(ProgressionAction.STABLE_CONSOLIDATE, eval.action)
        assertEquals(12, eval.newTargetReps) // Target maintained
    }

    @Test
    fun testSafetyDeloadOnIncompleteFailure() {
        val current = createSampleProgression(reps = 14)
        val summary = createSetSummary(validReps = 7, partialReps = 4, formPct = 60, fatigue = FatigueLevel.EXHAUSTED)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "COULD_NOT_COMPLETE")

        assertEquals(ProgressionAction.DELOAD_SAFETY_BACKOFF, eval.action)
        assertEquals(12, eval.newTargetReps) // -2 reps deload
        assertEquals(0, eval.consecutiveSuccesses)
        assertEquals(1, eval.consecutiveFailures)
        assertTrue(eval.coachExplanation.contains("Safety back-off"))
    }

    @Test
    fun testAdvanceTierAfterConsecutiveMasteries() {
        val current = createSampleProgression(reps = 16, successes = 2)
        val summary = createSetSummary(formPct = 95)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "TOO_EASY")

        assertEquals(ProgressionAction.ADVANCE_TIER, eval.action)
        assertEquals(3, eval.consecutiveSuccesses)
        assertTrue(eval.coachExplanation.contains("ready to advance to Tier 4"))
    }

    @Test
    fun testIsometricProgressionForPlank() {
        val current = createSampleProgression(exerciseId = "plank_standard", reps = 0, hold = 45)
        val summary = createSetSummary(validReps = 0, duration = 45, formPct = 90)

        val eval = AdaptiveEngine.evaluateSetProgression(current, summary, "TOO_EASY")

        assertEquals(ProgressionAction.OVERLOAD_INCREMENT, eval.action)
        assertEquals(55, eval.newTargetHoldSeconds) // +10s progressive overload
    }

    @Test
    fun testMuscleRecoverySafeguardsCalculation() {
        val now = System.currentTimeMillis()
        val fiveHoursAgo = now - (5L * 60L * 60L * 1000L)
        val thirtyHoursAgo = now - (30L * 60L * 60L * 1000L)

        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "push_up_standard",
                exerciseName = "Standard Push-up",
                timestampMs = fiveHoursAgo,
                dateString = "Today",
                durationSeconds = 120,
                totalValidReps = 30,
                totalPartialReps = 2,
                averageFormScore = 88,
                fatigueVelocityLossPercent = 0.1f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "squat_bodyweight",
                exerciseName = "Bodyweight Squat",
                timestampMs = thirtyHoursAgo,
                dateString = "Yesterday",
                durationSeconds = 150,
                totalValidReps = 36,
                totalPartialReps = 0,
                averageFormScore = 92,
                fatigueVelocityLossPercent = 0.05f,
                perceivedRating = "TOO_EASY"
            )
        )

        val recoveryMap = AdaptiveEngine.computeMuscleRecovery(sessions, nowMs = now)

        // Chest was trained 5 hours ago -> FATIGUED (<24h)
        val chestStatus = recoveryMap[MuscleGroup.CHEST]!!
        assertEquals(RecoveryPhase.FATIGUED, chestStatus.phase)
        assertTrue(chestStatus.hoursSinceTrained < 6f)
        assertTrue(chestStatus.recoveryPercentage < 70)

        // Quads trained 30 hours ago -> RECOVERING (24–48h)
        val quadsStatus = recoveryMap[MuscleGroup.QUADS]!!
        assertEquals(RecoveryPhase.RECOVERING, quadsStatus.phase)
        assertTrue(quadsStatus.recoveryPercentage in 65..95)

        // Back was not trained -> FULLY_RECOVERED
        val backStatus = recoveryMap[MuscleGroup.BACK]!!
        assertEquals(RecoveryPhase.FULLY_RECOVERED, backStatus.phase)
        assertEquals(100, backStatus.recoveryPercentage)
    }

    @Test
    fun testRecoverySafeguardAdaptsRecommendedDailyRoutine() {
        val now = System.currentTimeMillis()
        val fourHoursAgo = now - (4L * 60L * 60L * 1000L)

        // Heavy chest push-up session just completed
        val recentSessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "push_up_standard",
                exerciseName = "Standard Push-up",
                timestampMs = fourHoursAgo,
                dateString = "Today",
                durationSeconds = 180,
                totalValidReps = 40,
                totalPartialReps = 2,
                averageFormScore = 85,
                fatigueVelocityLossPercent = 0.2f,
                perceivedRating = "DIFFICULT"
            )
        )

        val recoveryMap = AdaptiveEngine.computeMuscleRecovery(recentSessions, nowMs = now)
        val recommendation = AdaptiveEngine.getRecommendedAdaptiveRoutine(recoveryMap)

        // Should avoid chest and pick non-conflicting routine (e.g. Legs & Posterior)
        assertTrue(recommendation.fatiguedMusclesAvoided.contains(MuscleGroup.CHEST))
        assertNotEquals("routine_total_body_burn", recommendation.routine.id)
        assertTrue(recommendation.explanation.contains("Recovery Safeguard Active"))
    }

    @Test
    fun testWorkoutCadenceAndLayoffRescheduling() {
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - (10L * 24L * 60L * 60L * 1000L)
        val fiveDaysAgo = now - (5L * 24L * 60L * 60L * 1000L)

        // Layoff (>7 days)
        val layoffCadence = AdaptiveEngine.analyzeWorkoutCadence(tenDaysAgo, nowMs = now)
        assertTrue(layoffCadence.isLayoff)
        assertEquals(0.80f, layoffCadence.volumeMultiplier)
        assertTrue(layoffCadence.guidance.contains("Re-entry Protocol"))

        // Missed Cadence (4-7 days)
        val missedCadence = AdaptiveEngine.analyzeWorkoutCadence(fiveDaysAgo, nowMs = now)
        assertFalse(missedCadence.isLayoff)
        assertTrue(missedCadence.isMissedCadence)
        assertEquals(0.90f, missedCadence.volumeMultiplier)
        assertTrue(missedCadence.guidance.contains("Catch-Up Cadence"))
    }

    @Test
    fun testFutureWorkoutDemonstrablyAdaptsToPriorPerformance() {
        // Acceptance test requirement: Future workouts demonstrably adapt to prior performance
        val priorProgression = createSampleProgression(exerciseId = "push_up_standard", reps = 10)
        val excellentSet = createSetSummary(validReps = 10, formPct = 95)

        val eval = AdaptiveEngine.evaluateSetProgression(priorProgression, excellentSet, "TOO_EASY")

        // Prior target: 10 reps -> Adapted future target: 12 reps
        assertEquals(10, priorProgression.targetReps)
        assertEquals(12, eval.newTargetReps)
        assertTrue(eval.newTargetReps > priorProgression.targetReps)
    }
}
