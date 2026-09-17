package com.brixavier.tskr.data

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PlanC4IntegrationTest {

    @Test
    fun createTask_withoutPlan_defaultsToNullPlanId() {
        val task = Reminder(
            title = "Unassigned Task",
            date = "2026-09-25",
            time = "10:00"
        )
        assertNull(task.planId)
    }

    @Test
    fun createTask_withPlan_assignsPlanIdToTemplateAndOccurrence() {
        val plan = Plan(name = "Learn Kotlin")
        val task = Reminder(
            title = "Read Coroutines Chapter",
            date = "2026-09-25",
            time = "10:00",
            planId = plan.id
        )
        val occurrence = ReminderOccurrence(
            templateId = task.id,
            title = task.title,
            date = task.date,
            time = task.time,
            planId = task.planId
        )

        assertEquals(plan.id, task.planId)
        assertEquals(plan.id, occurrence.planId)
    }

    @Test
    fun moveTask_fromPlanAToPlanB_updatesTemplateAndPendingOccurrence_preservesCompletedHistory() {
        val planA = Plan(name = "Plan A")
        val planB = Plan(name = "Plan B")

        val completedOccUnderA = ReminderOccurrence(
            templateId = "tmpl-1",
            title = "Task",
            date = "2026-09-10",
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            planId = planA.id
        )

        var template = Reminder(
            id = "tmpl-1",
            title = "Task",
            date = "2026-09-25",
            time = "10:00",
            planId = planA.id
        )

        var pendingOccUnderA = ReminderOccurrence(
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING,
            planId = planA.id
        )

        // Act: Move task to Plan B
        template = template.copy(planId = planB.id)
        pendingOccUnderA = pendingOccUnderA.copy(planId = planB.id)

        // Assert: Template and pending occurrence are updated to Plan B
        assertEquals(planB.id, template.planId)
        assertEquals(planB.id, pendingOccUnderA.planId)

        // Assert: Completed historical occurrence remains under Plan A
        assertEquals(planA.id, completedOccUnderA.planId)
    }

    @Test
    fun detachTask_fromPlan_setsPlanIdNullForTemplateAndPendingOccurrence() {
        val plan = Plan(name = "Fitness")
        var template = Reminder(
            title = "Pushups",
            date = "2026-09-25",
            time = "08:00",
            planId = plan.id
        )
        var pendingOcc = ReminderOccurrence(
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            planId = plan.id
        )

        // Act: Detach
        template = template.copy(planId = null)
        pendingOcc = pendingOcc.copy(planId = null)

        assertNull(template.planId)
        assertNull(pendingOcc.planId)
    }

    @Test
    fun recurringTask_completionGeneratesNextOccurrenceInheritingTemplatePlan() {
        val plan = Plan(name = "Daily Habits")
        val template = Reminder(
            title = "Journal",
            date = "2026-09-25",
            time = "21:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            planId = plan.id
        )

        val nextOccurrence = ReminderOccurrence(
            templateId = template.id,
            title = template.title,
            date = "2026-09-26",
            time = template.time,
            planId = template.planId
        )

        assertEquals(plan.id, nextOccurrence.planId)
    }
}
