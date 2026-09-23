package com.setons.trackrep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY timestampMs DESC")
    fun getAllSessionsFlow(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestampMs DESC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE timestampMs >= :sinceMs ORDER BY timestampMs DESC")
    suspend fun getSessionsSince(sinceMs: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE exerciseId = :exerciseId ORDER BY timestampMs DESC")
    suspend fun getSessionsForExercise(exerciseId: String): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COALESCE(SUM(totalValidReps), 0) FROM workout_sessions")
    suspend fun getTotalValidRepsCount(): Int

    @Query("SELECT MAX(timestampMs) FROM workout_sessions")
    suspend fun getLatestWorkoutTimestamp(): Long?
}

@Dao
interface SetRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetRecordEntity>)

    @Query("SELECT * FROM set_records WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getSetsForSession(sessionId: String): List<SetRecordEntity>

    @Query("SELECT * FROM set_records WHERE exerciseId = :exerciseId ORDER BY id DESC")
    suspend fun getSetsForExercise(exerciseId: String): List<SetRecordEntity>
}

@Dao
interface ExerciseProgressionDao {
    @Query("SELECT * FROM exercise_progressions WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getProgression(exerciseId: String): ExerciseProgressionEntity?

    @Query("SELECT * FROM exercise_progressions")
    suspend fun getAllProgressions(): List<ExerciseProgressionEntity>

    @Upsert
    suspend fun upsertProgression(progression: ExerciseProgressionEntity)

    @Query("SELECT * FROM exercise_progressions ORDER BY lastTrainedTimestampMs DESC")
    suspend fun getRecentlyTrainedProgressions(): List<ExerciseProgressionEntity>
}

@Dao
interface AIActionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AIActionLogEntity)

    @Query("SELECT * FROM ai_action_logs ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 10): List<AIActionLogEntity>

    @Query("SELECT COUNT(*) FROM ai_action_logs")
    suspend fun getTotalLogCount(): Int
}
