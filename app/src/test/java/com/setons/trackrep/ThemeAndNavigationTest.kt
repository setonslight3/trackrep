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
}
