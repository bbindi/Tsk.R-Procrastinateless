package com.brixavier.tskr.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Service to handle scheduling and cancelling of alarms using Android's AlarmManager.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    companion object {
        private const val TAG = "ReminderScheduler"
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_REMINDER_TITLE = "EXTRA_REMINDER_TITLE"
        const val EXTRA_ALARM_STAGE = "EXTRA_ALARM_STAGE"
        const val EXTRA_REMINDER_PRIORITY = "EXTRA_REMINDER_PRIORITY"
        
        const val STAGE_7_DAYS = "STAGE_7_DAYS"
        const val STAGE_24_HOURS = "STAGE_24_HOURS"
        const val STAGE_DUE_NOW = "STAGE_DUE_NOW"
    }

    /**
     * Schedules a 3-tiered warning cascade:
     * 1. 7 days before the event.
     * 2. 24 hours before the event.
     * 3. At the exact event time (Due Now).
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
            if (sevenDaysBeforeMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = sevenDaysBeforeMs,
                    requestCode = getRequestCode(reminder.id, STAGE_7_DAYS),
                    stage = STAGE_7_DAYS
                )
            }

            // 2. Stage: 24 Hours Before
            val twentyFourHoursBeforeMs = eventDateTime.minusHours(24).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (twentyFourHoursBeforeMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = twentyFourHoursBeforeMs,
                    requestCode = getRequestCode(reminder.id, STAGE_24_HOURS),
                    stage = STAGE_24_HOURS
                )
            }

            // 3. Stage: Due Now
            if (eventEpochMs > currentTimeMs) {
                scheduleAlarm(
                    reminder = reminder,
                    triggerTimeMs = eventEpochMs,
                    requestCode = getRequestCode(reminder.id, STAGE_DUE_NOW),
                    stage = STAGE_DUE_NOW
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing or scheduling alarms for reminder: ${reminder.title}", e)
        }
    }

    /**
     * Cancels all 3 scheduled alarms for the given reminder.
     */
    fun cancelAlarms(reminder: Reminder) {
        cancelAlarm(getRequestCode(reminder.id, STAGE_7_DAYS))
        cancelAlarm(getRequestCode(reminder.id, STAGE_24_HOURS))
        cancelAlarm(getRequestCode(reminder.id, STAGE_DUE_NOW))
        Log.d(TAG, "Cancelled warning cascade for: '${reminder.title}'")
    }

    /**
     * Helper to schedule an individual alarm.
     */
    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(
        reminder: Reminder,
        triggerTimeMs: Long,
        requestCode: Int,
        stage: String
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
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

        // Permission Guard for Exact Alarms (Android 12+)
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } else {
            // Fallback to non-exact scheduling
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

    /**
     * Generates a unique integer request code.
     * Uses a 28-bit mask of the hashCode to avoid overflow when multiplied by 10.
     */
    private fun getRequestCode(id: String, stage: String): Int {
        val hash = id.hashCode() and 0x0fffffff 
        return when (stage) {
            STAGE_7_DAYS -> hash * 10 + 1
            STAGE_24_HOURS -> hash * 10 + 2
            STAGE_DUE_NOW -> hash * 10 + 3
            else -> hash * 10 + 9
        }
    }
}
