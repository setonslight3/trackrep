package com.setons.trackrep

import com.setons.trackrep.analytics.ProgressAnalyticsEngine
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.exercise.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressAnalyticsEngineTest {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun testComputeLifetimeStats_AggregatesAccurately() {
        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "pushup",
                exerciseName = "Push-Up",
                dateString = "2026-09-21",
                durationSeconds = 120,
                totalValidReps = 25,
                totalPartialReps = 2,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "squat",
                exerciseName = "Bodyweight Squat",
                dateString = "2026-09-22",
                durationSeconds = 180,
                totalValidReps = 35,
                totalPartialReps = 1,
                averageFormScore = 96,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            )
        )

        val stats = ProgressAnalyticsEngine.computeLifetimeStats(sessions)
        assertEquals(2, stats.totalWorkouts)
        assertEquals(60, stats.totalValidReps)
        assertEquals(5, stats.totalTrainingMinutes)
        assertEquals(93, stats.averageFormAccuracy)
    }

    @Test
    fun testComputeStreaks_ConsecutiveDays() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24)
        }

        val cal22 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }
        val cal23 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val cal24 = cal.clone() as Calendar

        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "pushup",
                exerciseName = "Push-Up",
                timestampMs = cal22.timeInMillis,
                dateString = "2026-09-22",
                durationSeconds = 60,
                totalValidReps = 10,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "squat",
                exerciseName = "Squat",
                timestampMs = cal23.timeInMillis,
                dateString = "2026-09-23",
                durationSeconds = 60,
                totalValidReps = 15,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "plank",
                exerciseName = "Plank",
                timestampMs = cal24.timeInMillis,
                dateString = "2026-09-24",
                durationSeconds = 45,
                totalValidReps = 0,
                totalPartialReps = 0,
                averageFormScore = 95,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            )
        )

        val streaks = ProgressAnalyticsEngine.computeStreaks(sessions, cal)
        assertEquals(3, streaks.currentStreakDays)
        assertEquals(3, streaks.bestStreakDays)
    }

    @Test
    fun testComputeStreaks_GapBreaksCurrentStreak() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24)
        }

        val cal18 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        val cal19 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -5) }
        val cal20 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -4) }

        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "pushup",
                exerciseName = "Push-Up",
                timestampMs = cal18.timeInMillis,
                dateString = dayFormat.format(cal18.time),
                durationSeconds = 60,
                totalValidReps = 10,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "squat",
                exerciseName = "Squat",
                timestampMs = cal19.timeInMillis,
                dateString = dayFormat.format(cal19.time),
                durationSeconds = 60,
                totalValidReps = 15,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "plank",
                exerciseName = "Plank",
                timestampMs = cal20.timeInMillis,
                dateString = dayFormat.format(cal20.time),
                durationSeconds = 45,
                totalValidReps = 0,
                totalPartialReps = 0,
                averageFormScore = 95,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            )
        )

        val streaks = ProgressAnalyticsEngine.computeStreaks(sessions, cal)
        assertEquals(0, streaks.currentStreakDays)
        assertEquals(3, streaks.bestStreakDays)
    }

    @Test
    fun testComputeMilestones() {
        val progressions = listOf(
            ExerciseProgressionEntity(
                exerciseId = "pushup",
                personalRecordReps = 32,
                targetReps = 35,
                currentDifficultyRank = 3
            ),
            ExerciseProgressionEntity(
                exerciseId = "plank",
                personalRecordHoldSeconds = 90,
                targetHoldSeconds = 120,
                currentDifficultyRank = 3
            )
        )

        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "pushup",
                exerciseName = "Push-Up",
                dateString = "2026-09-23",
                durationSeconds = 60,
                totalValidReps = 32,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            )
        )

        val milestones = ProgressAnalyticsEngine.computeMilestones(progressions, sessions)
        val pushupMilestone = milestones.find { it.exerciseId == "push_up_standard" || it.exerciseId == "pushup" }
        assertNotNull(pushupMilestone)
        assertEquals(32, pushupMilestone?.personalRecordValue)
        assertEquals("reps", pushupMilestone?.personalRecordUnit)
        assertEquals(1, pushupMilestone?.totalSessionsCompleted)

        val plankMilestone = milestones.find { it.exerciseId == "plank_standard" || it.exerciseId == "plank" }
        assertNotNull(plankMilestone)
        assertEquals(90, plankMilestone?.personalRecordValue)
        assertEquals("sec", plankMilestone?.personalRecordUnit)
        assertTrue(plankMilestone?.isIsometric == true)
    }

    @Test
    fun testComputeMuscleDistribution() {
        val sessions = listOf(
            WorkoutSessionEntity(
                exerciseId = "pushup",
                exerciseName = "Push-Up",
                dateString = "2026-09-23",
                durationSeconds = 60,
                totalValidReps = 20,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            ),
            WorkoutSessionEntity(
                exerciseId = "squat",
                exerciseName = "Squat",
                dateString = "2026-09-23",
                durationSeconds = 60,
                totalValidReps = 20,
                totalPartialReps = 0,
                averageFormScore = 90,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = "JUST_RIGHT"
            )
        )

        val distribution = ProgressAnalyticsEngine.computeMuscleDistribution(sessions)
        assertTrue("Distribution should contain muscle groups", distribution.isNotEmpty())

        val chestStat = distribution.find { it.muscleGroup == MuscleGroup.CHEST }
        val quadsStat = distribution.find { it.muscleGroup == MuscleGroup.QUADS }
        assertNotNull(chestStat)
        assertNotNull(quadsStat)
        assertTrue("Chest volume should be greater than 0", chestStat!!.reps > 0)
        assertTrue("Quads volume should be greater than 0", quadsStat!!.reps > 0)
    }
}
