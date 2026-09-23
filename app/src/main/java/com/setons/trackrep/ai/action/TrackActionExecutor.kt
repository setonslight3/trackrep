package com.setons.trackrep.ai.action

import android.content.Context
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.WorkoutItem
import com.setons.trackrep.workout.WorkoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes validated Track AI actions on the domain layer and persists an audit log.
 * Enforces architectural guarantee: "Track service -> Gemini -> validated structured commands -> domain actions."
 */
object TrackActionExecutor {

    suspend fun execute(
        context: Context?,
        action: TrackAction,
        requestPrompt: String = ""
    ): ActionExecutionResult = withContext(Dispatchers.IO) {
        val validation = TrackActionValidator.validate(action)
        if (!validation.isValid) {
            logAction(context, requestPrompt, action.actionType, action.summary, "REJECTED: ${validation.rejectionReason}", false)
            return@withContext ActionExecutionResult(
                action = action,
                isSuccess = false,
                feedbackMessage = "Action rejected by safety layer: ${validation.rejectionReason}"
            )
        }

        val result = when (action) {
            is ReplaceExerciseAction -> executeReplaceExercise(context, action)
            is AdjustTargetAction -> executeAdjustTarget(context, action)
            is RescheduleWorkoutAction -> executeReschedule(context, action)
            is ModifyRoutineAction -> executeModifyRoutine(context, action)
            is ExplainWorkoutAdjustmentAction -> ActionExecutionResult(
                action = action,
                isSuccess = true,
                feedbackMessage = action.explanation
            )
        }

        logAction(context, requestPrompt, action.actionType, action.summary, result.feedbackMessage, result.isSuccess)
        result
    }

    private suspend fun executeReplaceExercise(
        context: Context?,
        action: ReplaceExerciseAction
    ): ActionExecutionResult {
        val oldEx = ExerciseCatalog.getById(action.oldExerciseId)!!
        val newEx = ExerciseCatalog.getById(action.newExerciseId)!!

        val currentRoutine = WorkoutEngine.getActiveOrTodayRoutine()
        val updatedItems = currentRoutine.items.map { item ->
            if (item.exerciseId == action.oldExerciseId) {
                WorkoutItem(
                    exerciseId = newEx.id,
                    targetSets = item.targetSets,
                    targetReps = newEx.defaultReps,
                    targetHoldSeconds = newEx.defaultHoldSeconds,
                    restSeconds = newEx.restSeconds
                )
            } else {
                item
            }
        }

        val newRoutine = currentRoutine.copy(
            id = "adapted_${System.currentTimeMillis()}",
            name = "${currentRoutine.name} (Track Adapted)",
            items = updatedItems
        )
        WorkoutEngine.setCustomRoutine(newRoutine)

        return ActionExecutionResult(
            action = action,
            isSuccess = true,
            feedbackMessage = "Swapped '${oldEx.name}' for '${newEx.name}'. Reason: ${action.reason}."
        )
    }

    private suspend fun executeAdjustTarget(
        context: Context?,
        action: AdjustTargetAction
    ): ActionExecutionResult {
        val ex = ExerciseCatalog.getById(action.exerciseId)!!
        val currentRoutine = WorkoutEngine.getActiveOrTodayRoutine()

        // 1. Update active routine items if present
        val updatedItems = currentRoutine.items.map { item ->
            if (item.exerciseId == action.exerciseId) {
                item.copy(
                    targetReps = action.newTargetReps ?: item.targetReps,
                    targetSets = action.newTargetSets ?: item.targetSets,
                    targetHoldSeconds = action.newTargetHoldSeconds ?: item.targetHoldSeconds,
                    restSeconds = action.newRestSeconds ?: item.restSeconds
                )
            } else {
                item
            }
        }
        WorkoutEngine.setCustomRoutine(currentRoutine.copy(items = updatedItems))

        // 2. Persist to Room ExerciseProgressionDao if database is available
        context?.let { ctx ->
            try {
                val db = TrackRepDatabase.getDatabase(ctx)
                val existing = db.progressionDao().getProgression(action.exerciseId) ?: ExerciseProgressionEntity(
                    exerciseId = action.exerciseId,
                    currentDifficultyRank = ex.difficulty.rank,
                    targetReps = ex.defaultReps,
                    targetSets = ex.defaultSets,
                    targetHoldSeconds = ex.defaultHoldSeconds
                )

                val updatedProg = existing.copy(
                    targetReps = action.newTargetReps ?: existing.targetReps,
                    targetSets = action.newTargetSets ?: existing.targetSets,
                    targetHoldSeconds = action.newTargetHoldSeconds ?: existing.targetHoldSeconds
                )
                db.progressionDao().upsertProgression(updatedProg)
            } catch (e: Exception) {
                // Ignore DB failure in unit test environments without context
            }
        }

        return ActionExecutionResult(
            action = action,
            isSuccess = true,
            feedbackMessage = "Updated targets for '${ex.name}'. Reason: ${action.reason}."
        )
    }

    private fun executeReschedule(
        context: Context?,
        action: RescheduleWorkoutAction
    ): ActionExecutionResult {
        WorkoutEngine.scheduledDateOffsetDays += action.daysOffset
        val dir = if (action.daysOffset >= 0) "+${action.daysOffset}" else "${action.daysOffset}"
        return ActionExecutionResult(
            action = action,
            isSuccess = true,
            feedbackMessage = "Rescheduled workout by $dir days without stacking. Reason: ${action.reason}."
        )
    }

    private fun executeModifyRoutine(
        context: Context?,
        action: ModifyRoutineAction
    ): ActionExecutionResult {
        val currentRoutine = WorkoutEngine.getActiveOrTodayRoutine()
        val mutableItems = currentRoutine.items.filterNot { it.exerciseId in action.removedExerciseIds }.toMutableList()

        for (addId in action.addedExerciseIds) {
            val ex = ExerciseCatalog.getById(addId) ?: continue
            if (mutableItems.none { it.exerciseId == addId }) {
                mutableItems.add(
                    WorkoutItem(
                        exerciseId = ex.id,
                        targetSets = ex.defaultSets,
                        targetReps = ex.defaultReps,
                        targetHoldSeconds = ex.defaultHoldSeconds,
                        restSeconds = ex.restSeconds
                    )
                )
            }
        }

        val updatedRoutine = currentRoutine.copy(
            id = "modified_${System.currentTimeMillis()}",
            name = "${currentRoutine.name} (Track Modified)",
            items = mutableItems
        )
        WorkoutEngine.setCustomRoutine(updatedRoutine)

        return ActionExecutionResult(
            action = action,
            isSuccess = true,
            feedbackMessage = "Modified routine: added ${action.addedExerciseIds.size}, removed ${action.removedExerciseIds.size} exercises. Reason: ${action.reason}."
        )
    }

    private suspend fun logAction(
        context: Context?,
        requestPrompt: String,
        actionType: String,
        details: String,
        result: String,
        isSuccess: Boolean
    ) {
        if (context == null) return
        try {
            val db = TrackRepDatabase.getDatabase(context)
            db.aiActionLogDao().insertLog(
                AIActionLogEntity(
                    requestPrompt = requestPrompt,
                    actionType = actionType,
                    actionDetailsJson = details,
                    executionResult = result,
                    isSuccess = isSuccess
                )
            )
        } catch (_: Exception) {}
    }
}
