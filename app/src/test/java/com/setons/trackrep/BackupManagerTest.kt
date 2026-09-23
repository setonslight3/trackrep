package com.setons.trackrep

import com.setons.trackrep.backup.BackupManager
import com.setons.trackrep.backup.TrackRepBackupPayload
import com.setons.trackrep.data.local.entity.AIActionLogEntity
import com.setons.trackrep.data.local.entity.ExerciseProgressionEntity
import com.setons.trackrep.data.local.entity.ScheduledWorkoutEntity
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.UserProfileEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun testBackupValidation_ValidPayload() {
        val validJson = "{\n" +
            "  \"schemaVersion\": 1,\n" +
            "  \"appName\": \"TrackRep\",\n" +
            "  \"exportedAtMs\": 1774384200000,\n" +
            "  \"exportedAtFormatted\": \"2026-09-23 20:30:00\",\n" +
            "  \"deviceInfo\": \"Infinix Smart 9\",\n" +
            "  \"data\": {\n" +
            "    \"userProfile\": {\n" +
            "      \"id\": \"default_user\",\n" +
            "      \"goal\": \"Full-Body Muscle Development\",\n" +
            "      \"fitnessLevel\": \"Intermediate\",\n" +
            "      \"equipment\": \"Bodyweight (No Equipment)\",\n" +
            "      \"customEquipment\": \"\",\n" +
            "      \"availableDaysCsv\": \"MON,WED,FRI\",\n" +
            "      \"preferredTimeOfDay\": \"MORNING\",\n" +
            "      \"reminderHour\": 7,\n" +
            "      \"reminderMinute\": 30,\n" +
            "      \"workoutDurationMinutes\": 25,\n" +
            "      \"isCameraEnabled\": true,\n" +
            "      \"remindersEnabled\": true,\n" +
            "      \"isOnboardingCompleted\": true,\n" +
            "      \"createdAtTimestampMs\": 1774380000000\n" +
            "    },\n" +
            "    \"workoutSessions\": [\n" +
            "      {\n" +
            "        \"id\": \"sess_1\",\n" +
            "        \"routineId\": \"foundation\",\n" +
            "        \"routineName\": \"Full Body Foundation\",\n" +
            "        \"exerciseId\": \"pushup\",\n" +
            "        \"exerciseName\": \"Standard Push-Up\",\n" +
            "        \"timestampMs\": 1774384000000,\n" +
            "        \"dateString\": \"2026-09-23\",\n" +
            "        \"durationSeconds\": 180,\n" +
            "        \"totalValidReps\": 30,\n" +
            "        \"totalPartialReps\": 2,\n" +
            "        \"averageFormScore\": 92,\n" +
            "        \"fatigueVelocityLossPercent\": 5.0,\n" +
            "        \"perceivedRating\": \"JUST_RIGHT\",\n" +
            "        \"isCompleted\": true\n" +
            "      }\n" +
            "    ],\n" +
            "    \"setRecords\": [\n" +
            "      {\n" +
            "        \"id\": \"set_1\",\n" +
            "        \"sessionId\": \"sess_1\",\n" +
            "        \"exerciseId\": \"pushup\",\n" +
            "        \"setNumber\": 1,\n" +
            "        \"validReps\": 15,\n" +
            "        \"partialReps\": 1,\n" +
            "        \"durationSeconds\": 60,\n" +
            "        \"averageDepthDegrees\": 88.5,\n" +
            "        \"formConsistencyPercent\": 94,\n" +
            "        \"fatigueLevel\": \"LOW\",\n" +
            "        \"flawsDetectedJson\": \"[]\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"exerciseProgressions\": [],\n" +
            "    \"scheduledWorkouts\": [],\n" +
            "    \"aiActionLogs\": []\n" +
            "  }\n" +
            "}"

        val result = BackupManager.validateBackup(validJson)
        assertTrue("Validation should succeed for conforming backup", result.isValid)
        assertEquals(1, result.schemaVersion)
        assertEquals(1, result.sessionCount)
        assertEquals(1, result.setRecordCount)
        assertTrue(result.userProfileIncluded)
    }

    @Test
    fun testBackupValidation_RejectsInvalidJson() {
        val corruptedJson = "{ this is definitely not valid json }"
        val result = BackupManager.validateBackup(corruptedJson)
        assertFalse("Malformed JSON must be rejected", result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testBackupValidation_RejectsWrongAppName() {
        val wrongAppJson = "{\n" +
            "  \"schemaVersion\": 1,\n" +
            "  \"appName\": \"OtherFitnessApp\",\n" +
            "  \"data\": {}\n" +
            "}"

        val result = BackupManager.validateBackup(wrongAppJson)
        assertFalse("Foreign app backup must be rejected", result.isValid)
        assertTrue("Error message mentions expected app name", result.errorMessage?.contains("TrackRep") == true)
    }

    @Test
    fun testBackupValidation_RejectsUnsupportedSchemaVersion() {
        val futureVersionJson = "{\n" +
            "  \"schemaVersion\": 999,\n" +
            "  \"appName\": \"TrackRep\",\n" +
            "  \"data\": {}\n" +
            "}"

        val result = BackupManager.validateBackup(futureVersionJson)
        assertFalse("Future schema version must be rejected safely", result.isValid)
        assertTrue("Error message mentions unsupported version", result.errorMessage?.contains("Unsupported schemaVersion 999") == true)
    }

    @Test
    fun testRoundTrip_ParsePayload() {
        val jsonString = "{\n" +
            "  \"schemaVersion\": 1,\n" +
            "  \"appName\": \"TrackRep\",\n" +
            "  \"exportedAtMs\": 1774384200000,\n" +
            "  \"exportedAtFormatted\": \"2026-09-23 20:30:00\",\n" +
            "  \"deviceInfo\": \"Test Device\",\n" +
            "  \"data\": {\n" +
            "    \"userProfile\": {\n" +
            "      \"id\": \"default_user\",\n" +
            "      \"goal\": \"Strength\",\n" +
            "      \"fitnessLevel\": \"Advanced\",\n" +
            "      \"equipment\": \"Bodyweight (No Equipment)\",\n" +
            "      \"customEquipment\": \"\",\n" +
            "      \"availableDaysCsv\": \"MON,WED,FRI\",\n" +
            "      \"preferredTimeOfDay\": \"MORNING\",\n" +
            "      \"reminderHour\": 7,\n" +
            "      \"reminderMinute\": 30,\n" +
            "      \"workoutDurationMinutes\": 20,\n" +
            "      \"isCameraEnabled\": true,\n" +
            "      \"remindersEnabled\": true,\n" +
            "      \"isOnboardingCompleted\": true,\n" +
            "      \"createdAtTimestampMs\": 1774380000000\n" +
            "    },\n" +
            "    \"workoutSessions\": [\n" +
            "      {\n" +
            "        \"id\": \"session_roundtrip\",\n" +
            "        \"routineId\": null,\n" +
            "        \"routineName\": null,\n" +
            "        \"exerciseId\": \"pushup\",\n" +
            "        \"exerciseName\": \"Push-Up\",\n" +
            "        \"timestampMs\": 1774384200000,\n" +
            "        \"dateString\": \"2026-09-23\",\n" +
            "        \"durationSeconds\": 120,\n" +
            "        \"totalValidReps\": 25,\n" +
            "        \"totalPartialReps\": 0,\n" +
            "        \"averageFormScore\": 95,\n" +
            "        \"fatigueVelocityLossPercent\": 0.0,\n" +
            "        \"perceivedRating\": \"JUST_RIGHT\",\n" +
            "        \"isCompleted\": true\n" +
            "      }\n" +
            "    ],\n" +
            "    \"setRecords\": [\n" +
            "      {\n" +
            "        \"id\": \"set_roundtrip\",\n" +
            "        \"sessionId\": \"session_roundtrip\",\n" +
            "        \"exerciseId\": \"pushup\",\n" +
            "        \"setNumber\": 1,\n" +
            "        \"validReps\": 25,\n" +
            "        \"partialReps\": 0,\n" +
            "        \"durationSeconds\": 120,\n" +
            "        \"averageDepthDegrees\": 90.0,\n" +
            "        \"formConsistencyPercent\": 95,\n" +
            "        \"fatigueLevel\": \"NONE\",\n" +
            "        \"flawsDetectedJson\": \"[]\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"exerciseProgressions\": [\n" +
            "      {\n" +
            "        \"exerciseId\": \"pushup\",\n" +
            "        \"currentDifficultyRank\": 3,\n" +
            "        \"targetReps\": 10,\n" +
            "        \"targetSets\": 3,\n" +
            "        \"targetHoldSeconds\": 0,\n" +
            "        \"personalRecordReps\": 25,\n" +
            "        \"personalRecordHoldSeconds\": 0,\n" +
            "        \"lastTrainedTimestampMs\": 0,\n" +
            "        \"consecutiveSuccesses\": 0,\n" +
            "        \"consecutiveFailures\": 0,\n" +
            "        \"readinessScore\": 100\n" +
            "      }\n" +
            "    ],\n" +
            "    \"scheduledWorkouts\": [\n" +
            "      {\n" +
            "        \"id\": \"sched_1\",\n" +
            "        \"dayOfWeek\": \"MONDAY\",\n" +
            "        \"dayIndex\": 1,\n" +
            "        \"dateString\": \"2026-09-21\",\n" +
            "        \"routineId\": \"foundation\",\n" +
            "        \"routineName\": \"Full Body Foundation\",\n" +
            "        \"targetMusclesCsv\": \"CHEST,CORE\",\n" +
            "        \"isRestDay\": false,\n" +
            "        \"status\": \"SCHEDULED\",\n" +
            "        \"notes\": \"\",\n" +
            "        \"timeOfDay\": \"MORNING\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"aiActionLogs\": [\n" +
            "      {\n" +
            "        \"id\": \"log_1\",\n" +
            "        \"timestampMs\": 1774384200000,\n" +
            "        \"requestPrompt\": \"Show stats\",\n" +
            "        \"actionType\": \"QUERY_STATS\",\n" +
            "        \"actionDetailsJson\": \"{}\",\n" +
            "        \"executionResult\": \"Success\",\n" +
            "        \"isSuccess\": true\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}"

        val parsed = BackupManager.parsePayload(jsonString)
        assertEquals(1, parsed.schemaVersion)
        assertEquals("TrackRep", parsed.appName)
        assertEquals("Advanced", parsed.userProfile?.fitnessLevel)
        assertEquals(1, parsed.workoutSessions.size)
        assertEquals("session_roundtrip", parsed.workoutSessions[0].id)
        assertEquals(25, parsed.workoutSessions[0].totalValidReps)
        assertEquals(1, parsed.setRecords.size)
        assertEquals(25, parsed.setRecords[0].validReps)
        assertEquals(1, parsed.exerciseProgressions.size)
        assertEquals(25, parsed.exerciseProgressions[0].personalRecordReps)
        assertEquals(1, parsed.scheduledWorkouts.size)
        assertEquals("MONDAY", parsed.scheduledWorkouts[0].dayOfWeek)
        assertEquals(1, parsed.aiActionLogs.size)
        assertEquals("QUERY_STATS", parsed.aiActionLogs[0].actionType)
    }
}
