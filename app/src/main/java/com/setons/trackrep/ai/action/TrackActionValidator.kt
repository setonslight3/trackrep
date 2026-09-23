package com.setons.trackrep.ai.action

import com.setons.trackrep.exercise.catalog.ExerciseCatalog

/**
 * Strict safety validator for Track AI actions.
 * Enforces physiological limits, checks existence of catalog movements,
 * and ensures actions comply with TrackRep safety guidelines.
 */
object TrackActionValidator {

    fun validate(action: TrackAction): ActionValidationResult {
        return when (action) {
            is ReplaceExerciseAction -> validateReplaceExercise(action)
            is AdjustTargetAction -> validateAdjustTarget(action)
            is RescheduleWorkoutAction -> validateReschedule(action)
            is ModifyRoutineAction -> validateModifyRoutine(action)
            is ExplainWorkoutAdjustmentAction -> ActionValidationResult(isValid = true, action = action)
        }
    }

    fun validateAll(actions: List<TrackAction>): List<ActionValidationResult> {
        return actions.map { validate(it) }
    }

    private fun validateReplaceExercise(action: ReplaceExerciseAction): ActionValidationResult {
        val oldEx = ExerciseCatalog.getById(action.oldExerciseId)
            ?: return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Target exercise '${action.oldExerciseId}' does not exist in catalog."
            )

        val newEx = ExerciseCatalog.getById(action.newExerciseId)
            ?: return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Replacement exercise '${action.newExerciseId}' does not exist in catalog."
            )

        if (action.oldExerciseId == action.newExerciseId) {
            return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Cannot replace exercise '${action.oldExerciseId}' with itself."
            )
        }

        return ActionValidationResult(isValid = true, action = action)
    }

    private fun validateAdjustTarget(action: AdjustTargetAction): ActionValidationResult {
        ExerciseCatalog.getById(action.exerciseId)
            ?: return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Exercise '${action.exerciseId}' not found in catalog."
            )

        action.newTargetReps?.let { reps ->
            if (reps !in 1..50) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Prescribed target reps ($reps) outside safe physiological limits (1–50 reps)."
                )
            }
        }

        action.newTargetSets?.let { sets ->
            if (sets !in 1..10) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Prescribed target sets ($sets) outside safe physiological limits (1–10 sets)."
                )
            }
        }

        action.newTargetHoldSeconds?.let { hold ->
            if (hold !in 5..300) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Prescribed isometric hold duration (${hold}s) outside safe bounds (5–300s)."
                )
            }
        }

        action.newRestSeconds?.let { rest ->
            if (rest !in 15..300) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Prescribed rest interval (${rest}s) outside allowable range (15–300s)."
                )
            }
        }

        return ActionValidationResult(isValid = true, action = action)
    }

    private fun validateReschedule(action: RescheduleWorkoutAction): ActionValidationResult {
        if (action.daysOffset !in -7..14) {
            return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Reschedule offset (${action.daysOffset} days) outside allowable bounds (-7 to +14 days)."
            )
        }
        return ActionValidationResult(isValid = true, action = action)
    }

    private fun validateModifyRoutine(action: ModifyRoutineAction): ActionValidationResult {
        for (id in action.addedExerciseIds) {
            if (ExerciseCatalog.getById(id) == null) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Cannot add unknown exercise '$id' to routine."
                )
            }
        }

        for (id in action.removedExerciseIds) {
            if (ExerciseCatalog.getById(id) == null) {
                return ActionValidationResult(
                    isValid = false,
                    action = action,
                    rejectionReason = "Cannot remove unknown exercise '$id' from routine."
                )
            }
        }

        if (action.addedExerciseIds.size > 8) {
            return ActionValidationResult(
                isValid = false,
                action = action,
                rejectionReason = "Cannot add more than 8 exercises to a single routine session."
            )
        }

        return ActionValidationResult(isValid = true, action = action)
    }
}
