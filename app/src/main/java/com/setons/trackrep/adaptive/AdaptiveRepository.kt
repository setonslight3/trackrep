package com.setons.trackrep.adaptive

import android.content.Context
import com.setons.trackrep.coach.CompletedSetSummary
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.model.WorkoutRoutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AthleteReadinessState(
    val readinessPercentage: Int,
    val recoveryMap: Map<MuscleGroup, MuscleRecoveryStatus>,
    val cadence: CadenceAnalysis,
    val totalWorkouts: Int,
    val totalReps: Int
)

data class AdaptedWorkoutPlan(
    val routine: WorkoutRoutine,
    val adaptationReason: String,
    val cadenceGuidance: String,
    val isRecoverySafeguardActive: Boolean,
    val avoidedMuscles: List<MuscleGroup>
)

object AdaptiveRepository {

    private fun getDb(context: Context): TrackRepDatabase =
        TrackRepDatabase.getDatabase(context)

    suspend fun recordCompletedSet(
        context: Context,
        exerciseId: String,
        exerciseName: String,
        routineId: String? = null,
        routineName: String? = null,
        summary: CompletedSetSummary,
        ratingString: String,
        existingSessionId: String? = null
    ): AdaptiveEvaluation = withContext(Dispatchers.IO) {
        val db = getDb(context)
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(now))
        val sessionId = existingSessionId ?: UUID.randomUUID().toString()

        if (existingSessionId != null) {
            // Already recorded by CoachScreen! Just update the rating
            try {
                db.sessionDao().updatePerceivedRating(existingSessionId, ratingString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 1. Insert Session Record
            val sessionEntity = WorkoutSessionEntity(
                id = sessionId,
                routineId = routineId,
                routineName = routineName,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                timestampMs = now,
                dateString = dateStr,
                durationSeconds = summary.durationSeconds,
                totalValidReps = summary.validReps,
                totalPartialReps = summary.partialReps,
                averageFormScore = summary.formConsistencyPercent,
                fatigueVelocityLossPercent = 0f,
                perceivedRating = ratingString,
                isCompleted = true
            )
            db.sessionDao().insertSession(sessionEntity)

            // 2. Insert Set Record
            val setEntity = SetRecordEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = summary.setNumber,
                validReps = summary.validReps,
                partialReps = summary.partialReps,
                durationSeconds = summary.durationSeconds,
                averageDepthDegrees = summary.averageDepthDegrees,
                formConsistencyPercent = summary.formConsistencyPercent,
                fatigueLevel = summary.fatigueLevel.name
            )
            db.setRecordDao().insertSet(setEntity)
        }

        // 3. Load or initialize current progression
        val currentProg = db.progressionDao().getProgression(exerciseId) ?: run {
            val catalogEx = ExerciseCatalog.getById(exerciseId)
            ExerciseProgressionEntity(
                exerciseId = exerciseId,
                currentDifficultyRank = catalogEx?.difficulty?.rank ?: 3,
                targetReps = catalogEx?.defaultReps ?: 10,
                targetSets = catalogEx?.defaultSets ?: 3,
                targetHoldSeconds = catalogEx?.defaultHoldSeconds ?: 0
            )
        }

        // 4. Run Adaptive Progression Engine
        val eval = AdaptiveEngine.evaluateSetProgression(currentProg, summary, ratingString)

        // 5. Update Progression Entity
        val updatedPrReps = maxOf(currentProg.personalRecordReps, summary.validReps)
        val updatedPrHold = maxOf(currentProg.personalRecordHoldSeconds, if (currentProg.targetHoldSeconds > 0) summary.durationSeconds else 0)

        val updatedProg = currentProg.copy(
            targetReps = eval.newTargetReps,
            targetSets = eval.newTargetSets,
            targetHoldSeconds = eval.newTargetHoldSeconds,
            personalRecordReps = updatedPrReps,
            personalRecordHoldSeconds = updatedPrHold,
            lastTrainedTimestampMs = now,
            consecutiveSuccesses = eval.consecutiveSuccesses,
            consecutiveFailures = eval.consecutiveFailures,
            readinessScore = eval.readinessScore
        )
        db.progressionDao().upsertProgression(updatedProg)

        eval
    }

    suspend fun getAthleteReadiness(context: Context): AthleteReadinessState = withContext(Dispatchers.IO) {
        val db = getDb(context)
        val sessions = db.sessionDao().getAllSessions()
        val latestTimestamp = db.sessionDao().getLatestWorkoutTimestamp()
        val totalWorkouts = db.sessionDao().getTotalSessionCount()
        val totalReps = db.sessionDao().getTotalValidRepsCount()

        val recoveryMap = AdaptiveEngine.computeMuscleRecovery(sessions)
        val readinessPct = AdaptiveEngine.calculateOverallReadiness(recoveryMap)
        val cadence = AdaptiveEngine.analyzeWorkoutCadence(latestTimestamp)

        AthleteReadinessState(
            readinessPercentage = readinessPct,
            recoveryMap = recoveryMap,
            cadence = cadence,
            totalWorkouts = totalWorkouts,
            totalReps = totalReps
        )
    }

    suspend fun getAdaptedTodayWorkout(
        context: Context,
        scheduledRoutineId: String? = null,
        activeRoutine: WorkoutRoutine? = null
    ): AdaptedWorkoutPlan = withContext(Dispatchers.IO) {
        val db = getDb(context)
        val sessions = db.sessionDao().getAllSessions()
        val latestTimestamp = db.sessionDao().getLatestWorkoutTimestamp()
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Compute recovery excluding today's sessions so in-progress workouts are not interrupted
        val recoveryMap = AdaptiveEngine.computeMuscleRecovery(sessions, ignoreTodayDateString = todayDateStr)
        val cadence = AdaptiveEngine.analyzeWorkoutCadence(latestTimestamp)

        val baseRoutine = activeRoutine
            ?: scheduledRoutineId?.let { com.setons.trackrep.workout.WorkoutEngine.getRoutineById(it) }
            ?: com.setons.trackrep.workout.WorkoutEngine.getDefaultTodayRoutine()

        val rec = if (activeRoutine != null) {
            AdaptiveRoutineRecommendation(
                routine = activeRoutine,
                explanation = "Active session in progress: ${activeRoutine.name}",
                fatiguedMusclesAvoided = emptyList(),
                targetMuscles = activeRoutine.targetMuscles
            )
        } else {
            AdaptiveEngine.getRecommendedAdaptiveRoutine(recoveryMap, baseRoutine = baseRoutine)
        }

        val allProgs = db.progressionDao().getAllProgressions().associateBy { it.exerciseId }
        val adaptedRoutine = AdaptiveEngine.applyAdaptiveTargetsToRoutine(rec.routine, allProgs, cadence)

        AdaptedWorkoutPlan(
            routine = adaptedRoutine,
            adaptationReason = rec.explanation,
            cadenceGuidance = cadence.guidance,
            isRecoverySafeguardActive = rec.fatiguedMusclesAvoided.isNotEmpty(),
            avoidedMuscles = rec.fatiguedMusclesAvoided
        )
    }

    suspend fun getProgressionForExercise(context: Context, exerciseId: String): ExerciseProgressionEntity? = withContext(Dispatchers.IO) {
        getDb(context).progressionDao().getProgression(exerciseId)
    }

    suspend fun getRecentSessions(context: Context, limit: Int = 5): List<WorkoutSessionEntity> = withContext(Dispatchers.IO) {
        getDb(context).sessionDao().getRecentSessions(limit)
    }

    fun getAllSessionsFlow(context: Context): Flow<List<WorkoutSessionEntity>> {
        return getDb(context).sessionDao().getAllSessionsFlow()
    }

    suspend fun deduplicateDuplicateSessions(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = getDb(context)
            val sessions = db.sessionDao().getAllSessions()
            val toDelete = mutableListOf<String>()
            for (i in sessions.indices) {
                val s1 = sessions[i]
                for (j in i + 1 until sessions.size) {
                    val s2 = sessions[j]
                    if (s1.exerciseId == s2.exerciseId && kotlin.math.abs(s1.timestampMs - s2.timestampMs) < 120_000L) {
                        if (s1.videoPath != null && s2.videoPath == null) {
                            toDelete.add(s2.id)
                        } else if (s2.videoPath != null && s1.videoPath == null) {
                            toDelete.add(s1.id)
                        } else if (s1.videoPath == null && s2.videoPath == null) {
                            if (s1.totalValidReps >= s2.totalValidReps) {
                                toDelete.add(s2.id)
                            } else {
                                toDelete.add(s1.id)
                            }
                        }
                    }
                }
            }
            toDelete.distinct().forEach { id ->
                db.sessionDao().deleteSessionById(id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
