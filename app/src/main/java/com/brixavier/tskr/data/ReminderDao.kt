package com.brixavier.tskr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

/**
 * Data Access Object (DAO) for Room database operations on Reminders and Occurrences.
 */
@Dao
interface ReminderDao {

    // --- Template Queries ---

    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun getAllRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC")
    suspend fun getUpcomingReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC LIMIT :limit")
    suspend fun getUpcomingRemindersLimit(limit: Int): List<Reminder>

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // --- Occurrence Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: ReminderOccurrence): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<ReminderOccurrence>)

    @Update
    suspend fun updateOccurrence(occurrence: ReminderOccurrence)

    @Query("SELECT * FROM reminder_occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrenceById(id: String): ReminderOccurrence?

    @Query("SELECT * FROM reminder_occurrences WHERE templateId = :templateId")
    suspend fun getOccurrencesForTemplate(templateId: String): List<ReminderOccurrence>

    @Query("SELECT * FROM reminder_occurrences WHERE status = 'PENDING' ORDER BY date ASC, time ASC")
    fun getPendingOccurrencesFlow(): Flow<List<ReminderOccurrence>>

    @Query("SELECT * FROM reminder_occurrences WHERE status = 'PENDING' ORDER BY date ASC, time ASC")
    suspend fun getPendingOccurrences(): List<ReminderOccurrence>

    @Query("SELECT * FROM reminder_occurrences WHERE status = 'COMPLETED' ORDER BY completedAtMs DESC")
    fun getCompletedOccurrencesFlow(): Flow<List<ReminderOccurrence>>

    @Delete
    suspend fun deleteOccurrence(occurrence: ReminderOccurrence)

    @Query("DELETE FROM reminder_occurrences WHERE templateId = :templateId AND status = 'PENDING'")
    suspend fun deletePendingOccurrencesForTemplate(templateId: String)

    // --- Plan Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: Plan)

    @Update
    suspend fun updatePlan(plan: Plan)

    @Delete
    suspend fun deletePlanEntity(plan: Plan)

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): Plan?

    @Query("SELECT * FROM plans ORDER BY createdAtMs DESC")
    fun getAllPlansFlow(): Flow<List<Plan>>

    @Query("""
        SELECT
            :planId AS planId,
            COUNT(*) AS totalTasks,
            COALESCE(SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END), 0) AS completedTasks
        FROM plan_steps
        WHERE planId = :planId
    """)
    suspend fun getPlanProgress(planId: String): PlanProgress

    @Query("""
        SELECT
            :planId AS planId,
            COUNT(*) AS totalTasks,
            COALESCE(SUM(CASE WHEN isActive = 0 THEN 1 ELSE 0 END), 0) AS completedTasks
        FROM reminders
        WHERE planId = :planId
    """)
    suspend fun getConnectedTaskProgress(planId: String): PlanProgress

    @Query("""
        SELECT
            p.id AS planId,
            COUNT(s.id) AS totalTasks,
            COALESCE(SUM(CASE WHEN s.isCompleted = 1 THEN 1 ELSE 0 END), 0) AS completedTasks
        FROM plans p
        LEFT JOIN plan_steps s ON p.id = s.planId
        GROUP BY p.id
    """)
    fun getAllPlanProgressFlow(): Flow<List<PlanProgress>>

    // --- Plan Note Section Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteSection(section: PlanNoteSection): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteSections(sections: List<PlanNoteSection>)

    @Update
    suspend fun updateNoteSection(section: PlanNoteSection)

    @Delete
    suspend fun deleteNoteSection(section: PlanNoteSection)

    @Query("SELECT * FROM plan_note_sections ORDER BY sortOrder ASC, createdAtMs ASC")
    fun getAllNoteSectionsFlow(): Flow<List<PlanNoteSection>>

    @Query("SELECT * FROM plan_note_sections WHERE planId = :planId ORDER BY sortOrder ASC, createdAtMs ASC")
    fun getNoteSectionsForPlanFlow(planId: String): Flow<List<PlanNoteSection>>

    @Query("SELECT * FROM plan_note_sections WHERE planId = :planId ORDER BY sortOrder ASC, createdAtMs ASC")
    suspend fun getNoteSectionsForPlan(planId: String): List<PlanNoteSection>

    @Query("DELETE FROM plan_note_sections WHERE planId = :planId")
    suspend fun deleteNoteSectionsForPlan(planId: String)

    // --- Plan Step Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: PlanStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<PlanStep>)

    @Update
    suspend fun updateStep(step: PlanStep)

    @Delete
    suspend fun deleteStep(step: PlanStep)

    @Query("SELECT * FROM plan_steps ORDER BY sortOrder ASC, createdAtMs ASC")
    fun getAllPlanStepsFlow(): Flow<List<PlanStep>>

    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY sortOrder ASC, createdAtMs ASC")
    fun getStepsForPlanFlow(planId: String): Flow<List<PlanStep>>

    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY sortOrder ASC, createdAtMs ASC")
    suspend fun getStepsForPlan(planId: String): List<PlanStep>

    @Query("DELETE FROM plan_steps WHERE planId = :planId")
    suspend fun deleteStepsForPlan(planId: String)

    // --- Transactional Operations ---

    /**
     * Atomically completes the target occurrence and generates the single next occurrence if recurring.
     * Enforces single-pending-occurrence invariant, strict recurrence anchoring, and alert stage flag inheritance.
     */
    @Transaction
    suspend fun completeOccurrenceTransaction(
        occurrenceId: String,
        completedAtMs: Long = System.currentTimeMillis()
    ): ReminderOccurrence? {
        // 1. Resolve occurrence by ID or fallback template ID
        var occurrence = getOccurrenceById(occurrenceId)
        if (occurrence == null) {
            val pendingList = getOccurrencesForTemplate(occurrenceId).filter { it.isPending() }
            if (pendingList.isNotEmpty()) {
                occurrence = pendingList.first()
            }
        }

        if (occurrence == null || !occurrence.isPending()) {
            // Idempotent guard: already completed, skipped, or missing
            return null
        }

        // 2. Mark target occurrence as COMPLETED
        val completedOcc = occurrence.copy(
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = completedAtMs
        )
        updateOccurrence(completedOcc)

        // 3. Resolve parent template
        val template = if (occurrence.templateId != null) {
            getReminderById(occurrence.templateId)
        } else {
            getReminderById(occurrence.id)
        } ?: return null

        // 4. Handle Non-Recurring vs Recurring
        if (!template.isRecurring()) {
            update(template.copy(isActive = false))
            return null
        }

        // 5. Single Pending Occurrence Invariant Guard
        val existingPending = getOccurrencesForTemplate(template.id).filter { it.isPending() }
        if (existingPending.isNotEmpty()) {
            return existingPending.first()
        }

        // 6. Calculate single next future occurrence date using RecurrenceEngine 2.0
        val nextDueDateStr = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            rule = template.toRecurrenceRule(),
            afterDateStr = occurrence.date
        ) ?: return null

        // 7. Reset checklist from master template definition
        val resetChecklist = resetChecklistFromTemplate(template.subTasksRaw)

        // 8. Create and insert exactly ONE new PENDING occurrence inheriting template's alert configuration and planId
        val nextOccurrence = ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            templateId = template.id,
            title = template.title,
            priority = template.priority,
            date = nextDueDateStr,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING,
            completedAtMs = null,
            subTasksRaw = resetChecklist,
            snoozedUntilMs = null,
            notify7Days = template.notify7Days,
            notify24Hours = template.notify24Hours,
            notifyDueNow = template.notifyDueNow,
            planId = template.planId
        )

        insertOccurrence(nextOccurrence)
        return nextOccurrence
    }

    /**
     * Restores a completed occurrence to PENDING if doing so does not violate single-pending invariant.
     */
    @Transaction
    suspend fun restoreOccurrenceTransaction(targetId: String): ReminderOccurrence? {
        val occurrence = getOccurrenceById(targetId)
        if (occurrence != null) {
            if (occurrence.templateId != null) {
                val template = getReminderById(occurrence.templateId)
                if (template != null) {
                    val existingPending = getOccurrencesForTemplate(template.id).filter { it.isPending() }
                    if (existingPending.isNotEmpty()) {
                        return existingPending.first()
                    }
                    update(template.copy(isActive = true))
                }
            }
            val restored = occurrence.copy(
                status = ReminderOccurrence.STATUS_PENDING,
                completedAtMs = null
            )
            updateOccurrence(restored)
            return restored
        }

        val template = getReminderById(targetId)
        if (template != null) {
            update(template.copy(isActive = true))
            val pendingList = getOccurrencesForTemplate(template.id).filter { it.isPending() }
            if (pendingList.isNotEmpty()) {
                return pendingList.first()
            }
            val newOcc = ReminderOccurrence(
                id = UUID.randomUUID().toString(),
                templateId = template.id,
                title = template.title,
                priority = template.priority,
                date = template.date,
                time = template.time,
                status = ReminderOccurrence.STATUS_PENDING,
                subTasksRaw = resetChecklistFromTemplate(template.subTasksRaw),
                notify7Days = template.notify7Days,
                notify24Hours = template.notify24Hours,
                notifyDueNow = template.notifyDueNow,
                planId = template.planId
            )
            insertOccurrence(newOcc)
            return newOcc
        }

        return null
    }

    /**
     * Atomically assigns a task (Reminder Template) to a Plan and updates pending occurrences.
     * Preserves completed historical occurrences (they retain their old planId).
     */
    @Transaction
    suspend fun assignTaskToPlanTransaction(taskId: String, newPlanId: String?) {
        val template = getReminderById(taskId) ?: return
        update(template.copy(planId = newPlanId))

        val pendingOccurrences = getOccurrencesForTemplate(taskId).filter { it.isPending() }
        pendingOccurrences.forEach { occ ->
            updateOccurrence(occ.copy(planId = newPlanId))
        }
    }

    /**
     * Atomically detaches a task from any Plan and nullifies pending occurrence associations.
     */
    @Transaction
    suspend fun detachTaskFromPlanTransaction(taskId: String) {
        assignTaskToPlanTransaction(taskId, null)
    }

    /**
     * Atomically deletes a template, its pending occurrences, and preserves completed history.
     */
    @Transaction
    suspend fun deleteTemplateTransaction(templateId: String) {
        val template = getReminderById(templateId) ?: return
        deletePendingOccurrencesForTemplate(templateId)
        deleteReminder(template)
    }

    /**
     * Atomically deletes a Plan and detaches tasks (Room ON DELETE SET NULL also covers this).
     */
    @Transaction
    suspend fun deletePlanTransaction(planId: String) {
        val plan = getPlanById(planId) ?: return
        deletePlanEntity(plan)
    }

    /**
     * Atomically updates a Reminder template and reconciles its active PENDING occurrence in-place.
     * Guarantees atomic database mutation without orphaned occurrences or stale schedules.
     */
    @Transaction
    suspend fun updateReminderTransaction(reminder: Reminder): ReminderOccurrence? {
        // 1. Assign task to plan and update master template in Room
        assignTaskToPlanTransaction(reminder.id, reminder.planId)
        update(reminder)

        // 2. Query active pending occurrences for template
        val pendingList = getOccurrencesForTemplate(reminder.id).filter { it.isPending() }
        if (pendingList.isEmpty()) {
            if (!reminder.isActive) return null

            // If no pending occurrence exists and task is active, create initial
            val newOcc = ReminderOccurrence(
                id = UUID.randomUUID().toString(),
                templateId = reminder.id,
                title = reminder.title,
                priority = reminder.priority,
                date = reminder.date,
                time = reminder.time,
                status = ReminderOccurrence.STATUS_PENDING,
                subTasksRaw = resetChecklistFromTemplate(reminder.subTasksRaw),
                notify7Days = reminder.notify7Days,
                notify24Hours = reminder.notify24Hours,
                notifyDueNow = reminder.notifyDueNow,
                planId = reminder.planId
            )
            insertOccurrence(newOcc)
            return newOcc
        }

        val existingPending = pendingList.first()

        // 3. Reconcile pending occurrence date using RecurrenceEngine 2.0
        val newDate = if (reminder.isRecurring()) {
            RecurrenceEngine.calculateFirstDueDate(
                baseDateStr = reminder.date,
                rule = reminder.toRecurrenceRule(),
                searchFromDateStr = LocalDate.now().toString()
            ) ?: reminder.date
        } else {
            reminder.date
        }

        // 4. Reconcile pending occurrence in-place (preserving occurrence ID, clearing stale snooze)
        val updatedPending = existingPending.copy(
            title = reminder.title,
            priority = reminder.priority,
            date = newDate,
            time = reminder.time,
            snoozedUntilMs = null, // Clears stale snooze state on series edit
            notify7Days = reminder.notify7Days,
            notify24Hours = reminder.notify24Hours,
            notifyDueNow = reminder.notifyDueNow,
            planId = reminder.planId
        )
        updateOccurrence(updatedPending)
        return updatedPending
    }
}

/**
 * Resets checklist checkmarks from master template raw string.
 */
private fun resetChecklistFromTemplate(templateSubTasksRaw: String): String {
    if (templateSubTasksRaw.isBlank()) return ""
    return templateSubTasksRaw.split("|")
        .filter { it.isNotBlank() }
        .joinToString("|") { rawItem ->
            val trimmed = rawItem.trim()
            val cleanText = when {
                trimmed.startsWith("[x] ") -> trimmed.substring(4)
                trimmed.startsWith("[ ] ") -> trimmed.substring(4)
                else -> trimmed
            }
            "[ ] $cleanText"
        }
}
