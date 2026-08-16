package com.brixavier.tskr.receiver

import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.brixavier.tskr.AlarmActivity
import com.brixavier.tskr.MainActivity
import com.brixavier.tskr.engine.PersonalityEngine
import com.brixavier.tskr.scheduler.ReminderScheduler

/**
 * BroadcastReceiver triggered by AlarmManager.
 * Automatically handles displaying highly legible, eye-catching reminder notifications.
 * Configured to instantly open AlarmActivity via Full Screen Intent.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "tsk_r_mission_critical_v3"
        private const val CHANNEL_NAME = "Mission Critical Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val reminderTitle = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Upcoming Task"
        val stage = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_STAGE) ?: ""

        Log.d(TAG, "Alarm triggered! ID: $reminderId, Title: $reminderTitle, Stage: $stage")

        // Build elegant notification content based on the alarm stage
        val warningMessage = when (stage) {
            ReminderScheduler.STAGE_7_DAYS -> "Early Warning: 7 Days Remaining!"
            ReminderScheduler.STAGE_24_HOURS -> "Action Required: 24 Hour Countdown!"
            ReminderScheduler.STAGE_DUE_NOW -> "Mission Start: It is time!"
            else -> "Upcoming event scheduled."
        }
        
        val personalityQuote = PersonalityEngine.getRandomQuote()
        val fullMessage = "$warningMessage\n\n\"$personalityQuote\""

        showNotification(context, reminderId.hashCode(), reminderTitle, fullMessage, stage, reminderId)
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        stage: String,
        reminderId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Notification channel with MAX importance for breakthrough
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Tsk.R critical mission alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setBypassDnd(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setSound(alarmSound, android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        notificationManager.createNotificationChannel(channel)

        // Tap notification opens AlarmActivity (Mission Mode) for the correct reminder
        val openAppIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderScheduler.EXTRA_REMINDER_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_ALARM_STAGE, stage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Full-Screen Intent to trigger AlarmActivity immediately (even over lock screen)
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderScheduler.EXTRA_REMINDER_TITLE, title)
            putExtra(ReminderScheduler.EXTRA_ALARM_STAGE, stage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1000, // Ensure unique request code from normal tap action
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Personality-driven messaging
        val emoji = when (stage) {
            ReminderScheduler.STAGE_7_DAYS -> "⚠️"
            ReminderScheduler.STAGE_24_HOURS -> "🚨"
            else -> "🎯"
        }
        val notificationTitle = "$emoji $title"
        
        // Android standard application launcher icon is used as the small icon
        val smallIconResId = android.R.drawable.ic_lock_idle_alarm

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconResId)
            .setContentTitle(notificationTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
    }
}
