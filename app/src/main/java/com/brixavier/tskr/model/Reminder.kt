package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a Reminder item/template in the application, configured as a Room Entity.
 *
 * @property id Unique identifier for the reminder template.
 * @property title The description or title of the task.
 * @property date The baseline event date formatted as YYYY-MM-DD.
 * @property time The baseline event time formatted as HH:MM.
 * @property isActive Toggle switch state indicating if the reminder is active.
 * @property recurrenceType Type of recurrence ("NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM_DAYS").
 * @property recurrenceInterval Number of interval units between recurrences (default 1).
 * @property notify7Days Whether 7-day advance warning alert is enabled.
 * @property notify24Hours Whether 24-hour advance warning alert is enabled.
 * @property notifyDueNow Whether at-scheduled-time alert is enabled (required/default true).
 * @property planId Optional foreign key referencing a parent Plan.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Plan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["planId"])
    ]
)
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val isActive: Boolean = true,
    val priority: String = PRIORITY_NORMAL,
    val notes: String = "",
    val subTasksRaw: String = "",
    val motivation: String = "",
    val recurrenceType: String = RECURRENCE_NONE,
    val recurrenceInterval: Int = 1,
    val notify7Days: Boolean = false,
    val notify24Hours: Boolean = false,
    val notifyDueNow: Boolean = true,
    val planId: String? = null
) {
    companion object {
        const val PRIORITY_IMPORTANT = "IMPORTANT"
        const val PRIORITY_NORMAL = "NORMAL"
        
        // Legacy values for mapping
        const val PRIORITY_CRITICAL = "CRITICAL"
        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_CASUAL = "CASUAL"

        // Recurrence Constants
        const val RECURRENCE_NONE = "NONE"
        const val RECURRENCE_DAILY = "DAILY"
        const val RECURRENCE_WEEKLY = "WEEKLY"
        const val RECURRENCE_MONTHLY = "MONTHLY"
        const val RECURRENCE_CUSTOM_DAYS = "CUSTOM_DAYS"

        fun isImportant(priority: String?): Boolean {
            return priority == PRIORITY_IMPORTANT || 
                   priority == PRIORITY_CRITICAL || 
                   priority == PRIORITY_HIGH
        }
    }

    fun isImportant(): Boolean = isImportant(priority)

    fun isNormal(): Boolean {
        return !isImportant()
    }

    fun isRecurring(): Boolean = recurrenceType != RECURRENCE_NONE

    fun getPriorityLabel(): String {
        return if (isImportant()) "IMPORTANT" else "NORMAL"
    }

    fun getPriorityEmoji(): String {
        return if (isImportant()) "🔥" else "🙂"
    }

    fun getPriorityDisplayName(): String {
        return "${getPriorityEmoji()} ${getPriorityLabel().lowercase().replaceFirstChar { it.uppercase() }}"
    }

    fun getRecurrenceDisplayName(): String {
        return when (recurrenceType) {
            RECURRENCE_DAILY -> if (recurrenceInterval == 1) "🔁 Daily" else "🔁 Every $recurrenceInterval days"
            RECURRENCE_WEEKLY -> if (recurrenceInterval == 1) "🔁 Weekly" else "🔁 Every $recurrenceInterval weeks"
            RECURRENCE_MONTHLY -> if (recurrenceInterval == 1) "🔁 Monthly" else "🔁 Every $recurrenceInterval months"
            RECURRENCE_CUSTOM_DAYS -> "🔁 Every $recurrenceInterval days"
            else -> ""
        }
    }
}
