package com.setons.trackrep

import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.pose.ExerciseClassifier
import com.setons.trackrep.pose.TrackedLandmark
import com.setons.trackrep.pose.TrackedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseClassifierTest {

    private lateinit var classifier: ExerciseClassifier

    @Before
    fun setUp() {
        classifier = ExerciseClassifier()
    }

    private fun createPose(
        shoulderY: Float,
        hipY: Float,
        shoulderX: Float = 0.5f,
        hipX: Float = 0.5f,
        leftElbowAngle: Double = 170.0,
        rightElbowAngle: Double = 170.0,
        leftKneeAngle: Double = 175.0,
        rightKneeAngle: Double = 175.0,
        leftAnkleY: Float = 0.85f,
        rightAnkleY: Float = 0.85f,
        noseY: Float = 0.2f,
        leftWristY: Float = 0.6f,
        rightWristY: Float = 0.6f
    ): TrackedPose {
        val landmarks = mutableMapOf<Int, TrackedLandmark>()

        landmarks[PoseLandmark.LEFT_SHOULDER] = TrackedLandmark(PoseLandmark.LEFT_SHOULDER, shoulderX - 0.05f, shoulderY, 0.99f)
        landmarks[PoseLandmark.RIGHT_SHOULDER] = TrackedLandmark(PoseLandmark.RIGHT_SHOULDER, shoulderX + 0.05f, shoulderY, 0.99f)

        landmarks[PoseLandmark.LEFT_HIP] = TrackedLandmark(PoseLandmark.LEFT_HIP, hipX - 0.05f, hipY, 0.99f)
        landmarks[PoseLandmark.RIGHT_HIP] = TrackedLandmark(PoseLandmark.RIGHT_HIP, hipX + 0.05f, hipY, 0.99f)

        landmarks[PoseLandmark.LEFT_ANKLE] = TrackedLandmark(PoseLandmark.LEFT_ANKLE, hipX - 0.05f, leftAnkleY, 0.99f)
        landmarks[PoseLandmark.RIGHT_ANKLE] = TrackedLandmark(PoseLandmark.RIGHT_ANKLE, hipX + 0.05f, rightAnkleY, 0.99f)

        landmarks[PoseLandmark.NOSE] = TrackedLandmark(PoseLandmark.NOSE, shoulderX, noseY, 0.99f)
        landmarks[PoseLandmark.LEFT_WRIST] = TrackedLandmark(PoseLandmark.LEFT_WRIST, shoulderX - 0.15f, leftWristY, 0.99f)
        landmarks[PoseLandmark.RIGHT_WRIST] = TrackedLandmark(PoseLandmark.RIGHT_WRIST, shoulderX + 0.15f, rightWristY, 0.99f)

        return TrackedPose(
            landmarks = landmarks,
            imageWidth = 720,
            imageHeight = 1280,
            isTrackingValid = true,
            leftElbowAngle = leftElbowAngle,
            rightElbowAngle = rightElbowAngle,
            hipAlignmentAngle = 175.0,
            leftKneeAngle = leftKneeAngle,
            rightKneeAngle = rightKneeAngle,
            torsoLeanAngle = 85.0
        )
    }

    @Test
    fun testClassifyUprightSquatPose() {
        // Standing upright, knees flexing to 90 degrees
        val squatPose = createPose(
            shoulderY = 0.25f,
            hipY = 0.55f,
            shoulderX = 0.50f,
            hipX = 0.50f,
            leftKneeAngle = 90.0,
            rightKneeAngle = 90.0
        )
        val classification = classifier.classifySinglePose(squatPose)
        assertEquals("squat_bodyweight", classification)
    }

    @Test
    fun testClassifyUprightLungePose() {
        // One knee deeply bent (85 deg), other extended (145 deg)
        val lungePose = createPose(
            shoulderY = 0.28f,
            hipY = 0.56f,
            shoulderX = 0.50f,
            hipX = 0.50f,
            leftKneeAngle = 85.0,
            rightKneeAngle = 145.0
        )
        val classification = classifier.classifySinglePose(lungePose)
        assertEquals("lunge_reverse", classification)
    }

    @Test
    fun testClassifyFloorPushUpPose() {
        // Horizontal torso on floor, elbows bending to 90 degrees
        val pushUpPose = createPose(
            shoulderY = 0.70f,
            hipY = 0.72f,
            shoulderX = 0.30f,
            hipX = 0.65f,
            leftElbowAngle = 90.0,
            rightElbowAngle = 90.0
        )
        val classification = classifier.classifySinglePose(pushUpPose)
        assertEquals("push_up_standard", classification)
    }

    @Test
    fun testClassifyFloorPlankPose() {
        // Horizontal torso on floor, static extended arms/plank (elbow > 150 deg)
        val plankPose = createPose(
            shoulderY = 0.70f,
            hipY = 0.72f,
            shoulderX = 0.30f,
            hipX = 0.65f,
            leftElbowAngle = 165.0,
            rightElbowAngle = 165.0
        )
        val classification = classifier.classifySinglePose(plankPose)
        assertEquals("plank_standard", classification)
    }

    @Test
    fun testClassifyMountainClimberPose() {
        // Horizontal torso with rapid alternating ankle heights
        val climberPose = createPose(
            shoulderY = 0.68f,
            hipY = 0.70f,
            shoulderX = 0.30f,
            hipX = 0.65f,
            leftAnkleY = 0.85f,
            rightAnkleY = 0.65f // High knee drive
        )
        val classification = classifier.classifySinglePose(climberPose)
        assertEquals("mountain_climber", classification)
    }

    @Test
    fun testClassifyInvertedPikePose() {
        // Inverted: Shoulders lower than hips (y greater than hip y)
        val pikePose = createPose(
            shoulderY = 0.65f,
            hipY = 0.35f,
            shoulderX = 0.35f,
            hipX = 0.50f
        )
        val classification = classifier.classifySinglePose(pikePose)
        assertEquals("pike_push_up", classification)
    }

    @Test
    fun testTemporalSmoothingRejectsSingleFrameGlitch() {
        // Feeding 3 standing frames, 1 floor glitch frame, then 3 standing frames
        val standingPose = createPose(shoulderY = 0.25f, hipY = 0.55f, leftKneeAngle = 90.0, rightKneeAngle = 90.0)
        val glitchFloorPose = createPose(shoulderY = 0.70f, hipY = 0.72f, shoulderX = 0.30f, hipX = 0.65f)

        // Initial frames build window
        repeat(5) { classifier.processPose(standingPose) }
        val confirmedInitial = classifier.processPose(standingPose)
        assertNotNull(confirmedInitial)
        assertEquals("squat_bodyweight", confirmedInitial?.exerciseId)

        // Single frame glitch should NOT trigger a switch to push_up
        val glitchResult = classifier.processPose(glitchFloorPose)
        assertNull("Single glitch frame should be suppressed by temporal smoothing", glitchResult)
    }

    @Test
    fun testSustainedMovementTriggersAutoSwitch() {
        val standingSquat = createPose(shoulderY = 0.25f, hipY = 0.55f, leftKneeAngle = 90.0, rightKneeAngle = 90.0)
        val floorPushUp = createPose(shoulderY = 0.70f, hipY = 0.72f, shoulderX = 0.30f, hipX = 0.65f, leftElbowAngle = 90.0)

        // User starts doing squats
        var initialDetected: String? = null
        repeat(8) {
            val res = classifier.processPose(standingSquat)
            if (res != null) initialDetected = res.exerciseId
        }
        assertEquals("squat_bodyweight", initialDetected)

        // User drops to floor and sustains push-ups
        var pushUpDetected = false
        repeat(12) {
            val res = classifier.processPose(floorPushUp)
            if (res?.exerciseId == "push_up_standard") {
                pushUpDetected = true
                assertEquals(ExerciseFramingMode.PUSH_UP, res.framingMode)
            }
        }
        assertTrue("Sustained floor push-up posture should auto-switch", pushUpDetected)
    }
}
