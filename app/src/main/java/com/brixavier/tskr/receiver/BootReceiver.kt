package com.brixavier.tskr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when the device finishes booting.
 * Responsible for restoring all active reminder alarms in AlarmManager.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Device reboot detected ($action). Restoring alarms...")

            val pendingResult = goAsync()
            val database = ReminderDatabase.getDatabase(context)
            val scheduler = ReminderScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Retrieve all active (upcoming) reminders
                    val activeReminders = database.reminderDao().getUpcomingReminders()
                    Log.d(TAG, "Found ${activeReminders.size} active reminders to restore.")

                    // 2. Reuse ReminderScheduler to restore the warning cascade
                    // scheduleAlarms is idempotent and handles past-time checks internally.
                    activeReminders.forEach { reminder ->
                        scheduler.scheduleAlarms(reminder)
                        Log.d(TAG, "Restored alarms for: '${reminder.title}'")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
