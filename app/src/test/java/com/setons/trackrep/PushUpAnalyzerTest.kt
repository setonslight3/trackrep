package com.setons.trackrep

import com.setons.trackrep.exercise.pushup.PushUpAnalyzer
import com.setons.trackrep.exercise.pushup.PushUpPhase
import com.setons.trackrep.pose.TrackedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PushUpAnalyzerTest {

    private lateinit var analyzer: PushUpAnalyzer

    @Before
    fun setUp() {
        analyzer = PushUpAnalyzer()
    }

    private fun mockPose(elbowAngle: Double, hipAngle: Double = 170.0): TrackedPose {
        return TrackedPose(
            landmarks = emptyMap(),
            imageWidth = 720,
            imageHeight = 1280,
            isTrackingValid = true,
            leftElbowAngle = elbowAngle,
            rightElbowAngle = elbowAngle,
            hipAlignmentAngle = hipAngle
        )
    }

    @Test
    fun testInitialState() {
        val telemetry = analyzer.liveTelemetry
        assertEquals(0, telemetry.validRepCount)
        assertEquals(0, telemetry.partialRepCount)
        assertEquals(PushUpPhase.UNKNOWN, telemetry.currentPhase)
        assertEquals(0f, telemetry.depthProgress, 0.01f)
        assertEquals(0.0, telemetry.averageCadenceSec, 0.01)
    }

    @Test
    fun testFullValidRepCycle() {
        var t = 1000L

        // 1. Establish Plank Ready at lockout (160 deg)
        var telemetry = analyzer.processPose(mockPose(160.0), t)
        assertEquals(PushUpPhase.PLANK_READY, telemetry.currentPhase)

        // 2. Initiate descent (under 142 deg)
        t += 200
        telemetry = analyzer.processPose(mockPose(135.0), t)
        assertEquals(PushUpPhase.DESCENDING, telemetry.currentPhase)

        // 3. Reach bottom depth (elbow at 88 deg <= 92 deg)
        t += 300
        telemetry = analyzer.processPose(mockPose(88.0), t)
        assertEquals(PushUpPhase.BOTTOM_DEPTH, telemetry.currentPhase)
        assertTrue("Depth progress should be 100%", telemetry.depthProgress >= 0.99f)

        // 4. Begin ascent (pressing up out of the hole, >= 100 deg)
        t += 300
        telemetry = analyzer.processPose(mockPose(105.0), t)
        assertEquals(PushUpPhase.ASCENDING, telemetry.currentPhase)

        // 5. Complete rep at top lockout (155 deg >= 150 deg) with duration = 1000ms >= 600ms
        t += 400
        telemetry = analyzer.processPose(mockPose(155.0), t)

        // Verified valid rep counted!
        assertEquals(1, telemetry.validRepCount)
        assertEquals(0, telemetry.partialRepCount)
        assertEquals(PushUpPhase.PLANK_READY, telemetry.currentPhase)
        assertNotNull(telemetry.lastCompletedRep)
        assertTrue(telemetry.lastCompletedRep!!.isValid)
        assertEquals(88.0, telemetry.lastCompletedRep!!.minElbowAngle, 0.1)
    }

    @Test
    fun testPartialRepRejectionOnIncompleteDepth() {
        var t = 1000L

        // 1. Plank ready
        analyzer.processPose(mockPose(160.0), t)

        // 2. Descend partially to only 115 deg (cuts depth short)
        t += 200
        analyzer.processPose(mockPose(130.0), t)
        t += 200
        analyzer.processPose(mockPose(115.0), t)

        // 3. User aborts descent and bounces up to 128 deg (> 115 + 10 deg)
        t += 200
        val telemetry = analyzer.processPose(mockPose(128.0), t)
        assertEquals(PushUpPhase.ASCENDING, telemetry.currentPhase)
        assertEquals("Push lower • Incomplete depth", telemetry.activeWarning)

        // 4. Returns to lockout at 155 deg
        t += 400
        val finalTelemetry = analyzer.processPose(mockPose(155.0), t)

        // Rep completed but counted as partial, not valid!
        assertEquals(0, finalTelemetry.validRepCount)
        assertEquals(1, finalTelemetry.partialRepCount)
        assertNotNull(finalTelemetry.lastCompletedRep)
        assertFalse(finalTelemetry.lastCompletedRep!!.isValid)
        assertTrue(finalTelemetry.lastCompletedRep!!.flawReason!!.contains("Incomplete depth"))

        // Flaw recorded in summary
        val flaws = analyzer.getSummaryFlaws()
        assertTrue(flaws.any { it.title.contains("Incomplete depth") })
    }

    @Test
    fun testHipSaggingDetection() {
        var t = 1000L

        // 1. Plank ready with good alignment
        analyzer.processPose(mockPose(160.0, hipAngle = 175.0), t)

        // 2. Descend, but hips sag significantly (130 deg < 150 deg MIN_HIP_ANGLE)
        t += 300
        val midTelemetry = analyzer.processPose(mockPose(90.0, hipAngle = 130.0), t)
        assertEquals("Keep core braced • Hips sagging", midTelemetry.activeWarning)

        // 3. Ascend and complete lockout
        t += 300
        analyzer.processPose(mockPose(115.0, hipAngle = 135.0), t)
        t += 400
        val finalTelemetry = analyzer.processPose(mockPose(155.0, hipAngle = 160.0), t)

        // Full depth was reached, but torso alignment failed
        assertEquals(0, finalTelemetry.validRepCount)
        assertEquals(1, finalTelemetry.partialRepCount)
        assertFalse(finalTelemetry.lastCompletedRep!!.isValid)
        assertTrue(finalTelemetry.lastCompletedRep!!.flawReason!!.contains("Hips sagging"))
    }

    @Test
    fun testHysteresisAtLockoutPreventsPrematureDescent() {
        // User trembling at top lockout between 150 deg and 145 deg (above 142 deg trigger)
        analyzer.processPose(mockPose(160.0), 1000L)
        val t1 = analyzer.processPose(mockPose(148.0), 1100L)
        assertEquals(PushUpPhase.PLANK_READY, t1.currentPhase)

        val t2 = analyzer.processPose(mockPose(144.0), 1200L)
        assertEquals(PushUpPhase.PLANK_READY, t2.currentPhase)

        // Once angle drops <= 142.0, descent triggers
        val t3 = analyzer.processPose(mockPose(140.0), 1300L)
        assertEquals(PushUpPhase.DESCENDING, t3.currentPhase)
    }

    @Test
    fun testCadenceCalculationAcrossMultipleReps() {
        var t = 1000L

        // Rep 1: Takes 1000ms (1.0 sec)
        analyzer.processPose(mockPose(160.0), t)
        t += 200
        analyzer.processPose(mockPose(135.0), t) // descent starts at t = 1200
        t += 300
        analyzer.processPose(mockPose(88.0), t)
        t += 300
        analyzer.processPose(mockPose(110.0), t)
        t += 400
        analyzer.processPose(mockPose(155.0), t) // end at t = 2200 (duration = 1.0s)
        assertEquals(1, analyzer.liveTelemetry.validRepCount)

        // Rep 2: Takes 2000ms (2.0 sec)
        t += 100
        analyzer.processPose(mockPose(140.0), t) // descent starts at t = 2300
        t += 800
        analyzer.processPose(mockPose(85.0), t)
        t += 600
        analyzer.processPose(mockPose(110.0), t)
        t += 600
        val telemetry = analyzer.processPose(mockPose(155.0), t) // end at t = 4300 (duration = 2.0s)

        assertEquals(2, telemetry.validRepCount)
        // Average cadence of 1.0s and 2.0s = 1.5s/rep
        assertEquals(1.5, telemetry.averageCadenceSec, 0.05)
    }

    @Test
    fun testResetClearsState() {
        // Complete one rep
        analyzer.processPose(mockPose(160.0), 1000L)
        analyzer.processPose(mockPose(135.0), 1200L)
        analyzer.processPose(mockPose(85.0), 1500L)
        analyzer.processPose(mockPose(110.0), 1800L)
        analyzer.processPose(mockPose(155.0), 2200L)
        assertEquals(1, analyzer.liveTelemetry.validRepCount)

        // Reset
        analyzer.reset()
        assertEquals(0, analyzer.liveTelemetry.validRepCount)
        assertEquals(PushUpPhase.UNKNOWN, analyzer.liveTelemetry.currentPhase)
        assertTrue(analyzer.getCompletedReps().isEmpty())
    }
}
