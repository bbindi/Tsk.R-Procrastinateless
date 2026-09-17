package com.brixavier.tskr.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.scheduler.ReminderScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CompleteTaskActionCallback : ActionCallback {

    companion object {
        val TargetOccurrenceIdKey = ActionParameters.Key<String>("TARGET_OCCURRENCE_ID")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val occurrenceId = parameters[TargetOccurrenceIdKey] ?: return
        val database = ReminderDatabase.getDatabase(context)
        val scheduler = ReminderScheduler(context)

        // Verify occurrence exists and is PENDING (Stale/race protection)
        val occurrence = database.reminderDao().getOccurrenceById(occurrenceId)
        if (occurrence == null || !occurrence.isPending()) {
            TskRWidget().updateAll(context)
            return
        }

        // Execute occurrence completion transaction
        val nextOccurrence = database.reminderDao().completeOccurrenceTransaction(occurrenceId)
        scheduler.cancelOccurrenceAlarms(occurrenceId)

        // If next recurring occurrence was generated, schedule its alarms
        if (nextOccurrence != null) {
            scheduler.scheduleOccurrenceAlarms(nextOccurrence)
        }

        // Immediate widget update
        TskRWidget().updateAll(context)
    }
}

class SnoozeTaskActionCallback : ActionCallback {

    companion object {
        val TargetOccurrenceIdKey = ActionParameters.Key<String>("TARGET_OCCURRENCE_ID")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val occurrenceId = parameters[TargetOccurrenceIdKey] ?: return
        val database = ReminderDatabase.getDatabase(context)
        val scheduler = ReminderScheduler(context)

        // Verify occurrence exists and is PENDING (Stale/race protection)
        val occurrence = database.reminderDao().getOccurrenceById(occurrenceId)
        if (occurrence == null || !occurrence.isPending()) {
            TskRWidget().updateAll(context)
            return
        }

        // Snooze by EXACTLY 5 MINUTES (Preserving Tsk.R 5-minute snooze invariant)
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        val snoozeEpochMs = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val snoozedOccurrence = occurrence.copy(
            date = snoozeTime.format(dateFormatter),
            time = snoozeTime.format(timeFormatter),
            snoozedUntilMs = snoozeEpochMs
        )

        database.reminderDao().updateOccurrence(snoozedOccurrence)
        scheduler.scheduleOccurrenceAlarms(snoozedOccurrence)

        // Immediate widget update
        TskRWidget().updateAll(context)
    }
}
