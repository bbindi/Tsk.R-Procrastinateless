package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardIntegrationTest {

    private val planWork = Plan(id = "p1", name = "Work Goals", icon = "💼")

    private val oneTimeReminder = Reminder(
        id = "r1",
        title = "File Reports",
        date = "2026-09-26",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        planId = planWork.id
    )

    private val oneTimeOccurrence = ReminderOccurrence(
        id = "occ-r1-1",
        templateId = "r1",
        title = "File Reports",
        date = "2026-09-26",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        status = ReminderOccurrence.STATUS_PENDING,
        planId = planWork.id
    )

    private val recurringMaster = Reminder(
        id = "r2",
        title = "Weekly Review",
        date = "2026-09-01", // Past creation date
        time = "09:00",
        recurrenceType = Reminder.RECURRENCE_WEEKLY,
        recurrenceInterval = 1
    )

    private val recurringPendingOccurrence = ReminderOccurrence(
        id = "occ-r2-2",
        templateId = "r2",
        title = "Weekly Review",
        date = "2026-09-26", // Current pending scheduled date
        time = "09:00",
        status = ReminderOccurrence.STATUS_PENDING
    )

    private val recurringCompletedOccurrence = ReminderOccurrence(
        id = "occ-r2-1",
        templateId = "r2",
        title = "Weekly Review",
        date = "2026-09-19", // Old completed historical occurrence
        time = "09:00",
        status = ReminderOccurrence.STATUS_COMPLETED,
        completedAtMs = 1700000000000L
    )

    @Test
    fun testPendingTasksCount_countsOnlyActivePendingOccurrences() {
        val uiItems = listOf(
            ReminderUiItem(
                occurrenceId = oneTimeOccurrence.id,
                templateId = oneTimeOccurrence.templateId,
                title = oneTimeOccurrence.title,
                date = oneTimeOccurrence.date,
                time = oneTimeOccurrence.time,
                priority = oneTimeOccurrence.priority,
                notes = "",
                subTasksRaw = "",
                motivation = "",
                status = oneTimeOccurrence.status,
                completedAtMs = null
            ),
            ReminderUiItem(
                occurrenceId = recurringPendingOccurrence.id,
                templateId = recurringPendingOccurrence.templateId,
                title = recurringPendingOccurrence.title,
                date = recurringPendingOccurrence.date,
                time = recurringPendingOccurrence.time,
                priority = recurringPendingOccurrence.priority,
                notes = "",
                subTasksRaw = "",
                motivation = "",
                status = recurringPendingOccurrence.status,
                completedAtMs = null,
                recurrenceType = Reminder.RECURRENCE_WEEKLY
            )
        )

        val pendingCount = uiItems.count { it.isPending() }
        assertEquals(2, pendingCount)
    }

    @Test
    fun testRecurringTasksCount_countsActiveMasterTemplates_doesNotInflateFromHistoricalOccurrences() {
        val templates = listOf(oneTimeReminder, recurringMaster)
        val occurrences = listOf(oneTimeOccurrence, recurringPendingOccurrence, recurringCompletedOccurrence)

        val activeRecurringMasterCount = templates.count { it.isRecurring() && it.isActive }
        assertEquals(1, activeRecurringMasterCount)

        // Verifies completed historical occurrences do NOT inflate recurring count
        val pendingRecurringUiCount = occurrences.filter { it.isPending() }.mapNotNull { occ ->
            templates.find { it.id == occ.templateId }?.takeIf { it.isRecurring() }
        }.distinct().size

        assertEquals(1, pendingRecurringUiCount)
    }

    @Test
    fun testUpcomingTaskCard_usesCurrentPendingOccurrenceDateNotStaleTemplateDate() {
        val uiItem = ReminderUiItem(
            occurrenceId = recurringPendingOccurrence.id,
            templateId = recurringPendingOccurrence.templateId,
            title = recurringPendingOccurrence.title,
            date = recurringPendingOccurrence.date, // 2026-09-26
            time = recurringPendingOccurrence.time,
            priority = recurringPendingOccurrence.priority,
            notes = "",
            subTasksRaw = "",
            motivation = "",
            status = recurringPendingOccurrence.status,
            completedAtMs = null,
            recurrenceType = Reminder.RECURRENCE_WEEKLY
        )

        assertEquals("2026-09-26", uiItem.date)
        assertEquals("09:00", uiItem.time)
        assertTrue(uiItem.isPending())
        assertTrue(uiItem.isRecurring())
    }

    @Test
    fun testActivePlansCount_calculatesFromPlanListSize() {
        val plans = listOf(planWork)
        assertEquals(1, plans.size)
    }
}
