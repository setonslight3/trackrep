package com.setons.trackrep.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.setons.trackrep.MainActivity
import com.setons.trackrep.schedule.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager to post workout reminders.
 */
class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val repository = UserProfileRepository.getInstance(context)
            val profile = repository.getProfile()

            if (!profile.remindersEnabled) return@launch

            val todayWorkout = repository.getTodayWorkout()
            val isRestDay = todayWorkout?.isRestDay ?: false

            val title = if (isRestDay) {
                "🌿 TrackRep • Active Recovery Day"
            } else {
                "⚡ TrackRep • Time to Train!"
            }

            val body = if (isRestDay) {
                "Today is scheduled for rest and muscle consolidation. Take 10 minutes to stretch and hydrate."
            } else {
                val routineName = todayWorkout?.routineName ?: "Daily Calisthenics"
                "Today's session: $routineName. Your adaptive coach is ready!"
            }

            showNotification(context, title, body)

            // Schedule next reminder
            WorkoutReminderScheduler.scheduleNextReminder(context)
        }
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val channelId = "trackrep_workout_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for scheduled TrackRep calisthenics sessions"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}