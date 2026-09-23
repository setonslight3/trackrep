package com.setons.trackrep.schedule

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for athlete user profile, onboarding state, and weekly training schedules.
 */
class UserProfileRepository(context: Context) {

    private val db = TrackRepDatabase.getDatabase(context)
    private val profileDao = db.userProfileDao()
    private val scheduleDao = db.scheduledWorkoutDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        @Volatile
        private var INSTANCE: UserProfileRepository? = null

        fun getInstance(context: Context): UserProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserProfileRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun getProfile(): UserProfileEntity = withContext(Dispatchers.IO) {
        profileDao.getProfile() ?: UserProfileEntity().also { defaultProfile ->
            profileDao.upsertProfile(defaultProfile)
        }
    }

    suspend fun saveProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        profileDao.upsertProfile(profile)
    }

    suspend fun isOnboardingCompleted(): Boolean = withContext(Dispatchers.IO) {
        val profile = profileDao.getProfile()
        profile?.isOnboardingCompleted ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        profileDao.setOnboardingCompleted(completed)
    }

    suspend fun getWeeklySchedule(): List<ScheduledWorkoutEntity> = withContext(Dispatchers.IO) {
        val schedule = scheduleDao.getWeeklySchedule()
        if (schedule.isEmpty()) {
            val profile = getProfile()
            val generated = WorkoutScheduleEngine.generateCoherentFirstWeek(profile)
            scheduleDao.insertSchedule(generated)
            generated
        } else {
            schedule
        }
    }

    suspend fun saveWeeklySchedule(schedule: List<ScheduledWorkoutEntity>) = withContext(Dispatchers.IO) {
        scheduleDao.clearSchedule()
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun getTodayWorkout(): ScheduledWorkoutEntity? = withContext(Dispatchers.IO) {
        val todayStr = dateFormat.format(Date())
        scheduleDao.getWorkoutForDate(todayStr)
    }

    suspend fun markWorkoutCompleted(dateString: String) = withContext(Dispatchers.IO) {
        scheduleDao.updateStatusForDate(dateString, "COMPLETED")
    }

    suspend fun resolveMissedSession(
        missed: ScheduledWorkoutEntity,
        strategy: MissedSessionStrategy
    ): List<ScheduledWorkoutEntity> = withContext(Dispatchers.IO) {
        val currentWeek = scheduleDao.getWeeklySchedule()
        val todayStr = dateFormat.format(Date())
        val updated = WorkoutScheduleEngine.resolveMissedSession(missed, strategy, currentWeek, todayStr)
        saveWeeklySchedule(updated)
        updated
    }

    suspend fun resetOnboarding() = withContext(Dispatchers.IO) {
        val current = getProfile()
        profileDao.upsertProfile(current.copy(isOnboardingCompleted = false))
        scheduleDao.clearSchedule()
    }
}