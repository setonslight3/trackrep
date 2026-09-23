package com.setons.trackrep.backup

import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity

/**
 * Top-level snapshot data container for TrackRep database export and import.
 */
data class TrackRepBackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appName: String = APP_NAME,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val exportedAtFormatted: String = "",
    val deviceInfo: String = "TrackRep Android",
    val userProfile: UserProfileEntity? = null,
    val workoutSessions: List<WorkoutSessionEntity> = emptyList(),
    val setRecords: List<SetRecordEntity> = emptyList(),
    val exerciseProgressions: List<ExerciseProgressionEntity> = emptyList(),
    val scheduledWorkouts: List<ScheduledWorkoutEntity> = emptyList(),
    val aiActionLogs: List<AIActionLogEntity> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val APP_NAME = "TrackRep"
    }
}

/**
 * Outcome of validating a raw backup JSON payload prior to database restoration.
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val schemaVersion: Int = 0,
    val sessionCount: Int = 0,
    val setRecordCount: Int = 0,
    val userProfileIncluded: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Summary metrics of an atomic backup restoration.
 */
data class BackupRestoreSummary(
    val sessionsRestored: Int,
    val setsRestored: Int,
    val progressionsRestored: Int,
    val schedulesRestored: Int,
    val profileRestored: Boolean,
    val logsRestored: Int
)
