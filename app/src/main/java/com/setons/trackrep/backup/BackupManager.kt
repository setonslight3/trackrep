package com.setons.trackrep.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.setons.trackrep.data.local.TrackRepDatabase
import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages versioned local JSON backup export, import, schema validation, and atomic Room restoration.
 */
object BackupManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Exports the entire local Room database to a formatted JSON string.
     */
    suspend fun exportBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = TrackRepDatabase.getDatabase(context)

        val profile = db.userProfileDao().getProfile()
        val sessions = db.sessionDao().getAllSessions()
        val sets = db.setRecordDao().getAllSets()
        val progressions = db.progressionDao().getAllProgressions()
        val schedules = db.scheduledWorkoutDao().getAllScheduledWorkouts()
        val logs = db.aiActionLogDao().getAllLogs()

        val payload = TrackRepBackupPayload(
            schemaVersion = TrackRepBackupPayload.CURRENT_SCHEMA_VERSION,
            appName = TrackRepBackupPayload.APP_NAME,
            exportedAtMs = System.currentTimeMillis(),
            exportedAtFormatted = dateFormat.format(Date()),
            deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})",
            userProfile = profile,
            workoutSessions = sessions,
            setRecords = sets,
            exerciseProgressions = progressions,
            scheduledWorkouts = schedules,
            aiActionLogs = logs
        )

        serializePayload(payload).toString(2)
    }

    /**
     * Validates a raw JSON backup string before attempting restoration.
     */
    fun validateBackup(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)

            if (!root.has("schemaVersion")) {
                return BackupValidationResult(isValid = false, errorMessage = "Missing schemaVersion header in backup JSON.")
            }
            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion > TrackRepBackupPayload.CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult(
                    isValid = false,
                    schemaVersion = schemaVersion,
                    errorMessage = "Unsupported schemaVersion $schemaVersion (this app supports up to ${TrackRepBackupPayload.CURRENT_SCHEMA_VERSION}). Please update TrackRep."
                )
            }
            if (schemaVersion < 1) {
                return BackupValidationResult(
                    isValid = false,
                    schemaVersion = schemaVersion,
                    errorMessage = "Invalid schemaVersion $schemaVersion."
                )
            }

            val appName = root.optString("appName", "")
            if (appName != TrackRepBackupPayload.APP_NAME) {
                return BackupValidationResult(
                    isValid = false,
                    schemaVersion = schemaVersion,
                    errorMessage = "Invalid application identifier '$appName'. Expected '${TrackRepBackupPayload.APP_NAME}'."
                )
            }

            if (!root.has("data")) {
                return BackupValidationResult(isValid = false, schemaVersion = schemaVersion, errorMessage = "Missing 'data' container in backup JSON.")
            }

            val data = root.getJSONObject("data")
            val sessionsArray = data.optJSONArray("workoutSessions") ?: JSONArray()
            val setsArray = data.optJSONArray("setRecords") ?: JSONArray()
            val hasProfile = data.has("userProfile") && !data.isNull("userProfile")

            BackupValidationResult(
                isValid = true,
                schemaVersion = schemaVersion,
                sessionCount = sessionsArray.length(),
                setRecordCount = setsArray.length(),
                userProfileIncluded = hasProfile
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Malformed JSON backup: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Parses a validated JSON backup string into a strongly-typed TrackRepBackupPayload.
     */
    fun parsePayload(jsonString: String): TrackRepBackupPayload {
        val root = JSONObject(jsonString)
        val schemaVersion = root.getInt("schemaVersion")
        val appName = root.optString("appName", TrackRepBackupPayload.APP_NAME)
        val exportedAtMs = root.optLong("exportedAtMs", System.currentTimeMillis())
        val exportedAtFormatted = root.optString("exportedAtFormatted", "")
        val deviceInfo = root.optString("deviceInfo", "Unknown")

        val data = root.getJSONObject("data")

        val profile = if (data.has("userProfile") && !data.isNull("userProfile")) {
            val pObj = data.getJSONObject("userProfile")
            UserProfileEntity(
                id = pObj.optString("id", "default_user"),
                goal = pObj.optString("goal", "Full-Body Muscle Development"),
                fitnessLevel = pObj.optString("fitnessLevel", "Beginner"),
                equipment = pObj.optString("equipment", "Bodyweight (No Equipment)"),
                customEquipment = pObj.optString("customEquipment", ""),
                availableDaysCsv = pObj.optString("availableDaysCsv", "MON,WED,FRI"),
                preferredTimeOfDay = pObj.optString("preferredTimeOfDay", "MORNING"),
                reminderHour = pObj.optInt("reminderHour", 7),
                reminderMinute = pObj.optInt("reminderMinute", 30),
                workoutDurationMinutes = pObj.optInt("workoutDurationMinutes", 20),
                isCameraEnabled = pObj.optBoolean("isCameraEnabled", true),
                remindersEnabled = pObj.optBoolean("remindersEnabled", true),
                isOnboardingCompleted = pObj.optBoolean("isOnboardingCompleted", false),
                createdAtTimestampMs = pObj.optLong("createdAtTimestampMs", System.currentTimeMillis())
            )
        } else null

        val sessions = mutableListOf<WorkoutSessionEntity>()
        val sessionsArray = data.optJSONArray("workoutSessions") ?: JSONArray()
        for (i in 0 until sessionsArray.length()) {
            val s = sessionsArray.getJSONObject(i)
            sessions.add(
                WorkoutSessionEntity(
                    id = s.getString("id"),
                    routineId = if (s.isNull("routineId")) null else s.optString("routineId"),
                    routineName = if (s.isNull("routineName")) null else s.optString("routineName"),
                    exerciseId = s.getString("exerciseId"),
                    exerciseName = s.getString("exerciseName"),
                    timestampMs = s.optLong("timestampMs", System.currentTimeMillis()),
                    dateString = s.optString("dateString", ""),
                    durationSeconds = s.optInt("durationSeconds", 0),
                    totalValidReps = s.optInt("totalValidReps", 0),
                    totalPartialReps = s.optInt("totalPartialReps", 0),
                    averageFormScore = s.optInt("averageFormScore", 100),
                    fatigueVelocityLossPercent = s.optDouble("fatigueVelocityLossPercent", 0.0).toFloat(),
                    perceivedRating = s.optString("perceivedRating", "JUST_RIGHT"),
                    isCompleted = s.optBoolean("isCompleted", true)
                )
            )
        }

        val sets = mutableListOf<SetRecordEntity>()
        val setsArray = data.optJSONArray("setRecords") ?: JSONArray()
        for (i in 0 until setsArray.length()) {
            val r = setsArray.getJSONObject(i)
            sets.add(
                SetRecordEntity(
                    id = r.getString("id"),
                    sessionId = r.getString("sessionId"),
                    exerciseId = r.getString("exerciseId"),
                    setNumber = r.optInt("setNumber", 1),
                    validReps = r.optInt("validReps", 0),
                    partialReps = r.optInt("partialReps", 0),
                    durationSeconds = r.optInt("durationSeconds", 0),
                    averageDepthDegrees = r.optDouble("averageDepthDegrees", 0.0).toFloat(),
                    formConsistencyPercent = r.optInt("formConsistencyPercent", 100),
                    fatigueLevel = r.optString("fatigueLevel", "NONE"),
                    flawsDetectedJson = r.optString("flawsDetectedJson", "[]")
                )
            )
        }

        val progressions = mutableListOf<ExerciseProgressionEntity>()
        val progArray = data.optJSONArray("exerciseProgressions") ?: JSONArray()
        for (i in 0 until progArray.length()) {
            val p = progArray.getJSONObject(i)
            progressions.add(
                ExerciseProgressionEntity(
                    exerciseId = p.getString("exerciseId"),
                    currentDifficultyRank = p.optInt("currentDifficultyRank", 3),
                    targetReps = p.optInt("targetReps", 10),
                    targetSets = p.optInt("targetSets", 3),
                    targetHoldSeconds = p.optInt("targetHoldSeconds", 0),
                    personalRecordReps = p.optInt("personalRecordReps", 0),
                    personalRecordHoldSeconds = p.optInt("personalRecordHoldSeconds", 0),
                    lastTrainedTimestampMs = p.optLong("lastTrainedTimestampMs", 0L),
                    consecutiveSuccesses = p.optInt("consecutiveSuccesses", 0),
                    consecutiveFailures = p.optInt("consecutiveFailures", 0),
                    readinessScore = p.optInt("readinessScore", 100)
                )
            )
        }

        val schedules = mutableListOf<ScheduledWorkoutEntity>()
        val schedArray = data.optJSONArray("scheduledWorkouts") ?: JSONArray()
        for (i in 0 until schedArray.length()) {
            val sc = schedArray.getJSONObject(i)
            schedules.add(
                ScheduledWorkoutEntity(
                    id = sc.getString("id"),
                    dayOfWeek = sc.getString("dayOfWeek"),
                    dayIndex = sc.optInt("dayIndex", 1),
                    dateString = sc.getString("dateString"),
                    routineId = sc.getString("routineId"),
                    routineName = sc.getString("routineName"),
                    targetMusclesCsv = sc.optString("targetMusclesCsv", ""),
                    isRestDay = sc.optBoolean("isRestDay", false),
                    status = sc.optString("status", "SCHEDULED"),
                    notes = sc.optString("notes", ""),
                    timeOfDay = sc.optString("timeOfDay", "MORNING")
                )
            )
        }

        val logs = mutableListOf<AIActionLogEntity>()
        val logsArray = data.optJSONArray("aiActionLogs") ?: JSONArray()
        for (i in 0 until logsArray.length()) {
            val l = logsArray.getJSONObject(i)
            logs.add(
                AIActionLogEntity(
                    id = l.getString("id"),
                    timestampMs = l.optLong("timestampMs", System.currentTimeMillis()),
                    requestPrompt = l.getString("requestPrompt"),
                    actionType = l.getString("actionType"),
                    actionDetailsJson = l.optString("actionDetailsJson", "{}"),
                    executionResult = l.optString("executionResult", ""),
                    isSuccess = l.optBoolean("isSuccess", true)
                )
            )
        }

        return TrackRepBackupPayload(
            schemaVersion = schemaVersion,
            appName = appName,
            exportedAtMs = exportedAtMs,
            exportedAtFormatted = exportedAtFormatted,
            deviceInfo = deviceInfo,
            userProfile = profile,
            workoutSessions = sessions,
            setRecords = sets,
            exerciseProgressions = progressions,
            scheduledWorkouts = schedules,
            aiActionLogs = logs
        )
    }

    /**
     * Atomically restores a backup payload into the Room database.
     */
    suspend fun restoreBackup(context: Context, payload: TrackRepBackupPayload): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val db = TrackRepDatabase.getDatabase(context)

        db.withTransaction {
            // 1. Wipe existing tables cleanly to guarantee integrity
            db.sessionDao().clearAllSessions()
            db.setRecordDao().clearAllSets()
            db.progressionDao().clearAllProgressions()
            db.scheduledWorkoutDao().clearSchedule()
            db.aiActionLogDao().clearAllLogs()
            db.userProfileDao().clearProfile()

            // 2. Insert payload data
            payload.userProfile?.let { db.userProfileDao().upsertProfile(it) }
            if (payload.workoutSessions.isNotEmpty()) db.sessionDao().insertSessions(payload.workoutSessions)
            if (payload.setRecords.isNotEmpty()) db.setRecordDao().insertSets(payload.setRecords)
            if (payload.exerciseProgressions.isNotEmpty()) db.progressionDao().insertProgressions(payload.exerciseProgressions)
            if (payload.scheduledWorkouts.isNotEmpty()) db.scheduledWorkoutDao().insertSchedule(payload.scheduledWorkouts)
            if (payload.aiActionLogs.isNotEmpty()) db.aiActionLogDao().insertLogs(payload.aiActionLogs)
        }

        BackupRestoreSummary(
            sessionsRestored = payload.workoutSessions.size,
            setsRestored = payload.setRecords.size,
            progressionsRestored = payload.exerciseProgressions.size,
            schedulesRestored = payload.scheduledWorkouts.size,
            profileRestored = payload.userProfile != null,
            logsRestored = payload.aiActionLogs.size
        )
    }

    /**
     * Writes backup JSON to an external file Uri (SAF).
     */
    suspend fun exportBackupToUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = exportBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: error("Failed to open output stream for uri: $uri")
        }
    }

    /**
     * Reads, validates, and restores a backup from an external file Uri (SAF).
     */
    suspend fun importBackupFromUri(context: Context, uri: Uri): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: error("Failed to open input stream for uri: $uri")

            val validation = validateBackup(json)
            if (!validation.isValid) {
                error(validation.errorMessage ?: "Invalid backup file")
            }

            val payload = parsePayload(json)
            restoreBackup(context, payload)
        }
    }

    private fun serializePayload(payload: TrackRepBackupPayload): JSONObject {
        val root = JSONObject()
        root.put("schemaVersion", payload.schemaVersion)
        root.put("appName", payload.appName)
        root.put("exportedAtMs", payload.exportedAtMs)
        root.put("exportedAtFormatted", payload.exportedAtFormatted)
        root.put("deviceInfo", payload.deviceInfo)

        val data = JSONObject()

        payload.userProfile?.let { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("goal", p.goal)
            pObj.put("fitnessLevel", p.fitnessLevel)
            pObj.put("equipment", p.equipment)
            pObj.put("customEquipment", p.customEquipment)
            pObj.put("availableDaysCsv", p.availableDaysCsv)
            pObj.put("preferredTimeOfDay", p.preferredTimeOfDay)
            pObj.put("reminderHour", p.reminderHour)
            pObj.put("reminderMinute", p.reminderMinute)
            pObj.put("workoutDurationMinutes", p.workoutDurationMinutes)
            pObj.put("isCameraEnabled", p.isCameraEnabled)
            pObj.put("remindersEnabled", p.remindersEnabled)
            pObj.put("isOnboardingCompleted", p.isOnboardingCompleted)
            pObj.put("createdAtTimestampMs", p.createdAtTimestampMs)
            data.put("userProfile", pObj)
        }

        val sessionsArray = JSONArray()
        payload.workoutSessions.forEach { s ->
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("routineId", s.routineId ?: JSONObject.NULL)
            sObj.put("routineName", s.routineName ?: JSONObject.NULL)
            sObj.put("exerciseId", s.exerciseId)
            sObj.put("exerciseName", s.exerciseName)
            sObj.put("timestampMs", s.timestampMs)
            sObj.put("dateString", s.dateString)
            sObj.put("durationSeconds", s.durationSeconds)
            sObj.put("totalValidReps", s.totalValidReps)
            sObj.put("totalPartialReps", s.totalPartialReps)
            sObj.put("averageFormScore", s.averageFormScore)
            sObj.put("fatigueVelocityLossPercent", s.fatigueVelocityLossPercent.toDouble())
            sObj.put("perceivedRating", s.perceivedRating)
            sObj.put("isCompleted", s.isCompleted)
            sessionsArray.put(sObj)
        }
        data.put("workoutSessions", sessionsArray)

        val setsArray = JSONArray()
        payload.setRecords.forEach { r ->
            val rObj = JSONObject()
            rObj.put("id", r.id)
            rObj.put("sessionId", r.sessionId)
            rObj.put("exerciseId", r.exerciseId)
            rObj.put("setNumber", r.setNumber)
            rObj.put("validReps", r.validReps)
            rObj.put("partialReps", r.partialReps)
            rObj.put("durationSeconds", r.durationSeconds)
            rObj.put("averageDepthDegrees", r.averageDepthDegrees.toDouble())
            rObj.put("formConsistencyPercent", r.formConsistencyPercent)
            rObj.put("fatigueLevel", r.fatigueLevel)
            rObj.put("flawsDetectedJson", r.flawsDetectedJson)
            setsArray.put(rObj)
        }
        data.put("setRecords", setsArray)

        val progArray = JSONArray()
        payload.exerciseProgressions.forEach { p ->
            val pObj = JSONObject()
            pObj.put("exerciseId", p.exerciseId)
            pObj.put("currentDifficultyRank", p.currentDifficultyRank)
            pObj.put("targetReps", p.targetReps)
            pObj.put("targetSets", p.targetSets)
            pObj.put("targetHoldSeconds", p.targetHoldSeconds)
            pObj.put("personalRecordReps", p.personalRecordReps)
            pObj.put("personalRecordHoldSeconds", p.personalRecordHoldSeconds)
            pObj.put("lastTrainedTimestampMs", p.lastTrainedTimestampMs)
            pObj.put("consecutiveSuccesses", p.consecutiveSuccesses)
            pObj.put("consecutiveFailures", p.consecutiveFailures)
            pObj.put("readinessScore", p.readinessScore)
            progArray.put(pObj)
        }
        data.put("exerciseProgressions", progArray)

        val schedArray = JSONArray()
        payload.scheduledWorkouts.forEach { sc ->
            val scObj = JSONObject()
            scObj.put("id", sc.id)
            scObj.put("dayOfWeek", sc.dayOfWeek)
            scObj.put("dayIndex", sc.dayIndex)
            scObj.put("dateString", sc.dateString)
            scObj.put("routineId", sc.routineId)
            scObj.put("routineName", sc.routineName)
            scObj.put("targetMusclesCsv", sc.targetMusclesCsv)
            scObj.put("isRestDay", sc.isRestDay)
            scObj.put("status", sc.status)
            scObj.put("notes", sc.notes)
            scObj.put("timeOfDay", sc.timeOfDay)
            schedArray.put(scObj)
        }
        data.put("scheduledWorkouts", schedArray)

        val logsArray = JSONArray()
        payload.aiActionLogs.forEach { l ->
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("timestampMs", l.timestampMs)
            lObj.put("requestPrompt", l.requestPrompt)
            lObj.put("actionType", l.actionType)
            lObj.put("actionDetailsJson", l.actionDetailsJson)
            lObj.put("executionResult", l.executionResult)
            lObj.put("isSuccess", l.isSuccess)
            logsArray.put(lObj)
        }
        data.put("aiActionLogs", logsArray)

        root.put("data", data)
        return root
    }
}
