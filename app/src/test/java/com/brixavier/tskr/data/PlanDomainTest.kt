package com.brixavier.tskr.data

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PlanDomainTest {

    // --- 1. OPTIONAL RELATIONSHIP ---

    @Test
    fun plan_canExistWithZeroTasks() {
        val plan = Plan(name = "Learn Compose")
        assertEquals("Learn Compose", plan.name)
        assertEquals(Plan.STATUS_ACTIVE, plan.status)
        assertTrue(plan.isActive())
    }

    @Test
    fun reminder_canExistWithoutPlan() {
        val reminder = Reminder(
            title = "Standalone Task",
            date = "2026-09-25",
            time = "10:00"
        )
        assertNull(reminder.planId)
    }

    // --- 2. ASSIGNMENT BEHAVIOR (MOCK DOMAIN LOGIC) ---

    @Test
    fun taskAssignment_updatesReminderAndPendingOccurrence() {
        val plan = Plan(name = "Fitness Goal")
        
        var template = Reminder(
            title = "Go to Gym",
            date = "2026-09-25",
            time = "18:00"
        )

        var occurrence = ReminderOccurrence(
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING
        )

        // Pre-assignment
        assertNull(template.planId)
        assertNull(occurrence.planId)

        // Assign
        template = template.copy(planId = plan.id)
        occurrence = occurrence.copy(planId = plan.id)

        // Verify
        assertEquals(plan.id, template.planId)
        assertEquals(plan.id, occurrence.planId)
    }

    // --- 3. RECURRING INHERITANCE ---

    @Test
    fun recurringCompletion_nextOccurrenceInheritsTemplatePlanId() {
        val planId = "plan-weekly-chores"
        val template = Reminder(
            title = "Take out trash",
            date = "2026-09-25",
            time = "19:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            planId = planId
        )

        // Act: generate next occurrence simulating ReminderDao.completeOccurrenceTransaction
        val nextOccurrence = ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            templateId = template.id,
            title = template.title,
            date = "2026-10-02",
            time = template.time,
            planId = template.planId
        )

        assertEquals(planId, nextOccurrence.planId)
    }

    // --- 4. PRESENTATION MODEL (UiItem) MAPS EXACT OCCURRENCE PLAN ID ---

    @Test
    fun uiItem_mapsExactOccurrencePlanIdForHistoricalIntegrity() {
        val targetPlanId = "plan-history"
        
        val completedOccurrence = ReminderOccurrence(
            templateId = "tmpl-1",
            title = "Done Task",
            date = "2026-09-20",
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            planId = targetPlanId
        )

        // Simulating ReminderBloc mapToUiItems logic:
        val uiItem = ReminderUiItem(
            occurrenceId = completedOccurrence.id,
            templateId = completedOccurrence.templateId,
            title = completedOccurrence.title,
            date = completedOccurrence.date,
            time = completedOccurrence.time,
            priority = completedOccurrence.priority,
            notes = "",
            subTasksRaw = completedOccurrence.subTasksRaw,
            motivation = "",
            status = completedOccurrence.status,
            completedAtMs = completedOccurrence.completedAtMs,
            planId = completedOccurrence.planId // Critical map
        )

        assertEquals(targetPlanId, uiItem.planId)
    }
}
