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

@Database(
    entities = [
        WorkoutSessionEntity::class,
        SetRecordEntity::class,
        ExerciseProgressionEntity::class,
        AIActionLogEntity::class,
        UserProfileEntity::class,
        ScheduledWorkoutEntity::class
    ],
    version = 2,
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

        fun getDatabase(context: Context): TrackRepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackRepDatabase::class.java,
                    "trackrep_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
