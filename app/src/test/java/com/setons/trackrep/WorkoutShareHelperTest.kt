package com.setons.trackrep

import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.share.WorkoutShareHelper
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutShareHelperTest {

    @Test
    fun testFormatWorkoutShareText_IncludesAllKeyMetrics() {
        val session = WorkoutSessionEntity(
            id = "sess_share_test",
            routineName = "Full Body Foundation",
            exerciseId = "pushup",
            exerciseName = "Standard Push-Up",
            dateString = "Sep 23, 2026 • 8:15 PM",
            durationSeconds = 195,
            totalValidReps = 28,
            totalPartialReps = 1,
            averageFormScore = 94,
            fatigueVelocityLossPercent = 2.5f,
            perceivedRating = "JUST_RIGHT"
        )

        val sets = listOf(
            SetRecordEntity(
                sessionId = "sess_share_test",
                exerciseId = "pushup",
                setNumber = 1,
                validReps = 14,
                partialReps = 0,
                durationSeconds = 90,
                averageDepthDegrees = 89f,
                formConsistencyPercent = 95,
                fatigueLevel = "NONE"
            ),
            SetRecordEntity(
                sessionId = "sess_share_test",
                exerciseId = "pushup",
                setNumber = 2,
                validReps = 14,
                partialReps = 1,
                durationSeconds = 105,
                averageDepthDegrees = 87f,
                formConsistencyPercent = 93,
                fatigueLevel = "MODERATE"
            )
        )

        val shareText = WorkoutShareHelper.formatWorkoutShareText(session, sets)

        assertTrue("Must mention exercise name", shareText.contains("Standard Push-Up"))
        assertTrue("Must mention routine name", shareText.contains("Full Body Foundation"))
        assertTrue("Must include total valid reps", shareText.contains("28 Valid Reps"))
        assertTrue("Must include form consistency", shareText.contains("94%"))
        assertTrue("Must include duration format", shareText.contains("3m 15s"))
        assertTrue("Must include set breakdown", shareText.contains("Set 1: 14 reps"))
        assertTrue("Must include Set 2", shareText.contains("Set 2: 14 reps"))
        assertTrue("Must include TrackRep branding", shareText.contains("TrackRep"))
        assertTrue("Must mention 100% Local-First", shareText.contains("100% Local-First"))
    }
}
