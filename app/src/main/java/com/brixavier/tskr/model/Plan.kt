package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a higher-level goal or Plan.
 * Plans are optional organizational buckets for Reminder tasks.
 *
 * @property id Unique identifier for the plan.
 * @property name The user-defined goal or objective name.
 * @property description Optional details about the plan.
 * @property icon Reference to an icon or emoji (default "").
 * @property deadline Optional deadline date for the plan.
 * @property status The status of the plan ("ACTIVE", "COMPLETED", "ARCHIVED").
 * @property createdAtMs Epoch timestamp in milliseconds of creation.
 */
@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "",
    val deadline: String? = null,
    val status: String = STATUS_ACTIVE,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ARCHIVED = "ARCHIVED"
    }

    fun isActive(): Boolean = status == STATUS_ACTIVE
    fun isCompleted(): Boolean = status == STATUS_COMPLETED
}
