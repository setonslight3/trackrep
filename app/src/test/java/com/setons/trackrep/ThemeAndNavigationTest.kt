package com.setons.trackrep

import com.setons.trackrep.navigation.CoachNavKey
import com.setons.trackrep.navigation.HistoryNavKey
import com.setons.trackrep.navigation.HomeNavKey
import com.setons.trackrep.navigation.ProfileNavKey
import com.setons.trackrep.navigation.TrackAiNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeAndNavigationTest {

    @Test
    fun testNavigationKeysExist() {
        val keys = listOf(HomeNavKey, CoachNavKey, TrackAiNavKey, HistoryNavKey, ProfileNavKey)
        assertEquals(5, keys.size)
        keys.forEach { key ->
            assertNotNull(key)
        }
    }

    @Test
    fun testExerciseFramingModes() {
        val modes = com.setons.trackrep.camera.ExerciseFramingMode.values()
        assertEquals(5, modes.size)
        val pushUp = com.setons.trackrep.camera.ExerciseFramingMode.PUSH_UP
        assertEquals("Push-up", pushUp.displayName)
        assertEquals("5–7 normal paces", pushUp.recommendedDistance)
    }

    @Test
    fun testFramingStatusPassing() {
        val aligned = com.setons.trackrep.camera.FramingStatus.FRAMING_ALIGNED
        val calibrating = com.setons.trackrep.camera.FramingStatus.CALIBRATING
        assertEquals(true, aligned.isPassing)
        assertEquals(false, calibrating.isPassing)
    }

    @Test
    fun testSessionReviewNavKey() {
        val key = com.setons.trackrep.navigation.SessionReviewNavKey("test_session_123")
        assertEquals("test_session_123", key.sessionId)
    }

    @Test
    fun testPoseAngleCalculatorRightAngle() {
        // Shoulder at (0, 1), Elbow at (0, 0), Wrist at (1, 0) forms a 90 degree angle
        val shoulder = com.setons.trackrep.pose.TrackedLandmark(0, 0f, 1f, 0.9f)
        val elbow = com.setons.trackrep.pose.TrackedLandmark(1, 0f, 0f, 0.9f)
        val wrist = com.setons.trackrep.pose.TrackedLandmark(2, 1f, 0f, 0.9f)

        val angle = com.setons.trackrep.pose.PoseAngleCalculator.calculateAngle(shoulder, elbow, wrist)
        assertNotNull(angle)
        assertEquals(90.0, angle!!, 0.5)
    }

    @Test
    fun testSessionReviewRepository() {
        val sample = com.setons.trackrep.review.SessionReviewRepository.getSessionById("sample_session_1")
        assertNotNull(sample)
        assertEquals(true, sample!!.detectedFlaws.isNotEmpty())
        assertEquals(true, sample.recordedPoses.isNotEmpty())
    }

    @Test
    fun testSessionReviewRepositoryVideoUpdate() {
        val testSession = com.setons.trackrep.review.RecordedWorkoutSession(
            id = "test_update_id",
            exerciseName = "Push-up Set",
            videoPath = null,
            durationSeconds = 15,
            repCount = 5,
            dateString = "Sep 19, 5:00 PM",
            detectedFlaws = emptyList(),
            recordedPoses = emptyList()
        )
        com.setons.trackrep.review.SessionReviewRepository.addSession(testSession)
        assertEquals(null, com.setons.trackrep.review.SessionReviewRepository.getSessionById("test_update_id")?.videoPath)

        com.setons.trackrep.review.SessionReviewRepository.updateVideoPath("test_update_id", "/path/to/recorded_set.mp4")
        assertEquals("/path/to/recorded_set.mp4", com.setons.trackrep.review.SessionReviewRepository.getSessionById("test_update_id")?.videoPath)
    }

    @Test
    fun testSyntheticPosesGeneration() {
        val poses = com.setons.trackrep.review.SessionReviewRepository.generatePosesForExercise("Push-up", 10)
        assertEquals(true, poses.isNotEmpty())
        assertEquals(0L, poses.first().timestampMs)
        assertEquals(true, poses.last().timestampMs >= 10000L)
        assertEquals(true, poses.first().pose.isTrackingValid)
    }

    @Test
    fun testRainbowPresetsValidation() {
        assertEquals(10, com.setons.trackrep.theme.ThemeManager.RAINBOW_PRESETS.size)
        val crimson = com.setons.trackrep.theme.ThemeManager.RAINBOW_PRESETS[0]
        assertEquals("Crimson Red", crimson.name)
        assertEquals("#E53935", crimson.hexCode)
    }

    @Test
    fun testThemeColorHexConversions() {
        val originalHex = "#D4AF37"
        val color = com.setons.trackrep.theme.ThemeManager.hexToColor(originalHex)
        assertNotNull(color)
        val roundtripHex = com.setons.trackrep.theme.ThemeManager.colorToHex(color!!)
        assertEquals(originalHex, roundtripHex)

        // Without hash prefix
        val colorNoHash = com.setons.trackrep.theme.ThemeManager.hexToColor("00E676")
        assertNotNull(colorNoHash)
        assertEquals("#00E676", com.setons.trackrep.theme.ThemeManager.colorToHex(colorNoHash!!))

        // Invalid hex strings
        assertEquals(null, com.setons.trackrep.theme.ThemeManager.hexToColor("INVALID"))
        assertEquals(null, com.setons.trackrep.theme.ThemeManager.hexToColor("123"))
    }

    @Test
    fun testHsvAndRgbMath() {
        val red = androidx.compose.ui.graphics.Color(1f, 0f, 0f, 1f)
        val hsv = com.setons.trackrep.theme.ThemeManager.colorToHsv(red)
        assertEquals(0f, hsv[0], 1f)
        assertEquals(1f, hsv[1], 0.05f)
        assertEquals(1f, hsv[2], 0.05f)

        val reconstructedRed = com.setons.trackrep.theme.ThemeManager.hsvToColor(hsv[0], hsv[1], hsv[2])
        assertEquals(1f, reconstructedRed.red, 0.05f)
        assertEquals(0f, reconstructedRed.green, 0.05f)
        assertEquals(0f, reconstructedRed.blue, 0.05f)
    }

    @Test
    fun testDynamicColorSchemeBuilders() {
        val gold = androidx.compose.ui.graphics.Color(0xFFD4AF37)
        val darkScheme = com.setons.trackrep.theme.buildDynamicDarkColorScheme(gold)
        assertEquals(gold, darkScheme.primary)
        assertEquals(com.setons.trackrep.theme.DarkBackground, darkScheme.background)

        val lightScheme = com.setons.trackrep.theme.buildDynamicLightColorScheme(gold)
        assertEquals(gold, lightScheme.primary)
        assertEquals(com.setons.trackrep.theme.LightBackground, lightScheme.background)
    }

    @Test
    fun testNavigateAppActionStructure() {
        val action = com.setons.trackrep.ai.action.NavigateAppAction(
            destination = "HISTORY",
            buttonLabel = "Take Me to Workout History",
            explanation = "View your personal records and past workout logs."
        )
        assertEquals("NAVIGATE_APP", action.actionType)
        assertEquals("HISTORY", action.destination)
        assertEquals("Take Me to Workout History", action.buttonLabel)
        assertEquals(true, action.explanation.contains("personal records"))
    }

    @Test
    fun testNavigateAppActionValidation() {
        val action = com.setons.trackrep.ai.action.NavigateAppAction(
            destination = "COACH",
            buttonLabel = "Launch AI Coach",
            explanation = "Start live tracking."
        )
        val validation = com.setons.trackrep.ai.action.TrackActionValidator.validate(action)
        assertEquals(true, validation.isValid)
    }

    @Test
    fun testOnDeviceReasoningNavigationBestReps() {
        val result = com.setons.trackrep.ai.service.TrackAiService.evaluateOnDeviceReasoning("Where are my best reps and records?")
        assertNotNull(result)
        assertEquals(true, result.actions.isNotEmpty())
        val action = result.actions.first() as com.setons.trackrep.ai.action.NavigateAppAction
        assertEquals("HISTORY", action.destination)
        assertEquals("Take Me to History & Best Reps", action.buttonLabel)
    }

    @Test
    fun testOnDeviceReasoningNavigationCustomExercises() {
        val result = com.setons.trackrep.ai.service.TrackAiService.evaluateOnDeviceReasoning("How do I add custom exercises?")
        assertNotNull(result)
        assertEquals(true, result.actions.isNotEmpty())
        val action = result.actions.first() as com.setons.trackrep.ai.action.NavigateAppAction
        assertEquals("EXERCISE_LIBRARY", action.destination)
        assertEquals("Take Me to Exercise Library", action.buttonLabel)
    }

    @Test
    fun testExerciseCatalogBuiltInCountAndArchetypes() {
        val all = com.setons.trackrep.exercise.catalog.ExerciseCatalog.getAll()
        assertEquals(true, all.size >= 35)
        val pushUp = com.setons.trackrep.exercise.catalog.ExerciseCatalog.getById("push_up_standard")
        assertNotNull(pushUp)
        assertEquals(true, pushUp!!.isVisionSupported)
        assertEquals(com.setons.trackrep.camera.ExerciseFramingMode.PUSH_UP, pushUp.framingMode)
    }

    @Test
    fun testCustomExerciseJsonRoundtrip() {
        val custom = com.setons.trackrep.exercise.model.Exercise(
            id = "custom_test_dip",
            name = "Parallel Bar Dip",
            targetMuscle = com.setons.trackrep.exercise.model.MuscleGroup.ARMS,
            secondaryMuscles = listOf(com.setons.trackrep.exercise.model.MuscleGroup.CHEST, com.setons.trackrep.exercise.model.MuscleGroup.SHOULDERS),
            difficulty = com.setons.trackrep.exercise.model.DifficultyLevel.ADVANCED,
            equipment = com.setons.trackrep.exercise.model.EquipmentType.BODYWEIGHT,
            framingMode = com.setons.trackrep.camera.ExerciseFramingMode.PUSH_UP,
            defaultSets = 4,
            defaultReps = 12,
            restSeconds = 60,
            instructions = listOf("Grip parallel bars firmly.", "Lower until elbows reach 90 degrees.", "Drive upward to full lockout."),
            commonFlaws = listOf("Shrugging shoulders", "Partial depth"),
            proTip = "Maintain a slight forward lean to emphasize triceps and lower chest."
        )

        assertEquals(true, custom.isVisionSupported)

        val obj = org.json.JSONObject().apply {
            put("id", custom.id)
            put("name", custom.name)
            put("targetMuscle", custom.targetMuscle.name)
            put("secondaryMuscles", org.json.JSONArray(custom.secondaryMuscles.map { it.name }))
            put("difficulty", custom.difficulty.name)
            put("equipment", custom.equipment.name)
            put("framingMode", custom.framingMode?.name)
            put("defaultSets", custom.defaultSets)
            put("defaultReps", custom.defaultReps)
            put("defaultHoldSeconds", custom.defaultHoldSeconds)
            put("restSeconds", custom.restSeconds)
            put("instructions", org.json.JSONArray(custom.instructions))
            put("commonFlaws", org.json.JSONArray(custom.commonFlaws))
            put("proTip", custom.proTip)
        }

        assertEquals("custom_test_dip", obj.getString("id"))
        assertEquals("Parallel Bar Dip", obj.getString("name"))
        assertEquals("PUSH_UP", obj.getString("framingMode"))
        assertEquals(12, obj.getInt("defaultReps"))
    }
}
