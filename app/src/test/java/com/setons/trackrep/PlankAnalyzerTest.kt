package com.setons.trackrep

import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.exercise.plank.PlankAnalyzer
import com.setons.trackrep.exercise.plank.PlankStatus
import com.setons.trackrep.pose.TrackedLandmark
import com.setons.trackrep.pose.TrackedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlankAnalyzerTest {

    private lateinit var analyzer: PlankAnalyzer

    @Before
    fun setUp() {
        analyzer = PlankAnalyzer()
    }

    private fun mockPlankPose(
        hipAngle: Double,
        shoulderY: Float = 0.4f,
        hipY: Float = 0.4f,
        ankleY: Float = 0.4f
    ): TrackedPose {
        val landmarks = mapOf(
            PoseLandmark.LEFT_SHOULDER to TrackedLandmark(PoseLandmark.LEFT_SHOULDER, 0.1f, shoulderY, 0.99f),
            PoseLandmark.LEFT_HIP to TrackedLandmark(PoseLandmark.LEFT_HIP, 0.3f, hipY, 0.99f),
            PoseLandmark.LEFT_ANKLE to TrackedLandmark(PoseLandmark.LEFT_ANKLE, 0.5f, ankleY, 0.99f)
        )
        return TrackedPose(
            landmarks = landmarks,
            imageWidth = 720,
            imageHeight = 1280,
            isTrackingValid = true,
            hipAlignmentAngle = hipAngle
        )
    }

    @Test
    fun testInitialState() {
        val telemetry = analyzer.liveTelemetry
        assertEquals(0, telemetry.holdDurationSeconds)
        assertEquals(PlankStatus.CALIBRATING, telemetry.currentStatus)
        assertEquals(100, telemetry.alignmentScorePercent)
    }

    @Test
    fun testSolidPlankAccumulation() {
        var t = 1000L

        // Initial frame
        var telemetry = analyzer.processPose(mockPlankPose(175.0), t)
        assertEquals(PlankStatus.SOLID_PLANK, telemetry.currentStatus)

        // Advance in 100ms steps up to 3000ms (2.0 seconds elapsed)
        for (i in 1..20) {
            t += 100
            telemetry = analyzer.processPose(mockPlankPose(175.0), t)
        }

        assertEquals(PlankStatus.SOLID_PLANK, telemetry.currentStatus)
        assertEquals(2, telemetry.holdDurationSeconds)
        assertEquals(100, telemetry.alignmentScorePercent)
        assertEquals(null, telemetry.activeWarning)
    }

    @Test
    fun testHipSagDetection() {
        var t = 1000L
        analyzer.processPose(mockPlankPose(175.0), t)

        // Hip drops below midline (expectedMidY = 0.4, hipY = 0.55, larger Y is lower towards floor)
        t += 200
        val telemetry = analyzer.processPose(mockPlankPose(145.0, shoulderY = 0.4f, hipY = 0.55f, ankleY = 0.4f), t)

        assertEquals(PlankStatus.HIP_SAG, telemetry.currentStatus)
        assertNotNull(telemetry.activeWarning)
        assertTrue(telemetry.activeWarning!!.contains("Hips sagging"))
    }

    @Test
    fun testHipPikeDetection() {
        var t = 1000L
        analyzer.processPose(mockPlankPose(175.0), t)

        // Hip pikes up above midline (expectedMidY = 0.4, hipY = 0.25, smaller Y is higher towards ceiling)
        t += 200
        val telemetry = analyzer.processPose(mockPlankPose(145.0, shoulderY = 0.4f, hipY = 0.25f, ankleY = 0.4f), t)

        assertEquals(PlankStatus.HIP_PIKE, telemetry.currentStatus)
        assertNotNull(telemetry.activeWarning)
        assertTrue(telemetry.activeWarning!!.contains("Hips too high"))
    }

    @Test
    fun testMilestoneVoiceCueAt15Seconds() {
        var t = 1000L
        analyzer.processPose(mockPlankPose(175.0), t)

        // Advance 150 steps of 100ms = 15 seconds
        var finalTelemetry = analyzer.liveTelemetry
        for (i in 1..150) {
            t += 100
            finalTelemetry = analyzer.processPose(mockPlankPose(175.0), t)
        }

        assertEquals(15, finalTelemetry.holdDurationSeconds)
        assertEquals("15 seconds, great start", finalTelemetry.milestoneVoiceCue)
    }

    @Test
    fun testResetClearsHold() {
        var t = 1000L
        for (i in 1..20) {
            t += 100
            analyzer.processPose(mockPlankPose(175.0), t)
        }
        assertTrue(analyzer.getTotalHoldSeconds() > 0)

        analyzer.reset()
        assertEquals(0, analyzer.getTotalHoldSeconds())
        assertEquals(PlankStatus.CALIBRATING, analyzer.liveTelemetry.currentStatus)
    }
}
