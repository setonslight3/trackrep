package com.setons.trackrep.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.setons.trackrep.data.local.dao.AIActionLogDao
import com.setons.trackrep.data.local.dao.ExerciseProgressionDao
import com.setons.trackrep.data.local.dao.SetRecordDao
import com.setons.trackrep.data.local.dao.WorkoutSessionDao
import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity

import com.setons.trackrep.data.local.dao.ScheduledWorkoutDao
import com.setons.trackrep.data.local.dao.UserProfileDao
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutSessionEntity::class,
        SetRecordEntity::class,
        ExerciseProgressionEntity::class,
        AIActionLogEntity::class,
        UserProfileEntity::class,
        ScheduledWorkoutEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrackRepDatabase : RoomDatabase() {

    abstract fun sessionDao(): WorkoutSessionDao
    abstract fun setRecordDao(): SetRecordDao
    abstract fun progressionDao(): ExerciseProgressionDao
    abstract fun aiActionLogDao(): AIActionLogDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: TrackRepDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` TEXT NOT NULL, `goal` TEXT NOT NULL, `fitnessLevel` TEXT NOT NULL, `equipment` TEXT NOT NULL, `customEquipment` TEXT NOT NULL, `availableDaysCsv` TEXT NOT NULL, `preferredTimeOfDay` TEXT NOT NULL, `reminderHour` INTEGER NOT NULL, `reminderMinute` INTEGER NOT NULL, `workoutDurationMinutes` INTEGER NOT NULL, `isCameraEnabled` INTEGER NOT NULL, `remindersEnabled` INTEGER NOT NULL, `isOnboardingCompleted` INTEGER NOT NULL, `createdAtTimestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `scheduled_workouts` (`id` TEXT NOT NULL, `dayOfWeek` TEXT NOT NULL, `dayIndex` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `routineId` TEXT NOT NULL, `routineName` TEXT NOT NULL, `targetMusclesCsv` TEXT NOT NULL, `isRestDay` INTEGER NOT NULL, `status` TEXT NOT NULL, `notes` TEXT NOT NULL, `timeOfDay` TEXT NOT NULL, PRIMARY KEY(`id`))")
                } catch (e: Exception) {
                    android.util.Log.e("TrackRepDatabase", "MIGRATION_1_2 error", e)
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workout_sessions ADD COLUMN videoPath TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.e("TrackRepDatabase", "MIGRATION_2_3 error", e)
                }
            }
        }

        fun getDatabase(context: Context): TrackRepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackRepDatabase::class.java,
                    "trackrep_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
