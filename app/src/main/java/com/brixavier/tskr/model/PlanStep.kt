package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a lightweight checklist Step belonging to a Plan.
 *
 * Steps represent direct Plan checklist items and do NOT create tasks, reminders, or alarms.
 *
 * @property id Unique identifier for this step.
 * @property planId Foreign key referencing the parent Plan.
 * @property text The step description/label.
 * @property isCompleted Whether this step is checked off.
 * @property sortOrder Deterministic display ordering position.
 * @property createdAtMs Epoch timestamp in milliseconds of creation.
 * @property updatedAtMs Epoch timestamp in milliseconds of last modification.
 */
@Entity(
    tableName = "plan_steps",
    foreignKeys = [
        ForeignKey(
            entity = Plan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["planId", "sortOrder"])
    ]
)
data class PlanStep(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val text: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
