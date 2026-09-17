package com.brixavier.tskr.model

/**
 * Domain model representing the calculated progress of a Plan.
 *
 * Progress is based entirely on master Task definitions (Reminder templates).
 * - Total Tasks: Number of templates assigned to the Plan.
 * - Completed Tasks: Number of those templates where `isActive == false` (e.g., one-time tasks fully completed).
 * - Recurring Tasks: Count as exactly 1 task, and remain incomplete (`isActive == true`) as long as the recurring cycle continues.
 *
 * @property planId The unique identifier of the Plan.
 * @property completedTasks The number of fully completed task definitions.
 * @property totalTasks The total number of task definitions assigned to the Plan.
 */
data class PlanProgress(
    val planId: String,
    val completedTasks: Int,
    val totalTasks: Int
) {
    /**
     * Normalized progress value from 0.0 to 1.0.
     * Guaranteed to be 0.0 if there are no tasks, preventing divide-by-zero.
     */
    val progressRatio: Float
        get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0.0f
}
