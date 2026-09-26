package com.brixavier.tskr.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.sound.NotificationIdHelper
import com.brixavier.tskr.widget.TskRWidget
import com.brixavier.tskr.widget.WidgetDataResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lightweight BroadcastReceiver responsible for processing direct notification actions
 * (e.g. Complete and Snooze) asynchronously without opening an Activity.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRx"

        const val ACTION_COMPLETE = "com.brixavier.tskr.action.COMPLETE_NOTIFICATION_TASK"
        const val ACTION_SNOOZE = "com.brixavier.tskr.action.SNOOZE_NOTIFICATION_TASK"

        const val EXTRA_OCCURRENCE_ID = "EXTRA_OCCURRENCE_ID"
        const val EXTRA_TEMPLATE_ID = "EXTRA_TEMPLATE_ID"
        const val EXTRA_ALARM_STAGE = "EXTRA_ALARM_STAGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_COMPLETE && action != ACTION_SNOOZE) {
            Log.d(TAG, "Unrecognized action '$action'. Ignoring.")
            return
        }

        val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE_ID)
            ?: intent.getStringExtra(ReminderScheduler.EXTRA_OCCURRENCE_ID)
            ?: intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID)

        if (occurrenceId.isNullOrBlank()) {
            Log.d(TAG, "Missing occurrenceId extra for action '$action'. Ignoring.")
            return
        }

        val pendingResult = goAsync()
        val database = ReminderDatabase.getDatabase(context)
        val scheduler = ReminderScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // HARD OCCURRENCE SAFETY GUARD: Load exact occurrence by ID
                val occurrence = database.reminderDao().getOccurrenceById(occurrenceId)

                if (occurrence == null) {
                    Log.d(TAG, "Occurrence '$occurrenceId' not found in database. Aborting $action.")
                    cancelAllStageNotifications(context, occurrenceId)
                    return@launch
                }

                if (!occurrence.isPending()) {
                    Log.d(TAG, "Occurrence '$occurrenceId' is not pending (status: ${occurrence.status}). Aborting $action.")
                    cancelAllStageNotifications(context, occurrenceId)
                    return@launch
                }

                when (action) {
                    ACTION_COMPLETE -> handleCompleteAction(context, database, scheduler, occurrence)
                    ACTION_SNOOZE -> handleSnoozeAction(context, database, scheduler, occurrence)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification action '$action' for occurrence '$occurrenceId'", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleCompleteAction(
        context: Context,
        database: ReminderDatabase,
        scheduler: ReminderScheduler,
        occurrence: ReminderOccurrence
    ) {
        val targetId = occurrence.id

        // Optimistic widget state integration
        WidgetDataResolver.markCompleting(targetId)
        try {
            TskRWidget().updateAll(context)

            // Authoritative Room completion transaction
            val nextOccurrence = database.reminderDao().completeOccurrenceTransaction(targetId)
            scheduler.cancelOccurrenceAlarms(targetId)

            if (nextOccurrence != null) {
                scheduler.scheduleOccurrenceAlarms(nextOccurrence)
            }

            cancelAllStageNotifications(context, targetId)
        } finally {
            WidgetDataResolver.clearCompleting(targetId)
            TskRWidget().updateAll(context)
        }
    }

    private suspend fun handleSnoozeAction(
        context: Context,
        database: ReminderDatabase,
        scheduler: ReminderScheduler,
        occurrence: ReminderOccurrence
    ) {
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val snoozeEpochMs = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val updated = occurrence.copy(
            date = snoozeTime.format(dateFormatter),
            time = snoozeTime.format(timeFormatter),
            snoozedUntilMs = snoozeEpochMs,
            snoozeCount = occurrence.snoozeCount + 1
        )

        database.reminderDao().updateOccurrence(updated)
        scheduler.scheduleOccurrenceAlarms(updated)

        cancelAllStageNotifications(context, occurrence.id)
        TskRWidget().updateAll(context)
    }

    private fun cancelAllStageNotifications(context: Context, occurrenceId: String) {
        if (occurrenceId.isBlank()) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationIdHelper.getNotificationId(occurrenceId, ReminderScheduler.STAGE_7_DAYS))
        notificationManager.cancel(NotificationIdHelper.getNotificationId(occurrenceId, ReminderScheduler.STAGE_24_HOURS))
        notificationManager.cancel(NotificationIdHelper.getNotificationId(occurrenceId, ReminderScheduler.STAGE_DUE_NOW))
        notificationManager.cancel(occurrenceId.hashCode())
    }
}
