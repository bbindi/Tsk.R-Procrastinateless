package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a concrete scheduled instance/occurrence of a Reminder task.
 *
 * @property id Unique identifier for this occurrence.
 * @property templateId Foreign key referencing the parent Reminder template (nullable to preserve history if template is deleted).
 * @property title Cached title for historical display.
 * @property priority Priority level ("NORMAL" or "IMPORTANT").
 * @property date Scheduled event date formatted as YYYY-MM-DD.
 * @property time Scheduled event time formatted as HH:MM.
 * @property status Occurrence status ("PENDING", "COMPLETED", "SKIPPED").
 * @property completedAtMs Epoch timestamp in milliseconds when completed.
 * @property subTasksRaw Checklist progress formatted as raw pipe-delimited string.
 * @property snoozedUntilMs Epoch timestamp in milliseconds if snoozed.
 * @property notify7Days Whether 7-day advance warning alert is enabled.
 * @property notify24Hours Whether 24-hour advance warning alert is enabled.
 * @property notifyDueNow Whether at-scheduled-time alert is enabled.
 */
@Entity(
    tableName = "reminder_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Plan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["status"]),
        Index(value = ["date", "time"]),
        Index(value = ["planId"])
    ]
)
data class ReminderOccurrence(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val templateId: String? = null,
    val title: String,
    val priority: String = Reminder.PRIORITY_NORMAL,
    val date: String,
    val time: String,
    val status: String = STATUS_PENDING,
    val completedAtMs: Long? = null,
    val subTasksRaw: String = "",
    val snoozedUntilMs: Long? = null,
    val notify7Days: Boolean = false,
    val notify24Hours: Boolean = false,
    val notifyDueNow: Boolean = true,
    val planId: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_SKIPPED = "SKIPPED"
    }

    fun isPending(): Boolean = status == STATUS_PENDING
    fun isCompleted(): Boolean = status == STATUS_COMPLETED
}
