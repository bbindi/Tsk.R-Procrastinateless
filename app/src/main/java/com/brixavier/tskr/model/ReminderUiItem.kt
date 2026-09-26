package com.brixavier.tskr.model

/**
 * Combined presentation model for UI rendering that pairs an occurrence instance
 * with its parent template metadata and alert configuration.
 */
data class ReminderUiItem(
    val occurrenceId: String,
    val templateId: String?,
    val title: String,
    val date: String,
    val time: String,
    val priority: String,
    val notes: String,
    val subTasksRaw: String,
    val motivation: String,
    val status: String,
    val completedAtMs: Long?,
    val recurrenceType: String = Reminder.RECURRENCE_NONE,
    val recurrenceInterval: Int = 1,
    val notify7Days: Boolean = false,
    val notify24Hours: Boolean = false,
    val notifyDueNow: Boolean = true,
    val planId: String? = null,
    val planName: String? = null,
    val planIcon: String? = null,
    val snoozeCount: Int = 0
) {
    fun isPending(): Boolean = status == ReminderOccurrence.STATUS_PENDING
    fun isCompleted(): Boolean = status == ReminderOccurrence.STATUS_COMPLETED
    fun isRecurring(): Boolean = recurrenceType != Reminder.RECURRENCE_NONE

    fun isImportant(): Boolean = Reminder.isImportant(priority)

    fun getPriorityEmoji(): String {
        return if (isImportant()) "🔥" else "🙂"
    }

    fun getPriorityLabel(): String {
        return if (isImportant()) "IMPORTANT" else "NORMAL"
    }

    fun getPriorityDisplayName(): String {
        return "${getPriorityEmoji()} ${getPriorityLabel().lowercase().replaceFirstChar { it.uppercase() }}"
    }

    fun getRecurrenceDisplayName(): String {
        return when (recurrenceType) {
            Reminder.RECURRENCE_DAILY -> if (recurrenceInterval == 1) "🔁 Daily" else "🔁 Every $recurrenceInterval days"
            Reminder.RECURRENCE_WEEKLY -> if (recurrenceInterval == 1) "🔁 Weekly" else "🔁 Every $recurrenceInterval weeks"
            Reminder.RECURRENCE_MONTHLY -> if (recurrenceInterval == 1) "🔁 Monthly" else "🔁 Every $recurrenceInterval months"
            Reminder.RECURRENCE_CUSTOM_DAYS -> "🔁 Every $recurrenceInterval days"
            else -> ""
        }
    }

    fun getRecurrenceCleanName(): String {
        return when (recurrenceType) {
            Reminder.RECURRENCE_DAILY -> if (recurrenceInterval == 1) "Daily" else "Every $recurrenceInterval days"
            Reminder.RECURRENCE_WEEKLY -> if (recurrenceInterval == 1) "Weekly" else "Every $recurrenceInterval weeks"
            Reminder.RECURRENCE_MONTHLY -> if (recurrenceInterval == 1) "Monthly" else "Every $recurrenceInterval months"
            Reminder.RECURRENCE_CUSTOM_DAYS -> "Every $recurrenceInterval days"
            else -> ""
        }
    }
}
