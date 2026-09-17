package com.brixavier.tskr.receiver

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.brixavier.tskr.AlarmActivity
import com.brixavier.tskr.MainActivity
import com.brixavier.tskr.data.ReminderDatabase

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityIntensity
import com.brixavier.tskr.personality.UrgencyLevel
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.sound.NotificationChannelManager
import com.brixavier.tskr.sound.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager.
 * Automatically handles displaying highly legible, eye-catching reminder notifications.
 * Configured to instantly open AlarmActivity via Full Screen Intent.
 * Updated in V2 Phase 3 to verify occurrence status and ignore stale/deleted alarms.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "tsk_r_mission_critical_v3"
        private const val CHANNEL_NAME = "Mission Critical Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getStringExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID)
            ?: intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID)
            ?: return
        val reminderTitle = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Upcoming Task"
        val stage = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_STAGE) ?: ""
        val priority = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_PRIORITY)

        val pendingResult = goAsync()
        val database = ReminderDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verify occurrence status in database to ignore stale or deleted alarms
                val occurrence = database.reminderDao().getOccurrenceById(occurrenceId)
                if (occurrence != null) {
                    if (!occurrence.isPending()) {
                        Log.d(TAG, "Occurrence $occurrenceId is not pending (status: ${occurrence.status}). Ignoring stale alarm.")
                        return@launch
                    }
                } else {
                    // Fallback check for legacy V1 reminder
                    val reminder = database.reminderDao().getReminderById(occurrenceId)
                    if (reminder == null || !reminder.isActive) {
                        Log.d(TAG, "Target $occurrenceId is missing or inactive. Ignoring stale alarm.")
                        return@launch
                    }
                }

                Log.d(TAG, "Alarm triggered! ID: $occurrenceId, Title: $reminderTitle, Stage: $stage, Priority: $priority")

                // Build elegant notification content based on the alarm stage
                val warningMessage = when (stage) {
                    ReminderScheduler.STAGE_7_DAYS -> "Early Warning: 7 Days Remaining!"
                    ReminderScheduler.STAGE_24_HOURS -> "Action Required: 24 Hour Countdown!"
                    ReminderScheduler.STAGE_DUE_NOW -> "Mission Start: It is time!"
                    else -> "Upcoming event scheduled."
                }

                val ctxAction = when (stage) {
                    ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
                    else -> ActionType.MISSION_MODE_DUE
                }
                val isImportant = Reminder.isImportant(priority)
                val ctx = PersonalityContext(
                    actionType = ctxAction,
                    intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.PLAYFUL,
                    urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL
                )
                val personalityEngine = PersonalityEngine()
                val personalityQuote = personalityEngine.getMessage(ctx).text
                val fullMessage = "$warningMessage\n\n\"$personalityQuote\""

                val notificationId = getNotificationId(occurrenceId, stage)
                showNotification(context, notificationId, reminderTitle, fullMessage, stage, occurrenceId, priority)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm receive for ID: $occurrenceId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Generates a deterministic, stage-aware notification ID.
     * Uses a 24-bit mask to prevent integer overflow when multiplied by 10.
     */
    private fun getNotificationId(id: String, stage: String): Int {
        val hash = id.hashCode() and 0x00ffffff
        val offset = when (stage) {
            ReminderScheduler.STAGE_7_DAYS -> 1
            ReminderScheduler.STAGE_24_HOURS -> 2
            ReminderScheduler.STAGE_DUE_NOW -> 3
            else -> 9
        }
        return hash * 10 + offset
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        stage: String,
        reminderId: String,
        priority: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isImportant = Reminder.isImportant(priority)
        val soundType = if (isImportant) SoundType.IMPORTANT else SoundType.NORMAL
        val channelId = NotificationChannelManager.getOrCreateNotificationChannel(context, soundType)

        val openAppIntent = if (isImportant) {
            // IMPORTANT: Open full Mission Mode
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID, reminderId)
                putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderScheduler.EXTRA_REMINDER_TITLE, title)
                putExtra(ReminderScheduler.EXTRA_ALARM_STAGE, stage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            // NORMAL: Open app flow (Reminder Detail)
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Full-Screen Intent to trigger AlarmActivity immediately for IMPORTANT tasks
        val fullScreenPendingIntent = if (isImportant) {
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID, reminderId)
                putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderScheduler.EXTRA_REMINDER_TITLE, title)
                putExtra(ReminderScheduler.EXTRA_ALARM_STAGE, stage)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
            PendingIntent.getActivity(
                context,
                notificationId + 1000,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        // Personality-driven messaging
        val emoji = when (stage) {
            ReminderScheduler.STAGE_7_DAYS -> "⚠️"
            ReminderScheduler.STAGE_24_HOURS -> "🚨"
            else -> "🎯"
        }
        val notificationTitle = "$emoji $title"
        
        // Android standard application launcher icon is used as the small icon
        val smallIconResId = R.drawable.ic_lock_idle_alarm

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(notificationTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .apply {
                if (Reminder.isImportant(priority)) {
                    setFullScreenIntent(fullScreenPendingIntent, true)
                }
            }
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
    }
}
