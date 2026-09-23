package com.setons.trackrep

import com.setons.trackrep.ai.action.AdjustTargetAction
import com.setons.trackrep.ai.action.ModifyRoutineAction
import com.setons.trackrep.ai.action.ReplaceExerciseAction
import com.setons.trackrep.ai.action.RescheduleWorkoutAction
import com.setons.trackrep.ai.action.TrackActionValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackActionSchemaTest {

    @Test
    fun testValidReplaceExerciseAction() {
        val action = ReplaceExerciseAction(
            oldExerciseId = "push_up_standard",
            newExerciseId = "push_up_incline",
            reason = "Wrist relief"
        )
        val result = TrackActionValidator.validate(action)
        assertTrue(result.isValid)
    }

    @Test
    fun testRejectUnknownExerciseInReplacement() {
        val action = ReplaceExerciseAction(
            oldExerciseId = "push_up_standard",
            newExerciseId = "non_existent_exercise_123",
            reason = "Test"
        )
        val result = TrackActionValidator.validate(action)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason!!.contains("does not exist"))
    }

    @Test
    fun testRejectSelfReplacement() {
        val action = ReplaceExerciseAction(
            oldExerciseId = "push_up_standard",
            newExerciseId = "push_up_standard",
            reason = "Test"
        )
        val result = TrackActionValidator.validate(action)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason!!.contains("Cannot replace exercise"))
    }

    @Test
    fun testValidAdjustTargetAction() {
        val action = AdjustTargetAction(
            exerciseId = "push_up_standard",
            newTargetReps = 14,
            newTargetSets = 4,
            newRestSeconds = 60,
            reason = "Overload"
        )
        val result = TrackActionValidator.validate(action)
        assertTrue(result.isValid)
    }

    @Test
    fun testRejectUnsafeTargetReps() {
        val actionTooHigh = AdjustTargetAction(
            exerciseId = "push_up_standard",
            newTargetReps = 250, // Unrealistic/unsafe
            reason = "Extreme overload"
        )
        val result = TrackActionValidator.validate(actionTooHigh)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason!!.contains("physiological limits"))

        val actionNegative = AdjustTargetAction(
            exerciseId = "push_up_standard",
            newTargetReps = -5,
            reason = "Negative reps"
        )
        assertFalse(TrackActionValidator.validate(actionNegative).isValid)
    }

    @Test
    fun testRejectUnsafeTargetSets() {
        val action = AdjustTargetAction(
            exerciseId = "push_up_standard",
            newTargetSets = 20, // Excess volume
            reason = "Too many sets"
        )
        val result = TrackActionValidator.validate(action)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason!!.contains("target sets"))
    }

    @Test
    fun testValidRescheduleAction() {
        val action = RescheduleWorkoutAction(daysOffset = 2, reason = "Missed yesterday")
        val result = TrackActionValidator.validate(action)
        assertTrue(result.isValid)
    }

    @Test
    fun testRejectExcessiveRescheduleOffset() {
        val action = RescheduleWorkoutAction(daysOffset = 30, reason = "Month delay")
        val result = TrackActionValidator.validate(action)
        assertFalse(result.isValid)
        assertTrue(result.rejectionReason!!.contains("outside allowable bounds"))
    }
}
