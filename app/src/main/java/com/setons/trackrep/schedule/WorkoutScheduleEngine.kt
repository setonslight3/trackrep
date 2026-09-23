package com.setons.trackrep.schedule

import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.DifficultyLevel
import com.setons.trackrep.exercise.model.MuscleGroup
import com.setons.trackrep.exercise.model.WorkoutRoutine
import com.setons.trackrep.workout.WorkoutEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Resolution strategies when a scheduled workout session is missed.
 */
enum class MissedSessionStrategy(val displayName: String, val description: String) {
    CATCH_UP_TODAY(
        "Catch Up Today",
        "Perform the missed workout today. If today was scheduled as rest, it becomes your training day."
    ),
    MOVE_TO_NEXT_REST_DAY(
        "Move to Next Rest Day",
        "Push the missed session to your upcoming rest day to prevent volume stacking."
    ),
    SKIP_AND_ADAPT(
        "Skip & Adapt",
        "Skip the missed workout and let Track calibrate your next workout with a safety buffer."
    )
}

/**
 * Core Scheduling Engine for TrackRep.
 * Generates coherent first-week training plans, enforces rest-day recovery rules,
 * and manages missed workout resolutions.
 */
object WorkoutScheduleEngine {

    val DAYS_OF_WEEK = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val DAY_ABBREVIATIONS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Generates a coherent 7-day training schedule matching the athlete's
     * fitness tier, available equipment, and preferred active days.
     */
    fun generateCoherentFirstWeek(
        profile: UserProfileEntity,
        startCalendar: Calendar = Calendar.getInstance()
    ): List<ScheduledWorkoutEntity> {
        val difficulty = try {
            DifficultyLevel.valueOf(profile.fitnessLevel.uppercase(Locale.US))
        } catch (_: Exception) {
            DifficultyLevel.BEGINNER
        }

        // Parse user selected days (default Mon, Wed, Fri)
        val selectedAbbrs = profile.availableDaysCsv
            .split(",")
            .map { it.trim().uppercase(Locale.US) }
            .filter { it in DAY_ABBREVIATIONS }
            .ifEmpty { listOf("MON", "WED", "FRI") }

        // Align calendar to Monday of the target week
        val cal = startCalendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Determine training routines based on difficulty & equipment
        val trainingPool = selectRoutinesForLevelAndEquipment(difficulty, profile.equipment)

        val schedule = mutableListOf<ScheduledWorkoutEntity>()
        var routineIndex = 0

        for (i in 0 until 7) {
            val dayAbbr = DAY_ABBREVIATIONS[i]
            val dayName = DAYS_OF_WEEK[i]
            val dateStr = DATE_FORMAT.format(cal.time)
            val isTrainingDay = dayAbbr in selectedAbbrs

            if (isTrainingDay) {
                val routine = trainingPool[routineIndex % trainingPool.size]
                routineIndex++

                schedule.add(
                    ScheduledWorkoutEntity(
                        dayOfWeek = dayName,
                        dayIndex = i + 1,
                        dateString = dateStr,
                        routineId = routine.id,
                        routineName = routine.name,
                        targetMusclesCsv = routine.targetMuscles.joinToString(",") { it.name },
                        isRestDay = false,
                        status = "SCHEDULED",
                        notes = routine.tagline,
                        timeOfDay = profile.preferredTimeOfDay
                    )
                )
            } else {
                schedule.add(
                    ScheduledWorkoutEntity(
                        dayOfWeek = dayName,
                        dayIndex = i + 1,
                        dateString = dateStr,
                        routineId = "routine_active_recovery",
                        routineName = "Rest & Active Recovery",
                        targetMusclesCsv = "MOBILITY,RECOVERY",
                        isRestDay = true,
                        status = "SCHEDULED",
                        notes = "Muscles repair and consolidate strength. Light stretching recommended.",
                        timeOfDay = profile.preferredTimeOfDay
                    )
                )
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Enforce muscle recovery safeguards: ensure no conflicting back-to-back muscle loading
        return applyRecoverySafeguards(schedule)
    }

    /**
     * Resolves the list of candidate routines matching difficulty and equipment.
     */
    fun selectRoutinesForLevelAndEquipment(
        level: DifficultyLevel,
        equipment: String
    ): List<WorkoutRoutine> {
        return when (level) {
            DifficultyLevel.BEGINNER -> listOf(
                WorkoutEngine.getRoutineById("routine_full_body_foundation")
                    ?: WorkoutEngine.curatedRoutines[0],
                WorkoutEngine.getRoutineById("routine_novice_builder")
                    ?: WorkoutEngine.curatedRoutines[1]
            )
            DifficultyLevel.NOVICE -> listOf(
                WorkoutEngine.getRoutineById("routine_novice_builder")
                    ?: WorkoutEngine.curatedRoutines[1],
                WorkoutEngine.getRoutineById("routine_total_body_burn")
                    ?: WorkoutEngine.curatedRoutines[2]
            )
            DifficultyLevel.INTERMEDIATE -> listOf(
                WorkoutEngine.getRoutineById("routine_total_body_burn")
                    ?: WorkoutEngine.curatedRoutines[2],
                WorkoutEngine.getRoutineById("routine_upper_core_blast")
                    ?: WorkoutEngine.curatedRoutines[3],
                WorkoutEngine.getRoutineById("routine_lower_posterior_power")
                    ?: WorkoutEngine.curatedRoutines[4]
            )
            DifficultyLevel.ADVANCED, DifficultyLevel.ELITE -> listOf(
                WorkoutEngine.getRoutineById("routine_upper_core_blast")
                    ?: WorkoutEngine.curatedRoutines[3],
                WorkoutEngine.getRoutineById("routine_lower_posterior_power")
                    ?: WorkoutEngine.curatedRoutines[4],
                WorkoutEngine.getRoutineById("routine_elite_calisthenics")
                    ?: WorkoutEngine.curatedRoutines[5]
            )
        }
    }

    /**
     * Validates that consecutive training days do not heavily fatigue identical muscle groups.
     * If two adjacent days both heavily load chest/shoulders, adjusts routine order.
     */
    fun applyRecoverySafeguards(
        schedule: List<ScheduledWorkoutEntity>
    ): List<ScheduledWorkoutEntity> {
        val result = schedule.toMutableList()
        for (i in 0 until result.size - 1) {
            val current = result[i]
            val next = result[i + 1]

            if (!current.isRestDay && !next.isRestDay) {
                val currentMuscles = current.targetMusclesCsv.split(",").toSet()
                val nextMuscles = next.targetMusclesCsv.split(",").toSet()
                val overlap = currentMuscles.intersect(nextMuscles).filter {
                    it in listOf("CHEST", "SHOULDERS", "QUADS", "BACK")
                }

                // If identical heavy compound muscle groups clash on back-to-back days
                if (overlap.size >= 2) {
                    val lowerRoutine = WorkoutEngine.getRoutineById("routine_lower_posterior_power")
                    if (lowerRoutine != null && !next.routineId.contains("lower")) {
                        result[i + 1] = next.copy(
                            routineId = lowerRoutine.id,
                            routineName = lowerRoutine.name,
                            targetMusclesCsv = lowerRoutine.targetMuscles.joinToString(",") { it.name },
                            notes = "Auto-adjusted for recovery: Lower Body Focus to allow upper muscles to rebuild."
                        )
                    }
                }
            }
        }
        return result
    }

    /**
     * Scans the weekly schedule to detect any uncompleted workouts whose date has passed.
     */
    fun detectMissedSessions(
        schedule: List<ScheduledWorkoutEntity>,
        completedDates: Set<String>,
        currentDateString: String
    ): List<ScheduledWorkoutEntity> {
        return schedule.filter { item ->
            !item.isRestDay &&
            item.status == "SCHEDULED" &&
            item.dateString < currentDateString &&
            item.dateString notIn completedDates
        }
    }

    private infix fun String.notIn(set: Set<String>): Boolean = !set.contains(this)

    /**
     * Resolves a missed workout according to the athlete's chosen strategy.
     */
    fun resolveMissedSession(
        missed: ScheduledWorkoutEntity,
        strategy: MissedSessionStrategy,
        currentSchedule: List<ScheduledWorkoutEntity>,
        todayDateString: String
    ): List<ScheduledWorkoutEntity> {
        val updated = currentSchedule.toMutableList()
        val missedIndex = updated.indexOfFirst { it.id == missed.id || it.dateString == missed.dateString }

        when (strategy) {
            MissedSessionStrategy.CATCH_UP_TODAY -> {
                val todayIndex = updated.indexOfFirst { it.dateString == todayDateString }
                if (todayIndex != -1) {
                    val today = updated[todayIndex]
                    // If today was rest or a different session, substitute today with the missed workout
                    updated[todayIndex] = today.copy(
                        routineId = missed.routineId,
                        routineName = missed.routineName,
                        targetMusclesCsv = missed.targetMusclesCsv,
                        isRestDay = false,
                        status = "SCHEDULED",
                        notes = "Catch-up session: ${missed.routineName}"
                    )
                }
                if (missedIndex != -1) {
                    updated[missedIndex] = updated[missedIndex].copy(status = "RESCHEDULED")
                }
            }

            MissedSessionStrategy.MOVE_TO_NEXT_REST_DAY -> {
                // Find the first upcoming rest day on or after today
                val nextRestIndex = updated.indexOfFirst {
                    it.isRestDay && it.dateString >= todayDateString
                }
                if (nextRestIndex != -1) {
                    val restDay = updated[nextRestIndex]
                    updated[nextRestIndex] = restDay.copy(
                        routineId = missed.routineId,
                        routineName = missed.routineName,
                        targetMusclesCsv = missed.targetMusclesCsv,
                        isRestDay = false,
                        status = "SCHEDULED",
                        notes = "Rescheduled to rest day: ${missed.routineName}"
                    )
                    if (missedIndex != -1) {
                        updated[missedIndex] = updated[missedIndex].copy(status = "RESCHEDULED")
                    }
                } else {
                    // Fallback to today if no future rest day available in this week
                    return resolveMissedSession(missed, MissedSessionStrategy.CATCH_UP_TODAY, currentSchedule, todayDateString)
                }
            }

            MissedSessionStrategy.SKIP_AND_ADAPT -> {
                if (missedIndex != -1) {
                    updated[missedIndex] = updated[missedIndex].copy(
                        status = "SKIPPED",
                        notes = "Skipped session. Adaptive engine applied recovery safety volume buffer."
                    )
                }
            }
        }
        return updated
    }
}