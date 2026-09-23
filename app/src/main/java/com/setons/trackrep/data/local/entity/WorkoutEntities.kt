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

