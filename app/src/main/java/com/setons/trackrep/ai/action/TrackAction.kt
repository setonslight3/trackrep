package com.setons.trackrep.ai.action

/**
 * Strict action schema layer defining all permissible mutations Track AI can perform.
 * Non-negotiable architectural rule: "Never allow Gemini to write directly to the database.
 * Track service -> Gemini -> validated structured commands -> domain actions."
 */
sealed interface TrackAction {
    val actionType: String
    val summary: String
}

/**
 * Replaces an exercise in the active routine with a biomechanically suitable alternative
 * (e.g. wrist-friendly, knee-friendly, or difficulty-adapted).
 */
data class ReplaceExerciseAction(
    val oldExerciseId: String,
    val newExerciseId: String,
    val reason: String
) : TrackAction {
    override val actionType: String = "REPLACE_EXERCISE"
    override val summary: String = "Replace exercise: $oldExerciseId -> $newExerciseId ($reason)"
}

/**
 * Adjusts prescribed target volume: target reps, target sets, hold duration, or rest intervals.
 */
data class AdjustTargetAction(
    val exerciseId: String,
    val newTargetReps: Int? = null,
    val newTargetSets: Int? = null,
    val newTargetHoldSeconds: Int? = null,
    val newRestSeconds: Int? = null,
    val reason: String
) : TrackAction {
    override val actionType: String = "ADJUST_TARGET"
    override val summary: String = buildString {
        append("Adjust targets for $exerciseId: ")
        newTargetReps?.let { append("$it reps; ") }
        newTargetSets?.let { append("$it sets; ") }
        newTargetHoldSeconds?.let { append("${it}s hold; ") }
        newRestSeconds?.let { append("${it}s rest; ") }
        append("($reason)")
    }
}

/**
 * Reschedules planned workout sessions without punitive volume stacking.
 */
data class RescheduleWorkoutAction(
    val routineId: String? = null,
    val daysOffset: Int,
    val reason: String
) : TrackAction {
    override val actionType: String = "RESCHEDULE_WORKOUT"
    override val summary: String = "Reschedule workout by $daysOffset days ($reason)"
}

/**
 * Modifies an active workout routine by adding or removing exercises.
 */
data class ModifyRoutineAction(
    val addedExerciseIds: List<String> = emptyList(),
    val removedExerciseIds: List<String> = emptyList(),
    val reason: String
) : TrackAction {
    override val actionType: String = "MODIFY_ROUTINE"
    override val summary: String = "Modify routine: +${addedExerciseIds.size} / -${removedExerciseIds.size} exercises ($reason)"
}

/**
 * Explains rationale, biomechanics, or recovery guidelines without mutating workout state.
 */
data class ExplainWorkoutAdjustmentAction(
    val topic: String,
    val explanation: String
) : TrackAction {
    override val actionType: String = "EXPLAIN_ADJUSTMENT"
    override val summary: String = "Explanation on $topic"
}

/**
 * Envelope holding Track's conversational reply message and validated action list.
 */
data class TrackAiResponse(
    val replyMessage: String,
    val actions: List<TrackAction> = emptyList()
)

/**
 * Result of strict action schema validation.
 */
data class ActionValidationResult(
    val isValid: Boolean,
    val action: TrackAction,
    val rejectionReason: String? = null
)

/**
 * Result of executing a validated action against domain state.
 */
data class ActionExecutionResult(
    val action: TrackAction,
    val isSuccess: Boolean,
    val feedbackMessage: String
)
