package com.setons.trackrep.ai.service

import android.content.Context
import com.setons.trackrep.adaptive.AdaptiveRepository
import com.setons.trackrep.adaptive.RecoveryPhase
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.workout.WorkoutEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds structured JSON context to supply to Gemini AI.
 * Enforces rule: "Gemini receives structured context, not raw camera video by default."
 */
object TrackContextBuilder {

    suspend fun buildStructuredContext(context: Context): String {
        val json = JSONObject()

        // 1. Athlete Readiness & Muscle Recovery States
        val readiness = AdaptiveRepository.getAthleteReadiness(context)
        val readinessObj = JSONObject().apply {
            put("readinessPercentage", readiness.readinessPercentage)
            put("daysSinceLastWorkout", readiness.cadence.daysSinceLastWorkout)
            put("isLayoff", readiness.cadence.isLayoff)
            put("isMissedCadence", readiness.cadence.isMissedCadence)
            put("cadenceGuidance", readiness.cadence.guidance)
            put("totalWorkoutsCompleted", readiness.totalWorkouts)
            put("totalRepsLogged", readiness.totalReps)

            val recoveryArr = JSONArray()
            readiness.recoveryMap.forEach { (muscle, status) ->
                recoveryArr.put(JSONObject().apply {
                    put("muscle", muscle.name)
                    put("phase", status.phase.name)
                    put("hoursSinceTrained", "%.1f".format(status.hoursSinceTrained))
                    put("recoveryPercentage", status.recoveryPercentage)
                })
            }
            put("muscleRecovery", recoveryArr)
        }
        json.put("athleteReadiness", readinessObj)

        // 2. Active Workout Routine
        val activeRoutine = WorkoutEngine.getActiveOrTodayRoutine()
        val routineObj = JSONObject().apply {
            put("id", activeRoutine.id)
            put("name", activeRoutine.name)
            put("difficulty", activeRoutine.difficulty.name)
            put("estimatedMinutes", activeRoutine.estimatedMinutes)
            put("scheduledDateOffsetDays", WorkoutEngine.scheduledDateOffsetDays)

            val itemsArr = JSONArray()
            activeRoutine.items.forEach { item ->
                val ex = ExerciseCatalog.getById(item.exerciseId)
                itemsArr.put(JSONObject().apply {
                    put("exerciseId", item.exerciseId)
                    put("exerciseName", ex?.name ?: item.exerciseId)
                    put("targetMuscle", ex?.targetMuscle?.name ?: "UNKNOWN")
                    put("targetSets", item.targetSets)
                    put("targetReps", item.targetReps)
                    put("targetHoldSeconds", item.targetHoldSeconds)
                    put("restSeconds", item.restSeconds)
                    put("isVisionSupported", ex?.isVisionSupported ?: false)
                })
            }
            put("exercises", itemsArr)
        }
        json.put("activeRoutine", routineObj)

        // 3. Recent Workout Logs
        val recentSessions = AdaptiveRepository.getRecentSessions(context, limit = 15)
        val sessionsArr = JSONArray()
        recentSessions.forEach { session ->
            sessionsArr.put(JSONObject().apply {
                put("exerciseName", session.exerciseName)
                put("routineName", session.routineName ?: "Standard Workout")
                put("date", session.dateString)
                put("validReps", session.totalValidReps)
                put("partialReps", session.totalPartialReps)
                put("durationSeconds", session.durationSeconds)
                put("formScore", session.averageFormScore)
                put("rating", session.perceivedRating)
            })
        }
        json.put("recentSessions", sessionsArr)

        // 4. Athlete Profile & Fitness Tier
        try {
            val userRepo = com.setons.trackrep.schedule.UserProfileRepository.getInstance(context)
            val profile = userRepo.getProfile()
            val profileObj = JSONObject().apply {
                put("fitnessLevel", profile.fitnessLevel)
                put("goal", profile.goal)
                put("equipment", profile.equipment)
                put("availableDays", profile.availableDaysCsv)
                put("preferredTimeOfDay", profile.preferredTimeOfDay)
                put("workoutDurationMinutes", profile.workoutDurationMinutes)
            }
            json.put("athleteProfile", profileObj)

            // 5. Weekly Schedule & Planned Routines
            val schedule = userRepo.getWeeklySchedule()
            val scheduleArr = JSONArray()
            schedule.forEach { day ->
                val dayRoutine = if (!day.isRestDay) WorkoutEngine.getRoutineById(day.routineId) else null
                scheduleArr.put(JSONObject().apply {
                    put("dayOfWeek", day.dayOfWeek)
                    put("date", day.dateString)
                    put("isRestDay", day.isRestDay)
                    put("routineName", day.routineName)
                    put("routineId", day.routineId)
                    put("targetMuscles", day.targetMusclesCsv)
                    if (dayRoutine != null) {
                        put("difficulty", dayRoutine.difficulty.displayName)
                        put("estimatedMinutes", dayRoutine.estimatedMinutes)
                        val exList = JSONArray()
                        dayRoutine.items.forEach { itm ->
                            val ex = ExerciseCatalog.getById(itm.exerciseId)
                            exList.put(ex?.name ?: itm.exerciseId)
                        }
                        put("movements", exList)
                    }
                })
            }
            json.put("weeklySchedule", scheduleArr)
        } catch (_: Exception) {}

        return json.toString(2)
    }
}
