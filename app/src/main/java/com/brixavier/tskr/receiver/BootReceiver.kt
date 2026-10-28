package com.brixavier.tskr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when the device finishes booting.
 * Responsible for restoring all active pending occurrence alarms in AlarmManager.
 * Updated in V2 Phase 3 to restore [ReminderOccurrence] alarms.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Device reboot or package update detected ($action). Restoring alarms...")

            val pendingResult = goAsync()
            val database = ReminderDatabase.getDatabase(context)
            val scheduler = ReminderScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Retrieve all PENDING occurrences
                    val pendingOccurrences = database.reminderDao().getPendingOccurrences()
                    Log.d(TAG, "Found ${pendingOccurrences.size} pending occurrences to restore.")

                    // 2. Restore warning cascades for active occurrences
                    pendingOccurrences.forEach { occurrence ->
                        scheduler.scheduleOccurrenceAlarms(occurrence)
                        Log.d(TAG, "Restored occurrence alarm for: '${occurrence.title}' (ID: ${occurrence.id})")
                    }

                    // 3. Fallback: Restore legacy active V1 reminders if any
                    val activeReminders = database.reminderDao().getUpcomingReminders()
                    activeReminders.forEach { reminder ->
                        scheduler.scheduleAlarms(reminder)
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
