package com.brixavier.tskr.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.scheduler.ReminderScheduler

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

        // Intercept rapid duplicate taps immediately at entry level
        if (!WidgetDataResolver.markCompleting(occurrenceId)) {
            return
        }

        try {
            // Immediate visual acknowledgement refresh
            TskRWidget().updateAll(context)

            val database = ReminderDatabase.getDatabase(context)
            val scheduler = ReminderScheduler(context)

            // Resolve occurrence directly by ID or fallback template lookup
            var occurrence = database.reminderDao().getOccurrenceById(occurrenceId)
            if (occurrence == null) {
                val pendingList = database.reminderDao().getOccurrencesForTemplate(occurrenceId).filter { it.isPending() }
                if (pendingList.isNotEmpty()) {
                    occurrence = pendingList.first()
                }
            }

            if (occurrence == null || !occurrence.isPending()) {
                return
            }

            // Execute occurrence completion transaction
            val targetId = occurrence.id
            val nextOccurrence = database.reminderDao().completeOccurrenceTransaction(targetId)
            scheduler.cancelOccurrenceAlarms(targetId)

            // If next recurring occurrence was generated, schedule its alarms
            if (nextOccurrence != null) {
                scheduler.scheduleOccurrenceAlarms(nextOccurrence)
            }
        } finally {
            // Guarantee cleanup and reconcile with authoritative Room state
            WidgetDataResolver.clearCompleting(occurrenceId)
            TskRWidget().updateAll(context)
        }
    }
}
