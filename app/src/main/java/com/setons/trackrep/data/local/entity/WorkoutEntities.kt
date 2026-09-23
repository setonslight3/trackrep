package com.setons.trackrep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persisted record of an entire completed workout session.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val routineId: String? = null,
    val routineName: String? = null,
    val exerciseId: String,
    val exerciseName: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val dateString: String,
    val durationSeconds: Int,
    val totalValidReps: Int,
    val totalPartialReps: Int,
    val averageFormScore: Int,
    val fatigueVelocityLossPercent: Float,
    val perceivedRating: String, // TOO_EASY, JUST_RIGHT, DIFFICULT, COULD_NOT_COMPLETE
    val isCompleted: Boolean = true
)

/**
 * Persisted record of an individual set within a session.
 */
@Entity(
    tableName = "set_records",
    indices = [Index(value = ["sessionId"])]
)
data class SetRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val validReps: Int,
    val partialReps: Int,
    val durationSeconds: Int,
    val averageDepthDegrees: Float,
    val formConsistencyPercent: Int,
    val fatigueLevel: String,
    val flawsDetectedJson: String = "[]"
)

/**
 * Persistent adaptive progression state for a specific exercise.
 * Tracks progressive overload, deload triggers, and personal records.
 */
@Entity(tableName = "exercise_progressions")
data class ExerciseProgressionEntity(
    @PrimaryKey
    val exerciseId: String,
    val currentDifficultyRank: Int = 3, // 1..5
    val targetReps: Int = 10,
    val targetSets: Int = 3,
    val targetHoldSeconds: Int = 0,
    val personalRecordReps: Int = 0,
    val personalRecordHoldSeconds: Int = 0,
    val lastTrainedTimestampMs: Long = 0L,
    val consecutiveSuccesses: Int = 0,
    val consecutiveFailures: Int = 0,
    val readinessScore: Int = 100 // 0..100
)

/**
 * Persisted audit log of all AI actions executed by Track.
 */
@Entity(tableName = "ai_action_logs")
data class AIActionLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val requestPrompt: String,
    val actionType: String,
    val actionDetailsJson: String,
    val executionResult: String,
    val isSuccess: Boolean = true
)

/**
 * Persisted profile and scheduling preferences for the athlete.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String = "default_user",
    val goal: String = "Full-Body Muscle Development",
    val fitnessLevel: String = "Beginner",
    val equipment: String = "Bodyweight (No Equipment)",
    val customEquipment: String = "",
    val availableDaysCsv: String = "MON,WED,FRI",
    val preferredTimeOfDay: String = "MORNING",
    val reminderHour: Int = 7,
    val reminderMinute: Int = 30,
    val workoutDurationMinutes: Int = 20,
    val isCameraEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val createdAtTimestampMs: Long = System.currentTimeMillis()
)

/**
 * Persisted scheduled workout for a specific day in the training plan.
 */
@Entity(tableName = "scheduled_workouts")
data class ScheduledWorkoutEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: String, // MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    val dayIndex: Int, // 1 = Monday .. 7 = Sunday
    val dateString: String, // e.g. "2026-09-24"
    val routineId: String,
    val routineName: String,
    val targetMusclesCsv: String,
    val isRestDay: Boolean = false,
    val status: String = "SCHEDULED", // SCHEDULED, COMPLETED, MISSED, SKIPPED, RESCHEDULED
    val notes: String = "",
    val timeOfDay: String = "MORNING"
)

