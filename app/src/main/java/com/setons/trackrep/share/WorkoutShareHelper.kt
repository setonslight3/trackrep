package com.setons.trackrep.share

import android.content.Context
import android.content.Intent
import com.setons.trackrep.data.local.entity.SetRecordEntity
import com.setons.trackrep.data.local.entity.WorkoutSessionEntity

/**
 * Generates formatted, readable workout summary cards and triggers native Android sharing.
 */
object WorkoutShareHelper {

    /**
     * Formats a workout session into an expressive, readable summary.
     */
    fun formatWorkoutShareText(
        session: WorkoutSessionEntity,
        sets: List<SetRecordEntity> = emptyList()
    ): String {
        val durationMin = session.durationSeconds / 60
        val durationSec = session.durationSeconds % 60
        val durationStr = if (durationMin > 0) "${durationMin}m ${durationSec}s" else "${durationSec}s"

        val ratingEmoji = when (session.perceivedRating) {
            "TOO_EASY" -> "🟢 Easy Pace"
            "JUST_RIGHT" -> "🎯 Target Zone"
            "DIFFICULT" -> "🔥 High Intensity"
            "COULD_NOT_COMPLETE" -> "⚡ Maximum Effort"
            else -> "💪 Completed"
        }

        val setsCount = if (sets.isNotEmpty()) sets.size else 1
        val setCountLabel = if (setsCount == 1) "1 Set" else "$setsCount Sets"

        val sb = StringBuilder()
        sb.append("🏆 TrackRep Workout Completed!\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💪 Exercise: ${session.exerciseName}\n")
        if (!session.routineName.isNullOrBlank()) {
            sb.append("📋 Routine: ${session.routineName}\n")
        }
        sb.append("📅 Date: ${session.dateString}\n")
        sb.append("⏱️ Duration: $durationStr\n")
        sb.append("📊 Performance: $setCountLabel • ${session.totalValidReps} Valid Reps\n")
        sb.append("🎯 Form Consistency: ${session.averageFormScore}%\n")
        sb.append("⚡ Intensity: $ratingEmoji\n")

        if (sets.isNotEmpty()) {
            sb.append("\n📝 Set Breakdown:\n")
            sets.forEach { set ->
                sb.append("• Set ${set.setNumber}: ${set.validReps} reps (${set.formConsistencyPercent}% form)\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛡️ 100% Local-First & Form-Diagnosed by TrackRep AI\n")
        sb.append("📲 https://github.com/Seton-S/TrackRep\n")

        return sb.toString()
    }

    /**
     * Launches the Android native share chooser for a completed workout.
     */
    fun shareWorkout(
        context: Context,
        session: WorkoutSessionEntity,
        sets: List<SetRecordEntity> = emptyList()
    ) {
        val shareText = formatWorkoutShareText(session, sets)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My TrackRep Workout: ${session.exerciseName}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share Workout Summary")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
