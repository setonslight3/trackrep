package com.setons.trackrep.analytics

import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.exercise.model.Exercise
import com.setons.trackrep.exercise.model.MuscleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LifetimeStats(
    val totalWorkouts: Int,
    val totalValidReps: Int,
    val totalTrainingMinutes: Int,
    val averageFormAccuracy: Int
)

data class StreakStats(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val lastActiveDate: String?
)

data class ExercisePrMilestone(
    val exerciseId: String,
    val exerciseName: String,
    val isIsometric: Boolean,
    val personalRecordValue: Int,
    val personalRecordUnit: String,
    val currentDifficultyRank: Int,
    val targetValue: Int,
    val totalSessionsCompleted: Int,
    val lastTrainedTimestampMs: Long
)

data class MuscleVolumeStat(
    val muscleGroup: MuscleGroup,
    val reps: Int,
    val percentage: Float
)

data class ProgressDashboardData(
    val lifetime: LifetimeStats,
    val streaks: StreakStats,
    val milestones: List<ExercisePrMilestone>,
    val muscleDistribution: List<MuscleVolumeStat>
)

/**
 * Calculates lifetime progress analytics, consistency streaks, exercise PR milestones,
 * and muscle volume distributions from Room session and progression entities.
 */
object ProgressAnalyticsEngine {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun computeLifetimeStats(sessions: List<WorkoutSessionEntity>): LifetimeStats {
        if (sessions.isEmpty()) {
            return LifetimeStats(totalWorkouts = 0, totalValidReps = 0, totalTrainingMinutes = 0, averageFormAccuracy = 100)
        }

        val totalReps = sessions.sumOf { it.totalValidReps }
        val totalSeconds = sessions.sumOf { it.durationSeconds }
        val totalMinutes = (totalSeconds + 59) / 60
        val avgForm = sessions.map { it.averageFormScore }.average().toInt()

        return LifetimeStats(
            totalWorkouts = sessions.size,
            totalValidReps = totalReps,
            totalTrainingMinutes = totalMinutes,
            averageFormAccuracy = avgForm
        )
    }

    fun computeStreaks(
        sessions: List<WorkoutSessionEntity>,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): StreakStats {
        if (sessions.isEmpty()) {
            return StreakStats(currentStreakDays = 0, bestStreakDays = 0, lastActiveDate = null)
        }

        // Extract sorted unique training dates (yyyy-MM-dd)
        val activeDates = sessions
            .map { dayFormat.format(Date(it.timestampMs)) }
            .distinct()
            .sorted()

        if (activeDates.isEmpty()) {
            return StreakStats(0, 0, null)
        }

        val todayStr = dayFormat.format(referenceCalendar.time)
        val calYesterday = (referenceCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dayFormat.format(calYesterday.time)

        val lastActiveDate = activeDates.last()

        // 1. Calculate Best Streak across history
        var bestStreak = 0
        var tempStreak = 0
        var prevCal: Calendar? = null

        for (dateStr in activeDates) {
            val currCal = Calendar.getInstance().apply {
                time = dayFormat.parse(dateStr) ?: Date()
            }

            if (prevCal == null) {
                tempStreak = 1
            } else {
                val diffDays = (currCal.timeInMillis - prevCal.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) {
                    tempStreak++
                } else if (diffDays > 1L) {
                    tempStreak = 1
                }
            }
            if (tempStreak > bestStreak) {
                bestStreak = tempStreak
            }
            prevCal = currCal
        }

        // 2. Calculate Current Streak (must touch today or yesterday to be active)
        var currentStreak = 0
        if (lastActiveDate == todayStr || lastActiveDate == yesterdayStr) {
            val reverseDates = activeDates.reversed()
            var expectedCal = Calendar.getInstance().apply {
                time = dayFormat.parse(lastActiveDate) ?: Date()
            }

            for (dateStr in reverseDates) {
                val currCal = Calendar.getInstance().apply {
                    time = dayFormat.parse(dateStr) ?: Date()
                }
                val diffDays = (expectedCal.timeInMillis - currCal.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diffDays == 0L) {
                    currentStreak++
                    expectedCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        return StreakStats(
            currentStreakDays = currentStreak,
            bestStreakDays = maxOf(bestStreak, currentStreak),
            lastActiveDate = lastActiveDate
        )
    }

    private fun resolveExercise(exerciseId: String): Exercise? {
        ExerciseCatalog.getById(exerciseId)?.let { return it }

        val normalized = exerciseId.lowercase().replace("_", "").replace("-", "")
        val targetId = when (normalized) {
            "pushup", "pushups" -> "push_up_standard"
            "squat", "squats" -> "squat_bodyweight"
            "plank", "planks" -> "plank_standard"
            "pullup", "pullups" -> "pull_up_standard"
            else -> null
        }
        if (targetId != null) {
            ExerciseCatalog.getById(targetId)?.let { return it }
        }

        return ExerciseCatalog.getAll().find {
            val exNorm = it.id.lowercase().replace("_", "").replace("-", "")
            exNorm == normalized || exNorm.contains(normalized) || normalized.contains(exNorm)
        }
    }

    fun computeMilestones(
        progressions: List<ExerciseProgressionEntity>,
        sessions: List<WorkoutSessionEntity>
    ): List<ExercisePrMilestone> {
        val catalogExercises = ExerciseCatalog.getAll()
        val progressionMap = mutableMapOf<String, ExerciseProgressionEntity>()
        progressions.forEach { p ->
            progressionMap[p.exerciseId] = p
            resolveExercise(p.exerciseId)?.let { ex ->
                progressionMap[ex.id] = p
            }
        }

        val sessionCountMap = mutableMapOf<String, Int>()
        sessions.forEach { s ->
            val prev = sessionCountMap[s.exerciseId] ?: 0
            sessionCountMap[s.exerciseId] = prev + 1
            resolveExercise(s.exerciseId)?.let { ex ->
                if (ex.id != s.exerciseId) {
                    sessionCountMap[ex.id] = (sessionCountMap[ex.id] ?: 0) + 1
                }
            }
        }

        return catalogExercises.map { ex ->
            val prog = progressionMap[ex.id] ?: progressionMap[ex.id.replace("_", "")]
            val count = sessionCountMap[ex.id] ?: 0
            val isIsometric = ex.id.contains("plank") || ex.defaultHoldSeconds > 0

            val prValue = if (isIsometric) {
                prog?.personalRecordHoldSeconds ?: 0
            } else {
                prog?.personalRecordReps ?: 0
            }

            val targetValue = if (isIsometric) {
                prog?.targetHoldSeconds ?: ex.defaultHoldSeconds
            } else {
                prog?.targetReps ?: ex.defaultReps
            }

            ExercisePrMilestone(
                exerciseId = ex.id,
                exerciseName = ex.name,
                isIsometric = isIsometric,
                personalRecordValue = prValue,
                personalRecordUnit = if (isIsometric) "sec" else "reps",
                currentDifficultyRank = prog?.currentDifficultyRank ?: ex.difficulty.rank,
                targetValue = targetValue,
                totalSessionsCompleted = count,
                lastTrainedTimestampMs = prog?.lastTrainedTimestampMs ?: 0L
            )
        }
    }

    fun computeMuscleDistribution(sessions: List<WorkoutSessionEntity>): List<MuscleVolumeStat> {
        if (sessions.isEmpty()) {
            return MuscleGroup.entries.map { MuscleVolumeStat(it, 0, 0f) }
        }

        val volumeMap = mutableMapOf<MuscleGroup, Int>()
        MuscleGroup.entries.forEach { volumeMap[it] = 0 }

        sessions.forEach { session ->
            val ex = resolveExercise(session.exerciseId)
            val reps = maxOf(session.totalValidReps, if (session.exerciseId.contains("plank")) session.durationSeconds else 0)
            val muscles = if (ex != null) listOf(ex.targetMuscle) + ex.secondaryMuscles else emptyList()
            muscles.forEach { muscle ->
                volumeMap[muscle] = (volumeMap[muscle] ?: 0) + reps
            }
        }

        val totalReps = volumeMap.values.sum()
        if (totalReps == 0) {
            return MuscleGroup.entries.map { MuscleVolumeStat(it, 0, 0f) }
        }

        return MuscleGroup.entries.map { muscle ->
            val reps = volumeMap[muscle] ?: 0
            val pct = (reps.toFloat() / totalReps.toFloat()) * 100f
            MuscleVolumeStat(muscle, reps, pct)
        }.sortedByDescending { it.reps }
    }

    fun buildDashboardData(
        sessions: List<WorkoutSessionEntity>,
        progressions: List<ExerciseProgressionEntity>
    ): ProgressDashboardData {
        return ProgressDashboardData(
            lifetime = computeLifetimeStats(sessions),
            streaks = computeStreaks(sessions),
            milestones = computeMilestones(progressions, sessions),
            muscleDistribution = computeMuscleDistribution(sessions)
        )
    }
}
