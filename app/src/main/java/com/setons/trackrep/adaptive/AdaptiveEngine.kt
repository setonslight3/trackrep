package com.setons.trackrep.adaptive

import com.setons.trackrep.coach.CompletedSetSummary
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.model.WorkoutItem
import com.setons.trackrep.exercise.model.WorkoutRoutine
import com.setons.trackrep.workout.WorkoutEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Muscle recovery phase based on biomechanical restitution windows.
 */
enum class RecoveryPhase(val label: String, val baseRecoveryPct: Int) {
    FATIGUED("Fatigued (<24h)", 45),
    RECOVERING("Recovering (24–48h)", 75),
    FULLY_RECOVERED("Fully Recovered (48h+)", 100)
}

/**
 * Recovery state for an individual muscle group.
 */
data class MuscleRecoveryStatus(
    val muscle: MuscleGroup,
    val phase: RecoveryPhase,
    val hoursSinceTrained: Float,
    val recoveryPercentage: Int
)

/**
 * Progression adaptation action determined by the Adaptive Engine.
 */
enum class ProgressionAction {
    OVERLOAD_INCREMENT,     // Performance exceeded target with high form quality -> +1 to +2 reps / +10s
    STABLE_CONSOLIDATE,     // Solid effort or difficult -> maintain target to build neurological consistency
    DELOAD_SAFETY_BACKOFF,  // Incomplete set, severe fatigue, or low form score -> deload -15-20% to avoid injury
    ADVANCE_TIER            // Multiple consecutive overload successes -> recommend advancing difficulty tier
}

/**
 * Result of evaluating athlete performance against progression rules.
 */
data class AdaptiveEvaluation(
    val action: ProgressionAction,
    val newTargetReps: Int,
    val newTargetSets: Int,
    val newTargetHoldSeconds: Int,
    val consecutiveSuccesses: Int,
    val consecutiveFailures: Int,
    val readinessScore: Int,
    val coachExplanation: String
)

/**
 * Analysis of training cadence and missed workouts.
 */
data class CadenceAnalysis(
    val daysSinceLastWorkout: Int,
    val isLayoff: Boolean,
    val isMissedCadence: Boolean,
    val volumeMultiplier: Float,
    val guidance: String
)

/**
 * Daily routine recommendation adapted to prior performance and recovery status.
 */
data class AdaptiveRoutineRecommendation(
    val routine: WorkoutRoutine,
    val explanation: String,
    val fatiguedMusclesAvoided: List<MuscleGroup>,
    val targetMuscles: List<MuscleGroup>
)

/**
 * Core Adaptive Engine implementing progressive overload, recovery safeguards,
 * and missed-workout/layoff adaptation.
 */
object AdaptiveEngine {

    /**
     * Evaluates a completed set with the athlete's RPE rating and camera biomechanics
     * to calculate the next target reps, sets, or isometric hold duration.
     */
    fun evaluateSetProgression(
        current: ExerciseProgressionEntity,
        summary: CompletedSetSummary,
        ratingString: String
    ): AdaptiveEvaluation {
        val formQuality = summary.formConsistencyPercent
        val isIsometric = current.targetHoldSeconds > 0
        val isHighForm = formQuality >= 80

        var newReps = current.targetReps
        var newHold = current.targetHoldSeconds
        var successes = current.consecutiveSuccesses
        var failures = current.consecutiveFailures
        val action: ProgressionAction
        val explanation: String

        when (ratingString) {
            "TOO_EASY" -> {
                if (isHighForm) {
                    successes++
                    failures = 0
                    if (successes >= 3 && current.currentDifficultyRank < 5) {
                        action = ProgressionAction.ADVANCE_TIER
                        explanation = "3 consecutive masteries! You are ready to advance to Tier ${current.currentDifficultyRank + 1}."
                    } else {
                        action = ProgressionAction.OVERLOAD_INCREMENT
                        if (isIsometric) {
                            newHold = min(120, current.targetHoldSeconds + 10)
                            explanation = "Overload unlocked! Holding +10s next session (${newHold}s)."
                        } else {
                            newReps = min(30, current.targetReps + 2)
                            explanation = "Target increased +2 reps (${newReps} reps) based on clean form and high ease."
                        }
                    }
                } else {
                    action = ProgressionAction.STABLE_CONSOLIDATE
                    explanation = "Reps felt light, but form consistency was $formQuality%. Maintaining target to refine technique."
                }
            }

            "JUST_RIGHT" -> {
                if (formQuality >= 85) {
                    successes++
                    failures = 0
                    action = ProgressionAction.OVERLOAD_INCREMENT
                    if (isIsometric) {
                        newHold = min(120, current.targetHoldSeconds + 5)
                        explanation = "High form excellence ($formQuality%). Progressive overload +5s hold (${newHold}s)."
                    } else {
                        newReps = min(30, current.targetReps + 1)
                        explanation = "Great stimulus & form ($formQuality%). Incremented +1 rep (${newReps} reps)."
                    }
                } else {
                    action = ProgressionAction.STABLE_CONSOLIDATE
                    explanation = "Target dialed in perfectly. Maintaining ${if (isIsometric) "${current.targetHoldSeconds}s" else "${current.targetReps} reps"} to build strength endurance."
                }
            }

            "DIFFICULT" -> {
                action = ProgressionAction.STABLE_CONSOLIDATE
                explanation = "High neuromuscular effort. Target maintained to consolidate adaptation without overreaching."
            }

            "COULD_NOT_COMPLETE" -> {
                failures++
                successes = 0
                action = ProgressionAction.DELOAD_SAFETY_BACKOFF
                if (isIsometric) {
                    newHold = max(15, (current.targetHoldSeconds * 0.80f).roundToInt())
                    explanation = "Safety deload: Hold adjusted to ${newHold}s to avoid joint strain and rebuild volume safely."
                } else {
                    newReps = max(4, current.targetReps - 2)
                    explanation = "Safety back-off: Target adjusted to $newReps reps to prioritize perfect biomechanics."
                }
            }

            else -> {
                action = ProgressionAction.STABLE_CONSOLIDATE
                explanation = "Maintaining current progression target."
            }
        }

        val updatedReadiness = when (action) {
            ProgressionAction.OVERLOAD_INCREMENT, ProgressionAction.ADVANCE_TIER -> min(100, current.readinessScore + 5)
            ProgressionAction.STABLE_CONSOLIDATE -> current.readinessScore
            ProgressionAction.DELOAD_SAFETY_BACKOFF -> max(50, current.readinessScore - 15)
        }

        return AdaptiveEvaluation(
            action = action,
            newTargetReps = newReps,
            newTargetSets = current.targetSets,
            newTargetHoldSeconds = newHold,
            consecutiveSuccesses = successes,
            consecutiveFailures = failures,
            readinessScore = updatedReadiness,
            coachExplanation = explanation
        )
    }

    /**
     * Calculates recovery state for all 9 major muscle groups based on recent workout history.
     */
    fun computeMuscleRecovery(
        sessions: List<WorkoutSessionEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): Map<MuscleGroup, MuscleRecoveryStatus> {
        val lastTrainedByMuscle = mutableMapOf<MuscleGroup, Long>()

        for (session in sessions) {
            val exercise = ExerciseCatalog.getById(session.exerciseId)
            val muscles = mutableListOf<MuscleGroup>()
            if (exercise != null) {
                muscles.add(exercise.targetMuscle)
                muscles.addAll(exercise.secondaryMuscles)
            } else {
                // Heuristic from exercise name if catalog lookup fails
                when {
                    session.exerciseName.contains("Push", ignoreCase = true) -> muscles.addAll(listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS))
                    session.exerciseName.contains("Squat", ignoreCase = true) -> muscles.addAll(listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES))
                    session.exerciseName.contains("Plank", ignoreCase = true) -> muscles.add(MuscleGroup.CORE)
                }
            }

            for (m in muscles) {
                val existing = lastTrainedByMuscle[m] ?: 0L
                if (session.timestampMs > existing) {
                    lastTrainedByMuscle[m] = session.timestampMs
                }
            }
        }

        val allMuscles = listOf(
            MuscleGroup.CHEST,
            MuscleGroup.QUADS,
            MuscleGroup.CORE,
            MuscleGroup.BACK,
            MuscleGroup.SHOULDERS,
            MuscleGroup.ARMS,
            MuscleGroup.GLUTES,
            MuscleGroup.HAMSTRINGS,
            MuscleGroup.CALVES
        )

        return allMuscles.associateWith { muscle ->
            val lastTrained = lastTrainedByMuscle[muscle]
            if (lastTrained == null || lastTrained == 0L) {
                MuscleRecoveryStatus(
                    muscle = muscle,
                    phase = RecoveryPhase.FULLY_RECOVERED,
                    hoursSinceTrained = 999f,
                    recoveryPercentage = 100
                )
            } else {
                val diffHours = max(0f, (nowMs - lastTrained) / (1000f * 60f * 60f))
                val phase = when {
                    diffHours < 24f -> RecoveryPhase.FATIGUED
                    diffHours < 48f -> RecoveryPhase.RECOVERING
                    else -> RecoveryPhase.FULLY_RECOVERED
                }

                val pct = when (phase) {
                    RecoveryPhase.FATIGUED -> min(65, (30 + (diffHours / 24f) * 35).roundToInt())
                    RecoveryPhase.RECOVERING -> min(90, (65 + ((diffHours - 24f) / 24f) * 25).roundToInt())
                    RecoveryPhase.FULLY_RECOVERED -> 100
                }

                MuscleRecoveryStatus(
                    muscle = muscle,
                    phase = phase,
                    hoursSinceTrained = diffHours,
                    recoveryPercentage = pct
                )
            }
        }
    }

    /**
     * Aggregate athlete training readiness score (0..100%).
     */
    fun calculateOverallReadiness(recoveryMap: Map<MuscleGroup, MuscleRecoveryStatus>): Int {
        if (recoveryMap.isEmpty()) return 100
        val avgPct = recoveryMap.values.map { it.recoveryPercentage }.average().roundToInt()
        return min(100, max(20, avgPct))
    }

    /**
     * Analyzes training cadence to detect missed workouts or long layoffs.
     */
    fun analyzeWorkoutCadence(
        latestWorkoutTimestampMs: Long?,
        nowMs: Long = System.currentTimeMillis()
    ): CadenceAnalysis {
        if (latestWorkoutTimestampMs == null || latestWorkoutTimestampMs == 0L) {
            return CadenceAnalysis(
                daysSinceLastWorkout = 0,
                isLayoff = false,
                isMissedCadence = false,
                volumeMultiplier = 1.0f,
                guidance = "Welcome! Ready to start your foundational training journey."
            )
        }

        val daysSince = max(0, ((nowMs - latestWorkoutTimestampMs) / (1000L * 60L * 60L * 24L)).toInt())

        return when {
            daysSince >= 8 -> {
                CadenceAnalysis(
                    daysSinceLastWorkout = daysSince,
                    isLayoff = true,
                    isMissedCadence = true,
                    volumeMultiplier = 0.80f,
                    guidance = "Re-entry Protocol: $daysSince days since last session. Target volume scaled -20% to prevent extreme soreness and rebuild groove."
                )
            }
            daysSince in 4..7 -> {
                CadenceAnalysis(
                    daysSinceLastWorkout = daysSince,
                    isLayoff = false,
                    isMissedCadence = true,
                    volumeMultiplier = 0.90f,
                    guidance = "Catch-Up Cadence: $daysSince days since last session. Rescheduled with slight volume buffer (-10%) to re-prime the system."
                )
            }
            else -> {
                CadenceAnalysis(
                    daysSinceLastWorkout = daysSince,
                    isLayoff = false,
                    isMissedCadence = false,
                    volumeMultiplier = 1.0f,
                    guidance = "Cadence on track! Primed for optimal performance."
                )
            }
        }
    }

    /**
     * Recommends a balanced workout routine adapted to recovery safeguards.
     * If the default routine stresses fatigued muscles (<24h), dynamically
     * selects a non-conflicting routine (e.g. Legs or Core) so the body recovers.
     */
    fun getRecommendedAdaptiveRoutine(
        recoveryMap: Map<MuscleGroup, MuscleRecoveryStatus>,
        defaultDifficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE
    ): AdaptiveRoutineRecommendation {
        val fatiguedMuscles = recoveryMap.filter { it.value.phase == RecoveryPhase.FATIGUED }.keys.toList()

        // Default routine is Total-Body Athletic Burn
        val defaultRoutine = WorkoutEngine.getDefaultTodayRoutine()

        // Check if default routine heavily stresses fatigued muscles
        val conflicts = defaultRoutine.targetMuscles.filter { it in fatiguedMuscles }

        if (conflicts.isEmpty()) {
            return AdaptiveRoutineRecommendation(
                routine = defaultRoutine,
                explanation = "All target muscle groups are fresh and recovered. Primed for full-body athletic training.",
                fatiguedMusclesAvoided = emptyList(),
                targetMuscles = defaultRoutine.targetMuscles
            )
        }

        // Recovery Safeguard Triggered: Find an alternative routine that avoids fatigued muscles
        val candidates = WorkoutEngine.getRoutines().filter { it.id != defaultRoutine.id }
        val bestAlternative = candidates.minByOrNull { routine ->
            routine.targetMuscles.count { it in fatiguedMuscles }
        } ?: defaultRoutine

        val avoidedStr = conflicts.joinToString(", ") { it.displayName }
        val explanation = "Recovery Safeguard Active: $avoidedStr trained recently and recovering. Recommended '${bestAlternative.name}' to balance recovery with stimulus."

        return AdaptiveRoutineRecommendation(
            routine = bestAlternative,
            explanation = explanation,
            fatiguedMusclesAvoided = conflicts,
            targetMuscles = bestAlternative.targetMuscles
        )
    }

    /**
     * Applies stored personal progressions and cadence volume multipliers
     * directly into the workout routine's item targets.
     */
    fun applyAdaptiveTargetsToRoutine(
        routine: WorkoutRoutine,
        progressions: Map<String, ExerciseProgressionEntity>,
        cadence: CadenceAnalysis
    ): WorkoutRoutine {
        val adaptedItems = routine.items.map { item ->
            val prog = progressions[item.exerciseId]
            if (prog != null) {
                val adaptedReps = if (prog.targetReps > 0) {
                    max(1, (prog.targetReps * cadence.volumeMultiplier).roundToInt())
                } else 0

                val adaptedHold = if (prog.targetHoldSeconds > 0) {
                    max(10, (prog.targetHoldSeconds * cadence.volumeMultiplier).roundToInt())
                } else 0

                item.copy(
                    targetReps = adaptedReps,
                    targetSets = prog.targetSets,
                    targetHoldSeconds = adaptedHold
                )
            } else {
                // Apply cadence multiplier to default
                val reps = if (item.targetReps > 0) max(1, (item.targetReps * cadence.volumeMultiplier).roundToInt()) else 0
                val hold = if (item.targetHoldSeconds > 0) max(10, (item.targetHoldSeconds * cadence.volumeMultiplier).roundToInt()) else 0
                item.copy(targetReps = reps, targetHoldSeconds = hold)
            }
        }

        return routine.copy(items = adaptedItems)
    }
}
