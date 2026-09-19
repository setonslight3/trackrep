package com.setons.trackrep.review

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

data class RecordedWorkoutSession(
    val id: String,
    val exerciseName: String,
    val videoPath: String?,
    val durationSeconds: Int,
    val repCount: Int,
    val dateString: String,
    val detectedFlaws: List<FormFlaw>
)

object SessionReviewRepository {
    private val sessions = mutableListOf<RecordedWorkoutSession>()

    init {
        // Pre-populate with a sample session demonstrating form flaw analysis
        sessions.add(
            RecordedWorkoutSession(
                id = "sample_session_1",
                exerciseName = "Push-up Set #1",
                videoPath = null, // Demo mode playback preview
                durationSeconds = 24,
                repCount = 10,
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
                )
            )
        )
    }

    fun getAllSessions(): List<RecordedWorkoutSession> = sessions.toList()

    fun getSessionById(id: String): RecordedWorkoutSession? = sessions.find { it.id == id }

    fun addSession(session: RecordedWorkoutSession) {
        sessions.add(0, session)
    }
}
