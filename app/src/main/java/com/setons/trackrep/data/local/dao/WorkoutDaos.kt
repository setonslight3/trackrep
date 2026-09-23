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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSessionEntity>)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllSessions()

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

    @Query("SELECT * FROM set_records ORDER BY id ASC")
    suspend fun getAllSets(): List<SetRecordEntity>

    @Query("DELETE FROM set_records")
    suspend fun clearAllSets()
}

@Dao
interface ExerciseProgressionDao {
    @Query("SELECT * FROM exercise_progressions WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun getProgression(exerciseId: String): ExerciseProgressionEntity?

    @Query("SELECT * FROM exercise_progressions")
    suspend fun getAllProgressions(): List<ExerciseProgressionEntity>

    @Upsert
    suspend fun upsertProgression(progression: ExerciseProgressionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressions(progressions: List<ExerciseProgressionEntity>)

    @Query("DELETE FROM exercise_progressions")
    suspend fun clearAllProgressions()

    @Query("SELECT * FROM exercise_progressions ORDER BY lastTrainedTimestampMs DESC")
    suspend fun getRecentlyTrainedProgressions(): List<ExerciseProgressionEntity>
}

@Dao
interface AIActionLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AIActionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AIActionLogEntity>)

    @Query("SELECT * FROM ai_action_logs ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 10): List<AIActionLogEntity>

    @Query("SELECT * FROM ai_action_logs ORDER BY timestampMs DESC")
    suspend fun getAllLogs(): List<AIActionLogEntity>

    @Query("SELECT COUNT(*) FROM ai_action_logs")
    suspend fun getTotalLogCount(): Int

    @Query("DELETE FROM ai_action_logs")
    suspend fun clearAllLogs()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = "default_user"): com.setons.trackrep.data.local.entity.UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String = "default_user"): Flow<com.setons.trackrep.data.local.entity.UserProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: com.setons.trackrep.data.local.entity.UserProfileEntity)

    @Query("UPDATE user_profile SET isOnboardingCompleted = :completed WHERE id = :id")
    suspend fun setOnboardingCompleted(completed: Boolean, id: String = "default_user")

    @Query("SELECT isOnboardingCompleted FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun isOnboardingCompleted(id: String = "default_user"): Boolean?

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface ScheduledWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: List<com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity>)

    @Upsert
    suspend fun upsertWorkout(workout: com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity)

    @Query("SELECT * FROM scheduled_workouts ORDER BY dayIndex ASC")
    suspend fun getWeeklySchedule(): List<com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity>

    @Query("SELECT * FROM scheduled_workouts ORDER BY dayIndex ASC")
    suspend fun getAllScheduledWorkouts(): List<com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity>

    @Query("SELECT * FROM scheduled_workouts ORDER BY dayIndex ASC")
    fun getWeeklyScheduleFlow(): Flow<List<com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity>>

    @Query("SELECT * FROM scheduled_workouts WHERE dateString = :dateString LIMIT 1")
    suspend fun getWorkoutForDate(dateString: String): com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity?

    @Query("UPDATE scheduled_workouts SET status = :status WHERE dateString = :dateString")
    suspend fun updateStatusForDate(dateString: String, status: String)

    @Query("UPDATE scheduled_workouts SET status = :status WHERE id = :id")
    suspend fun updateStatusById(id: String, status: String)

    @Query("DELETE FROM scheduled_workouts")
    suspend fun clearSchedule()
}
