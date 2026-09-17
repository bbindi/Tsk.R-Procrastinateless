package com.brixavier.tskr.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Data class representing a distinct Note Section belonging to a Plan workspace.
 *
 * Each note section stores a titled document content (plain text or markdown/rich-text string).
 *
 * @property id Unique identifier for this note section.
 * @property planId Foreign key referencing the parent Plan.
 * @property title Section heading or title (e.g., "Overview", "Discussion", "Decisions").
 * @property content Document content for this note section.
 * @property sortOrder Deterministic display ordering position.
 * @property createdAtMs Epoch timestamp in milliseconds of creation.
 * @property updatedAtMs Epoch timestamp in milliseconds of last modification.
 */
@Entity(
    tableName = "plan_note_sections",
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
data class PlanNoteSection(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val title: String,
    val content: String = "",
    val sortOrder: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
