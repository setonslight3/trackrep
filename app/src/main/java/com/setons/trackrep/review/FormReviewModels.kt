package com.setons.trackrep.review

import com.google.mlkit.vision.pose.PoseLandmark
import com.setons.trackrep.pose.TrackedLandmark
import com.setons.trackrep.pose.TrackedPose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FormFlaw(
    val timestampMs: Long,
    val title: String,
    val description: String,
    val correctionTip: String,
    val isSevere: Boolean = false
)

data class TimestampedPose(
    val timestampMs: Long,
    val pose: TrackedPose
)

data class RecordedWorkoutSession(
    val id: String,
    val exerciseName: String,
    val videoPath: String?,
    val durationSeconds: Int,
    val repCount: Int,
    val dateString: String,
    val detectedFlaws: List<FormFlaw>,
    val recordedPoses: List<TimestampedPose> = emptyList()
)

object SessionReviewRepository {
    private val sessions = mutableListOf<RecordedWorkoutSession>()

    init {
        // Pre-populate with realistic synthetic pose telemetry for sample session 1
        val samplePoses = generateSamplePushUpPoses(24)

        sessions.add(
            RecordedWorkoutSession(
                id = "sample_session_1",
                exerciseName = "Push-up Set #1",
                videoPath = null,
                durationSeconds = 24,
                repCount = 8,
                dateString = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date()),
                detectedFlaws = listOf(
                    FormFlaw(
                        timestampMs = 5000,
                        title = "Elbows Flared Out",
                        description = "Your elbows flared outwards past 75° during the bottom descent of Rep 3.",
                        correctionTip = "Tuck your elbows to approximately 45° relative to your torso to protect the shoulder capsule and load the chest."
                    ),
                    FormFlaw(
                        timestampMs = 12000,
                        title = "Hips Sagging",
                        description = "Lumbar spine hyperextended and hips dropped below shoulder level on Rep 6.",
                        correctionTip = "Squeeze your glutes and brace your core like a plank to keep a continuous straight line from head to heels."
                    ),
                    FormFlaw(
                        timestampMs = 18000,
                        title = "Incomplete Depth",
                        description = "Chest did not reach full depth (~90° elbow flexion) on Rep 9 due to fatigue.",
                        correctionTip = "Lower down smoothly until your chest is approximately 2–3 inches off the ground before pressing up."
                    )
                ),
                recordedPoses = samplePoses
            )
        )
    }

    fun getAllSessions(): List<RecordedWorkoutSession> = sessions.toList()

    fun getSessionById(id: String): RecordedWorkoutSession? = sessions.find { it.id == id }

    fun addSession(session: RecordedWorkoutSession) {
        sessions.add(0, session)
    }

    fun updateVideoPath(id: String, videoPath: String) {
        val index = sessions.indexOfFirst { it.id == id }
        if (index != -1) {
            sessions[index] = sessions[index].copy(videoPath = videoPath)
        }
    }

    fun generatePosesForExercise(exerciseName: String, durationSec: Int): List<TimestampedPose> {
        return generateSamplePushUpPoses(durationSec)
    }

    private fun generateSamplePushUpPoses(durationSec: Int): List<TimestampedPose> {
        val list = mutableListOf<TimestampedPose>()
        val totalMs = durationSec * 1000
        var t = 0L

        while (t <= totalMs) {
            // Rep cycle every ~3 seconds
            val phase = ((t % 3000) / 3000f) * 2f * Math.PI.toFloat()
            val yOffset = Math.sin(phase.toDouble()).toFloat() * 0.08f // vertical descent and ascent

            val landmarks = mapOf(
                PoseLandmark.LEFT_SHOULDER to TrackedLandmark(PoseLandmark.LEFT_SHOULDER, 0.42f, 0.50f + yOffset, 0.95f),
                PoseLandmark.RIGHT_SHOULDER to TrackedLandmark(PoseLandmark.RIGHT_SHOULDER, 0.58f, 0.50f + yOffset, 0.95f),
                PoseLandmark.LEFT_ELBOW to TrackedLandmark(PoseLandmark.LEFT_ELBOW, 0.36f, 0.62f + (yOffset * 1.3f), 0.92f),
                PoseLandmark.RIGHT_ELBOW to TrackedLandmark(PoseLandmark.RIGHT_ELBOW, 0.64f, 0.62f + (yOffset * 1.3f), 0.92f),
                PoseLandmark.LEFT_WRIST to TrackedLandmark(PoseLandmark.LEFT_WRIST, 0.37f, 0.76f, 0.90f),
                PoseLandmark.RIGHT_WRIST to TrackedLandmark(PoseLandmark.RIGHT_WRIST, 0.63f, 0.76f, 0.90f),
                PoseLandmark.LEFT_HIP to TrackedLandmark(PoseLandmark.LEFT_HIP, 0.45f, 0.52f + (yOffset * 0.7f), 0.93f),
                PoseLandmark.RIGHT_HIP to TrackedLandmark(PoseLandmark.RIGHT_HIP, 0.55f, 0.52f + (yOffset * 0.7f), 0.93f),
                PoseLandmark.LEFT_KNEE to TrackedLandmark(PoseLandmark.LEFT_KNEE, 0.46f, 0.62f, 0.90f),
                PoseLandmark.RIGHT_KNEE to TrackedLandmark(PoseLandmark.RIGHT_KNEE, 0.54f, 0.62f, 0.90f),
                PoseLandmark.LEFT_ANKLE to TrackedLandmark(PoseLandmark.LEFT_ANKLE, 0.47f, 0.75f, 0.88f),
                PoseLandmark.RIGHT_ANKLE to TrackedLandmark(PoseLandmark.RIGHT_ANKLE, 0.53f, 0.75f, 0.88f)
            )

            val pose = TrackedPose(
                landmarks = landmarks,
                imageWidth = 720,
                imageHeight = 1280,
                isTrackingValid = true,
                leftElbowAngle = 85.0 + (yOffset * 100.0),
                rightElbowAngle = 85.0 + (yOffset * 100.0),
                hipAlignmentAngle = 175.0
            )

            list.add(TimestampedPose(t, pose))
            t += 100 // 10 fps sample rate for smooth interpolation
        }

        return list
    }
}
