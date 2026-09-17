package com.brixavier.tskr.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service to handle scheduling and cancelling of alarms using Android's AlarmManager.
 * Updated in V2 Extension to support user-configurable alert stage scheduling.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    companion object {
        private const val TAG = "ReminderScheduler"
        const val EXTRA_OCCURRENCE_ID = "EXTRA_OCCURRENCE_ID"
        const val EXTRA_TEMPLATE_ID = "EXTRA_TEMPLATE_ID"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID" // Retained for backward compatibility
        const val EXTRA_REMINDER_TITLE = "EXTRA_REMINDER_TITLE"
        const val EXTRA_ALARM_STAGE = "EXTRA_ALARM_STAGE"
        const val EXTRA_REMINDER_PRIORITY = "EXTRA_REMINDER_PRIORITY"
        
        const val STAGE_7_DAYS = "STAGE_7_DAYS"
        const val STAGE_24_HOURS = "STAGE_24_HOURS"
        const val STAGE_DUE_NOW = "STAGE_DUE_NOW"

        /**
         * Generates a unique integer request code.
         * Uses a 24-bit mask of the hashCode to avoid overflow when multiplied by 10.
         */
        fun getRequestCode(id: String, stage: String): Int {
            val hash = id.hashCode() and 0x00ffffff
            val offset = when (stage) {
                STAGE_7_DAYS -> 1
                STAGE_24_HOURS -> 2
                STAGE_DUE_NOW -> 3
                else -> 9
            }
            return hash * 10 + offset
        }
    }

    /**
     * Schedules warning alarms for a [ReminderOccurrence] based on its active alert stage configuration:
     * 1. 7 days before the event (if enabled).
     * 2. 24 hours before the event (if enabled).
     * 3. At the exact event time (if enabled).
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleOccurrenceAlarms(occurrence: ReminderOccurrence) {
        if (!occurrence.isPending()) {
            cancelOccurrenceAlarms(occurrence)
            return
        }

        try {
            val dateTimeStr = "${occurrence.date} ${occurrence.time}"
            val eventDateTime = LocalDateTime.parse(dateTimeStr, formatter)
            val eventEpochMs = eventDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val currentTimeMs = System.currentTimeMillis()

            // 1. Stage: 7 Days Before
            val sevenDaysBeforeMs = eventDateTime.minusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (occurrence.notify7Days && sevenDaysBeforeMs > currentTimeMs) {
                scheduleOccurrenceAlarm(
                    occurrence = occurrence,
                    triggerTimeMs = sevenDaysBeforeMs,
                    requestCode = getRequestCode(occurrence.id, STAGE_7_DAYS),
                    stage = STAGE_7_DAYS
                )
            } else {
                cancelAlarm(getRequestCode(occurrence.id, STAGE_7_DAYS))
            }

            // 2. Stage: 24 Hours Before
            val twentyFourHoursBeforeMs = eventDateTime.minusHours(24).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (occurrence.notify24Hours && twentyFourHoursBeforeMs > currentTimeMs) {
                scheduleOccurrenceAlarm(
                    occurrence = occurrence,
                    triggerTimeMs = twentyFourHoursBeforeMs,
                    requestCode = getRequestCode(occurrence.id, STAGE_24_HOURS),
                    stage = STAGE_24_HOURS
                )
            } else {
                cancelAlarm(getRequestCode(occurrence.id, STAGE_24_HOURS))
            }

            // 3. Stage: Due Now
            if (occurrence.notifyDueNow && eventEpochMs > currentTimeMs) {
                scheduleOccurrenceAlarm(
                    occurrence = occurrence,
                    triggerTimeMs = eventEpochMs,
                    requestCode = getRequestCode(occurrence.id, STAGE_DUE_NOW),
                    stage = STAGE_DUE_NOW
                )
            } else {
                cancelAlarm(getRequestCode(occurrence.id, STAGE_DUE_NOW))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing or scheduling alarms for occurrence: ${occurrence.title}", e)
        }
    }

    /**
     * Cancels all scheduled alarms for the given [ReminderOccurrence].
     */
    fun cancelOccurrenceAlarms(occurrence: ReminderOccurrence) {
        cancelOccurrenceAlarms(occurrence.id)
    }

    /**
     * Cancels all scheduled alarms for a given occurrence ID.
     */
    fun cancelOccurrenceAlarms(occurrenceId: String) {
        cancelAlarm(getRequestCode(occurrenceId, STAGE_7_DAYS))
        cancelAlarm(getRequestCode(occurrenceId, STAGE_24_HOURS))
        cancelAlarm(getRequestCode(occurrenceId, STAGE_DUE_NOW))
        Log.d(TAG, "Cancelled warning cascade for occurrence ID: '$occurrenceId'")
    }

    /**
     * Schedules warning alarms for a legacy V1 [Reminder] template based on its alert stage configuration.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarms(reminder: Reminder) {
        if (!reminder.isActive) {
            cancelAlarms(reminder)
            return
        }

        try {
            val dateTimeStr = "${reminder.date} ${reminder.time}"
            val eventDateTime = LocalDateTime.parse(dateTimeStr, formatter)
            val eventEpochMs = eventDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val currentTimeMs = System.currentTimeMillis()

            // 1. Stage: 7 Days Before
            val sevenDaysBeforeMs = eventDateTime.minusDays(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (reminder.notify7Days && sevenDaysBeforeMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = sevenDaysBeforeMs,
                    requestCode = getRequestCode(reminder.id, STAGE_7_DAYS),
                    stage = STAGE_7_DAYS
                )
            } else {
                cancelAlarm(getRequestCode(reminder.id, STAGE_7_DAYS))
            }

            // 2. Stage: 24 Hours Before
            val twentyFourHoursBeforeMs = eventDateTime.minusHours(24).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (reminder.notify24Hours && twentyFourHoursBeforeMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = twentyFourHoursBeforeMs,
                    requestCode = getRequestCode(reminder.id, STAGE_24_HOURS),
                    stage = STAGE_24_HOURS
                )
            } else {
                cancelAlarm(getRequestCode(reminder.id, STAGE_24_HOURS))
            }

            // 3. Stage: Due Now
            if (reminder.notifyDueNow && eventEpochMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = eventEpochMs,
                    requestCode = getRequestCode(reminder.id, STAGE_DUE_NOW),
                    stage = STAGE_DUE_NOW
                )
            } else {
                cancelAlarm(getRequestCode(reminder.id, STAGE_DUE_NOW))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing or scheduling alarms for reminder: ${reminder.title}", e)
        }
    }

    /**
     * Cancels all scheduled alarms for the given V1 [Reminder].
     */
    fun cancelAlarms(reminder: Reminder) {
        cancelAlarm(getRequestCode(reminder.id, STAGE_7_DAYS))
        cancelAlarm(getRequestCode(reminder.id, STAGE_24_HOURS))
        cancelAlarm(getRequestCode(reminder.id, STAGE_DUE_NOW))
        Log.d(TAG, "Cancelled warning cascade for: '${reminder.title}'")
    }

    /**
     * Helper to schedule an individual occurrence alarm.
     */
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleOccurrenceAlarm(
        occurrence: ReminderOccurrence,
        triggerTimeMs: Long,
        requestCode: Int,
        stage: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_OCCURRENCE_ID, occurrence.id)
            putExtra(EXTRA_TEMPLATE_ID, occurrence.templateId ?: "")
            putExtra(EXTRA_REMINDER_ID, occurrence.id)
            putExtra(EXTRA_REMINDER_TITLE, occurrence.title)
            putExtra(EXTRA_ALARM_STAGE, stage)
            putExtra(EXTRA_REMINDER_PRIORITY, occurrence.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }

        Log.d(TAG, "Scheduled occurrence $stage for '${occurrence.title}' (ID: $requestCode, Exact: $canScheduleExact)")
    }

    /**
     * Helper to schedule an individual legacy reminder alarm.
     */
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(
        reminder: Reminder,
        triggerTimeMs: Long,
        requestCode: Int,
        stage: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_OCCURRENCE_ID, reminder.id)
            putExtra(EXTRA_TEMPLATE_ID, reminder.id)
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_ALARM_STAGE, stage)
            putExtra(EXTRA_REMINDER_PRIORITY, reminder.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }

        Log.d(TAG, "Scheduled $stage for '${reminder.title}' (ID: $requestCode, Exact: $canScheduleExact)")
    }

    /**
     * Helper to cancel an individual alarm.
     */
    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
