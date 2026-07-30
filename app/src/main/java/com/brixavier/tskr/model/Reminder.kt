package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a Reminder item in the application, configured as a Room Entity.
 *
 * @property id Unique identifier for the reminder.
 * @property title The description or title of the task.
 * @property date The event date formatted as YYYY-MM-DD.
 * @property time The event time formatted as HH:MM.
 * @property isActive Toggle switch state indicating if the reminder is active.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val isActive: Boolean = true,
    val priority: String = "CASUAL",
    val notes: String = "",
    val subTasksRaw: String = "",
    val motivation: String = ""
)
