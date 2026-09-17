package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLibraryFilterTest {

    private val nowStr = "2026-09-25 12:00"

    private val activeOverdueNormal = Reminder(
        id = "r-overdue-normal",
        title = "Pay Bill",
        date = "2026-09-24", // Past date (overdue)
        time = "10:00",
        priority = Reminder.PRIORITY_NORMAL,
        isActive = true
    )

    private val activeOverdueImportant = Reminder(
        id = "r-overdue-important",
        title = "Urgent Taxes",
        date = "2026-09-24", // Past date (overdue)
        time = "09:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        isActive = true
    )

    private val activeUpcomingNormal = Reminder(
        id = "r-upcoming-normal",
        title = "Buy Groceries",
        date = "2026-09-26",
        time = "14:00",
        priority = Reminder.PRIORITY_NORMAL,
        isActive = true
    )

    private val activeUpcomingImportant = Reminder(
        id = "r-upcoming-important",
        title = "Doctor Appointment",
        date = "2026-09-26",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        isActive = true
    )

    private val activeRecurring = Reminder(
        id = "r-recurring-water",
        title = "Water Plants",
        date = "2026-09-20",
        time = "08:00",
        recurrenceType = Reminder.RECURRENCE_DAILY,
        recurrenceInterval = 1,
        isActive = true
    )

    private val completedOneTime = Reminder(
        id = "r-completed-1",
        title = "Clean Garage",
        date = "2026-09-15",
        time = "12:00",
        isActive = false
    )

    @Test
    fun testTaskFilterValues_includesRecurring() {
        val filterValues = TaskFilter.entries.map { it.name }
        assertTrue(filterValues.contains("RECURRING"))
        assertEquals(listOf("ALL", "ACTIVE", "RECURRING", "COMPLETED", "OVERDUE"), TaskFilter.entries.map { it.name })
    }

    @Test
    fun testAllFilterSorting_groupsActiveOverdueThenActiveUpcomingThenCompleted() {
        val displayReminders = listOf(
            completedOneTime,
            activeUpcomingNormal,
            activeOverdueNormal,
            activeOverdueImportant,
            activeUpcomingImportant
        )

        // Comparator logic as implemented in TasksPage
        val sortedList = displayReminders.sortedWith { r1, r2 ->
            val group1 = if (!r1.isActive) 2 else if ("${r1.date} ${r1.time}" < nowStr) 0 else 1
            val group2 = if (!r2.isActive) 2 else if ("${r2.date} ${r2.time}" < nowStr) 0 else 1

            if (group1 != group2) {
                return@sortedWith group1.compareTo(group2)
            }

            if (group1 == 2) {
                0
            } else {
                val imp1 = if (r1.isImportant()) 0 else 1
                val imp2 = if (r2.isImportant()) 0 else 1
                if (imp1 != imp2) {
                    return@sortedWith imp1.compareTo(imp2)
                }
                "${r1.date} ${r1.time}".compareTo("${r2.date} ${r2.time}")
            }
        }

        // Group 0: Active Overdue Important first ("Urgent Taxes")
        assertEquals("r-overdue-important", sortedList[0].id)
        // Group 0: Active Overdue Normal second ("Pay Bill")
        assertEquals("r-overdue-normal", sortedList[1].id)
        // Group 1: Active Upcoming Important ("Doctor Appointment")
        assertEquals("r-upcoming-important", sortedList[2].id)
        // Group 1: Active Upcoming Normal ("Buy Groceries")
        assertEquals("r-upcoming-normal", sortedList[3].id)
        // Group 2: Completed task placed at bottom ("Clean Garage")
        assertEquals("r-completed-1", sortedList[4].id)
    }

    @Test
    fun testRecurringFilter_includesOnlyActiveRecurringTemplates() {
        val displayReminders = listOf(
            Reminder(id = "r1", title = "Submit Taxes", date = "2026-09-26", time = "10:00", isActive = true),
            activeRecurring,
            completedOneTime
        )

        val recurringList = displayReminders.filter { it.isActive && it.isRecurring() }

        assertEquals(1, recurringList.size)
        assertEquals("r-recurring-water", recurringList.first().id)
        assertTrue(recurringList.first().isRecurring())
    }

    @Test
    fun testSearchWithRecurringFilter_returnsMatchingRecurringTasks() {
        val displayReminders = listOf(
            activeRecurring, // "Water Plants"
            Reminder(id = "r-rec-2", title = "Gym Exercise", date = "2026-09-26", time = "10:00", recurrenceType = Reminder.RECURRENCE_DAILY, isActive = true)
        )

        val query = "water"
        val filtered = displayReminders
            .filter { it.isActive && it.isRecurring() }
            .filter { it.title.lowercase().contains(query) }

        assertEquals(1, filtered.size)
        assertEquals("r-recurring-water", filtered.first().id)
    }

    @Test
    fun testOverdueFilter_excludesFutureAndCompletedTasks() {
        val displayReminders = listOf(
            activeOverdueNormal,
            activeUpcomingNormal,
            completedOneTime
        )

        val overdueList = displayReminders.filter { it.isActive && "${it.date} ${it.time}" < nowStr }

        assertEquals(1, overdueList.size)
        assertEquals("r-overdue-normal", overdueList.first().id)
    }
}
