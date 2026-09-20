package com.setons.trackrep

import com.setons.trackrep.exercise.squat.SquatAnalyzer
import com.setons.trackrep.exercise.squat.SquatPhase
import com.setons.trackrep.pose.TrackedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SquatAnalyzerTest {

    private lateinit var analyzer: SquatAnalyzer

    @Before
    fun setUp() {
        analyzer = SquatAnalyzer()
    }

    private fun mockPose(kneeAngle: Double, torsoLean: Double = 80.0): TrackedPose {
        return TrackedPose(
            landmarks = emptyMap(),
            imageWidth = 720,
            imageHeight = 1280,
            isTrackingValid = true,
            leftKneeAngle = kneeAngle,
            rightKneeAngle = kneeAngle,
            torsoLeanAngle = torsoLean
        )
    }

    @Test
    fun testInitialState() {
        val telemetry = analyzer.liveTelemetry
        assertEquals(0, telemetry.validRepCount)
        assertEquals(0, telemetry.partialRepCount)
        assertEquals(SquatPhase.UNKNOWN, telemetry.currentPhase)
        assertEquals(0f, telemetry.depthProgress, 0.01f)
        assertEquals(0.0, telemetry.averageCadenceSec, 0.01)
    }

    @Test
    fun testFullValidSquatCycle() {
        var t = 1000L

        // 1. Establish Standing Lockout (165 deg)
        var telemetry = analyzer.processPose(mockPose(165.0), t)
        assertEquals(SquatPhase.STANDING_LOCKOUT, telemetry.currentPhase)

        // 2. Initiate descent (under 152 deg)
        t += 300
        telemetry = analyzer.processPose(mockPose(145.0), t)
        assertEquals(SquatPhase.DESCENDING, telemetry.currentPhase)

        // 3. Reach parallel bottom depth (88 deg <= 95 deg)
        t += 400
        telemetry = analyzer.processPose(mockPose(88.0), t)
        assertEquals(SquatPhase.BOTTOM_DEPTH, telemetry.currentPhase)
        assertTrue("Depth progress should be at or near 100%", telemetry.depthProgress >= 0.95f)

        // 4. Begin ascent (knee angle increases past bottom + 6 deg)
        t += 400
        telemetry = analyzer.processPose(mockPose(110.0), t)
        assertEquals(SquatPhase.ASCENDING, telemetry.currentPhase)

        // 5. Complete lockout (162 deg >= 154 deg) with duration = 1500ms >= 700ms
        t += 400
        telemetry = analyzer.processPose(mockPose(162.0), t)

        assertEquals(1, telemetry.validRepCount)
        assertEquals(0, telemetry.partialRepCount)
        assertEquals(SquatPhase.STANDING_LOCKOUT, telemetry.currentPhase)
        assertNotNull(telemetry.lastCompletedRep)
        assertTrue(telemetry.lastCompletedRep!!.isValid)
        assertEquals(88.0, telemetry.lastCompletedRep!!.minKneeAngle, 0.1)
    }

    @Test
    fun testPartialSquatDepthRejection() {
        var t = 1000L

        // 1. Lockout
        analyzer.processPose(mockPose(165.0), t)

        // 2. Descend only to 110 deg (above 95 deg threshold)
        t += 300
        analyzer.processPose(mockPose(140.0), t)
        t += 300
        analyzer.processPose(mockPose(110.0), t)

        // 3. Reverse upwards early without hitting parallel (110 + 8 = 118 deg)
        t += 300
        val midTelemetry = analyzer.processPose(mockPose(122.0), t)
        assertEquals(SquatPhase.ASCENDING, midTelemetry.currentPhase)

        // 4. Return to standing lockout
        t += 400
        val finalTelemetry = analyzer.processPose(mockPose(162.0), t)

        // Rep counted as partial, not valid
        assertEquals(0, finalTelemetry.validRepCount)
        assertEquals(1, finalTelemetry.partialRepCount)
        assertNotNull(finalTelemetry.lastCompletedRep)
        assertFalse(finalTelemetry.lastCompletedRep!!.isValid)
        assertTrue(finalTelemetry.lastCompletedRep!!.flawReason!!.contains("Incomplete depth"))

        val flaws = analyzer.getSummaryFlaws()
        assertTrue(flaws.any { it.title.contains("Incomplete depth") })
    }

    @Test
    fun testTorsoLeanForwardRejection() {
        var t = 1000L

        analyzer.processPose(mockPose(165.0, torsoLean = 80.0), t)

        // Descend with severe torso pitch (55 deg < 65 deg MIN_TORSO_ANGLE)
        t += 400
        val midTelemetry = analyzer.processPose(mockPose(88.0, torsoLean = 55.0), t)
        assertNotNull(midTelemetry.activeWarning)
        assertTrue(midTelemetry.activeWarning!!.contains("forward lean") || midTelemetry.activeWarning!!.contains("chest up"))

        // Ascend and finish lockout
        t += 400
        analyzer.processPose(mockPose(120.0, torsoLean = 60.0), t)
        t += 400
        val finalTelemetry = analyzer.processPose(mockPose(162.0, torsoLean = 80.0), t)

        assertEquals(0, finalTelemetry.validRepCount)
        assertEquals(1, finalTelemetry.partialRepCount)
        assertFalse(finalTelemetry.lastCompletedRep!!.isValid)
        assertTrue(finalTelemetry.lastCompletedRep!!.flawReason!!.contains("Chest pitched forward"))
    }

    @Test
    fun testResetClearsState() {
        analyzer.processPose(mockPose(165.0), 1000L)
        analyzer.processPose(mockPose(140.0), 1300L)
        analyzer.processPose(mockPose(88.0), 1700L)
        analyzer.processPose(mockPose(120.0), 2100L)
        analyzer.processPose(mockPose(162.0), 2500L)
        assertEquals(1, analyzer.liveTelemetry.validRepCount)

        analyzer.reset()
        assertEquals(0, analyzer.liveTelemetry.validRepCount)
        assertEquals(SquatPhase.UNKNOWN, analyzer.liveTelemetry.currentPhase)
        assertTrue(analyzer.getCompletedReps().isEmpty())
    }
}
