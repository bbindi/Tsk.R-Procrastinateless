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
 * @property category Serialized PlanCategory string ("WORK", "PERSONAL", "FINANCE", etc.).
 * @property categorySource Serialized CategorySource string ("AUTOMATIC" vs "MANUAL").
 * @property color Serialized PlanColor string ("BLUE", "GREEN", "SLATE", etc.).
 * @property colorSource Serialized ColorSource string ("CATEGORY" vs "MANUAL").
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
    val createdAtMs: Long = System.currentTimeMillis(),
    val category: String = "PERSONAL",
    val categorySource: String = "AUTOMATIC",
    val color: String = "SLATE",
    val colorSource: String = "CATEGORY"
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ARCHIVED = "ARCHIVED"
    }

    fun isActive(): Boolean = status == STATUS_ACTIVE
    fun isCompleted(): Boolean = status == STATUS_COMPLETED

    fun getPlanCategory(): PlanCategory = PlanCategory.fromString(category)
    fun getCategorySource(): CategorySource = CategorySource.fromString(categorySource)
    fun getPlanColor(): PlanColor = PlanColor.fromString(color)
    fun getColorSource(): ColorSource = ColorSource.fromString(colorSource)

    fun resolveEffectiveColor(): PlanColor {
        return if (getColorSource() == ColorSource.CATEGORY) {
            PlanCategoryColorMapping.getDefaultColor(getPlanCategory())
        } else {
            getPlanColor()
        }
    }
}
