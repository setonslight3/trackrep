package com.setons.trackrep.pose

import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.camera.ExerciseFramingMode
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

data class DetectedExerciseInfo(
    val exerciseId: String,
    val framingMode: ExerciseFramingMode,
    val displayName: String,
    val confidence: Float
)

/**
 * Real-time on-device Pose Classifier.
 * Analyzes ML Kit 3D skeletal geometry, spatial orientation (horizontal vs vertical vs inverted),
 * and dynamic joint angle kinetics to identify the active exercise automatically.
 * Includes temporal smoothing and hysteresis to eliminate flickering.
 */
class ExerciseClassifier {

    companion object {
        const val WINDOW_SIZE = 10           // ~330ms of frames at 30fps
        const val CONFIDENCE_THRESHOLD = 0.70f // 70% agreement required to trigger auto-switch
    }

    private val recentCandidates = ArrayDeque<String>()
    private var lastConfirmedExerciseId: String? = null

    // Track elbow movement variance for Push-up vs Plank distinction
    private val recentElbowAngles = ArrayDeque<Double>()

    fun reset() {
        recentCandidates.clear()
        recentElbowAngles.clear()
        lastConfirmedExerciseId = null
    }

    /**
     * Analyzes incoming pose frame and returns a confirmed exercise switch if consensus is reached.
     */
    fun processPose(pose: TrackedPose): DetectedExerciseInfo? {
        if (!pose.isTrackingValid) return null

        val candidateId = classifySinglePose(pose) ?: return null

        // Add to sliding window
        if (recentCandidates.size >= WINDOW_SIZE) {
            recentCandidates.removeFirst()
        }
        recentCandidates.addLast(candidateId)

        if (recentCandidates.size < 6) return null

        // Count occurrences in recent window
        val frequencyMap = recentCandidates.groupingBy { it }.eachCount()
        val topEntry = frequencyMap.maxByOrNull { it.value } ?: return null
        val ratio = topEntry.value.toFloat() / recentCandidates.size

        if (ratio >= CONFIDENCE_THRESHOLD && topEntry.key != lastConfirmedExerciseId) {
            lastConfirmedExerciseId = topEntry.key
            val exercise = ExerciseCatalog.getById(topEntry.key) ?: return null
            val framing = exercise.framingMode ?: ExerciseFramingMode.PUSH_UP
            return DetectedExerciseInfo(
                exerciseId = exercise.id,
                framingMode = framing,
                displayName = exercise.name,
                confidence = ratio
            )
        }

        return null
    }

    /**
     * Kinematic single-frame pose classifier.
     */
    fun classifySinglePose(pose: TrackedPose): String? {
        val landmarks = pose.landmarks

        val leftShoulder = landmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = landmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = landmarks[PoseLandmark.LEFT_HIP]
        val rightHip = landmarks[PoseLandmark.RIGHT_HIP]

        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return null
        }

        val shoulderX = (leftShoulder.x + rightShoulder.x) / 2f
        val shoulderY = (leftShoulder.y + rightShoulder.y) / 2f
        val hipX = (leftHip.x + rightHip.x) / 2f
        val hipY = (leftHip.y + rightHip.y) / 2f

        // Torso orientation vector
        val dx = abs(shoulderX - hipX).toDouble()
        val dy = (hipY - shoulderY).toDouble() // In screen coords: y=0 top, y=1 bottom. Upright dy is positive.

        val torsoAngleDeg = Math.toDegrees(atan2(abs(dy), dx)).toFloat()

        val leftElbow = pose.leftElbowAngle ?: 180.0
        val rightElbow = pose.rightElbowAngle ?: 180.0
        val minElbow = minOf(leftElbow, rightElbow)

        val leftKnee = pose.leftKneeAngle ?: 180.0
        val rightKnee = pose.rightKneeAngle ?: 180.0
        val minKnee = minOf(leftKnee, rightKnee)

        // Track elbow variance for push-up vs plank
        if (recentElbowAngles.size >= 12) recentElbowAngles.removeFirst()
        recentElbowAngles.addLast(minElbow)
        val elbowVariance = if (recentElbowAngles.size > 4) {
            val maxE = recentElbowAngles.maxOrNull() ?: 180.0
            val minE = recentElbowAngles.minOrNull() ?: 180.0
            maxE - minE
        } else 0.0

        // 1. INVERTED POSE (Shoulders lower than hips)
        if (shoulderY > hipY + 0.08f) {
            return "pike_push_up"
        }

        // 2. HORIZONTAL / FLOOR POSE (Torso angle < 40 degrees from horizontal)
        if (torsoAngleDeg < 40f) {
            // Check if hips are thrust upward higher than shoulders (Glute Bridge)
            if (hipY < shoulderY - 0.08f && minKnee < 120.0) {
                return "glute_bridge"
            }

            // Prone floor exercises: Push-up vs Plank vs Mountain Climber
            val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]
            val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]

            // Check dynamic leg movement for mountain climber
            if (leftAnkle != null && rightAnkle != null && abs(leftAnkle.y - rightAnkle.y) > 0.15f) {
                return "mountain_climber"
            }

            // If elbows are actively flexing or bent below 135 degrees -> Push-up
            return if (minElbow < 135.0 || elbowVariance > 25.0) {
                "push_up_standard"
            } else {
                "plank_standard"
            }
        }

        // 3. UPRIGHT / STANDING POSE (Torso angle >= 50 degrees)
        if (torsoAngleDeg >= 50f && shoulderY < hipY) {
            // Check overhead arms (Pull-up / Hanging)
            val leftWrist = landmarks[PoseLandmark.LEFT_WRIST]
            val rightWrist = landmarks[PoseLandmark.RIGHT_WRIST]
            val wristsAboveShoulders = (leftWrist != null && leftWrist.y < shoulderY - 0.10f) ||
                    (rightWrist != null && rightWrist.y < shoulderY - 0.10f)

            if (wristsAboveShoulders && minElbow < 130.0) {
                return "pull_up_standard"
            }

            // Check squat / lunge knee flexion
            if (minKnee < 140.0) {
                val kneeDiff = abs(leftKnee - rightKnee)
                return if (kneeDiff > 35.0) {
                    "lunge_reverse"
                } else {
                    "squat_bodyweight"
                }
            }

            // Check arms extended wide for scapular circles
            if (leftWrist != null && rightWrist != null && abs(leftWrist.x - rightWrist.x) > 0.65f) {
                return "arm_circles_scapular"
            }

            // Standing default
            return "squat_bodyweight"
        }

        return null
    }
}
