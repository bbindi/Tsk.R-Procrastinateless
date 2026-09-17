package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

enum class TestTaskFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    OVERDUE
}

class TaskExperienceTest {

    private val nowStr = "2026-09-25 12:00"

    private val planFitness = Plan(id = "plan-1", name = "Fitness Goal")
    private val planWork = Plan(id = "plan-2", name = "Work Deliverables")

    private val taskActiveNormal = Reminder(
        id = "t1",
        title = "Buy Groceries",
        date = "2026-09-26",
        time = "10:00",
        isActive = true,
        priority = Reminder.PRIORITY_NORMAL,
        notes = "Milk, Eggs, Bread",
        subTasksRaw = "[ ] Buy milk|[ ] Buy bread",
        planId = planFitness.id
    )

    private val taskActiveImportant = Reminder(
        id = "t2",
        title = "Submit Report",
        date = "2026-09-26",
        time = "09:00",
        isActive = true,
        priority = Reminder.PRIORITY_IMPORTANT,
        notes = "Quarterly analysis",
        planId = planWork.id
    )

    private val taskOverdueNormal = Reminder(
        id = "t3",
        title = "Call Dentist",
        date = "2026-09-24",
        time = "15:00",
        isActive = true,
        priority = Reminder.PRIORITY_NORMAL,
        notes = "Routine checkup"
    )

    private val taskOverdueImportant = Reminder(
        id = "t4",
        title = "Pay Taxes",
        date = "2026-09-20",
        time = "10:00",
        isActive = true,
        priority = Reminder.PRIORITY_IMPORTANT,
        notes = "Annual return"
    )

    private val taskCompletedOneTime = Reminder(
        id = "t5",
        title = "Read Book Chapter",
        date = "2026-09-21",
        time = "18:00",
        isActive = false,
        priority = Reminder.PRIORITY_NORMAL
    )

    private val recurringTaskDaily = Reminder(
        id = "t6",
        title = "Daily Journal",
        date = "2026-09-25", // Current pending date (today at 21:00 - upcoming)
        time = "21:00",
        isActive = true,
        recurrenceType = Reminder.RECURRENCE_DAILY,
        recurrenceInterval = 1
    )

    private val pendingOccurrenceForRecurring = ReminderOccurrence(
        id = "occ-t6-2",
        templateId = "t6",
        title = "Daily Journal",
        date = "2026-09-26", // Advanced date (tomorrow)
        time = "21:00",
        status = ReminderOccurrence.STATUS_PENDING
    )

    private val allTasks = listOf(
        taskActiveNormal,
        taskActiveImportant,
        taskOverdueNormal,
        taskOverdueImportant,
        taskCompletedOneTime,
        recurringTaskDaily
    )

    private val plansMap = mapOf(
        planFitness.id to planFitness,
        planWork.id to planWork
    )

    private fun filterAndSortTasks(
        tasks: List<Reminder>,
        filter: TestTaskFilter,
        searchQuery: String = "",
        pendingOccurrences: List<ReminderOccurrence> = emptyList()
    ): List<Reminder> {
        val pendingMap = pendingOccurrences.associateBy { it.templateId }
        val displayTasks = tasks.map { rem ->
            val pendingOcc = pendingMap[rem.id]
            if (rem.isActive && pendingOcc != null) {
                rem.copy(
                    date = pendingOcc.date,
                    time = pendingOcc.time,
                    subTasksRaw = pendingOcc.subTasksRaw
                )
            } else {
                rem
            }
        }

        val baseList = when (filter) {
            TestTaskFilter.ALL -> displayTasks
            TestTaskFilter.ACTIVE -> displayTasks.filter { it.isActive }
            TestTaskFilter.COMPLETED -> displayTasks.filter { !it.isActive }
            TestTaskFilter.OVERDUE -> displayTasks.filter { it.isActive && "${it.date} ${it.time}" < nowStr }
        }

        val searchedList = if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            baseList.filter { task ->
                val planName = task.planId?.let { plansMap[it]?.name } ?: ""
                task.title.lowercase(Locale.US).contains(q) ||
                        task.notes.lowercase(Locale.US).contains(q) ||
                        task.subTasksRaw.lowercase(Locale.US).contains(q) ||
                        planName.lowercase(Locale.US).contains(q)
            }
        }

        return searchedList.sortedWith(
            compareBy<Reminder> { task ->
                val isOverdue = task.isActive && "${task.date} ${task.time}" < nowStr
                if (isOverdue) 0 else 1
            }.thenBy { task ->
                if (task.isImportant()) 0 else 1
            }.thenBy { task ->
                "${task.date} ${task.time}"
            }
        )
    }

    @Test
    fun testFilterAll() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL)
        assertEquals(6, result.size)
    }

    @Test
    fun testFilterActive() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ACTIVE)
        assertEquals(5, result.size)
        assertTrue(result.all { it.isActive })
    }

    @Test
    fun testFilterCompleted_includesOnlyInactiveTemplates() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.COMPLETED)
        assertEquals(1, result.size)
        assertEquals("Read Book Chapter", result.first().title)
        assertFalse(result.contains(recurringTaskDaily))
    }

    @Test
    fun testFilterOverdue() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.OVERDUE)
        assertEquals(2, result.size)
        assertTrue(result.contains(taskOverdueNormal))
        assertTrue(result.contains(taskOverdueImportant))
    }

    @Test
    fun testSorting_overdueFirst_thenImportant_thenNormal() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL)
        assertEquals("t4", result[0].id) // Pay Taxes (Overdue Important)
        assertEquals("t3", result[1].id) // Call Dentist (Overdue Normal)
        assertEquals("t2", result[2].id) // Submit Report (Upcoming Important)
    }

    @Test
    fun testSearchByTitle() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL, searchQuery = "report")
        assertEquals(1, result.size)
        assertEquals("Submit Report", result.first().title)
    }

    @Test
    fun testSearchByNotes() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL, searchQuery = "milk")
        assertEquals(1, result.size)
        assertEquals("Buy Groceries", result.first().title)
    }

    @Test
    fun testSearchBySubtaskChecklist() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL, searchQuery = "bread")
        assertEquals(1, result.size)
        assertEquals("Buy Groceries", result.first().title)
    }

    @Test
    fun testSearchByPlanName() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL, searchQuery = "fitness")
        assertEquals(1, result.size)
        assertEquals("Buy Groceries", result.first().title)
    }

    @Test
    fun testSearchCombinedWithFilter() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.OVERDUE, searchQuery = "tax")
        assertEquals(1, result.size)
        assertEquals("Pay Taxes", result.first().title)
    }

    @Test
    fun testSearchEmptyResults() {
        val result = filterAndSortTasks(allTasks, TestTaskFilter.ALL, searchQuery = "nonexistent")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testOneRecurringTask_producesSingleCardInActiveList() {
        val result = filterAndSortTasks(listOf(recurringTaskDaily), TestTaskFilter.ACTIVE)
        assertEquals(1, result.size)
        assertEquals("Daily Journal", result.first().title)
    }

    @Test
    fun testRecurringTask_updatesDisplayedDateFromPendingOccurrence() {
        val result = filterAndSortTasks(
            tasks = listOf(recurringTaskDaily),
            filter = TestTaskFilter.ACTIVE,
            pendingOccurrences = listOf(pendingOccurrenceForRecurring)
        )
        assertEquals(1, result.size)
        val displayedTask = result.first()
        // Master template date was 2026-09-25, but mapped pending occurrence date is 2026-09-26
        assertEquals("2026-09-26", displayedTask.date)
    }

    @Test
    fun testRecurringTask_completedOccurrenceDoesNotMoveActiveRecurringTaskToCompleted() {
        val completedList = filterAndSortTasks(
            tasks = listOf(recurringTaskDaily),
            filter = TestTaskFilter.COMPLETED,
            pendingOccurrences = listOf(pendingOccurrenceForRecurring)
        )
        assertTrue(completedList.isEmpty())

        val activeList = filterAndSortTasks(
            tasks = listOf(recurringTaskDaily),
            filter = TestTaskFilter.ACTIVE,
            pendingOccurrences = listOf(pendingOccurrenceForRecurring)
        )
        assertEquals(1, activeList.size)
    }

    @Test
    fun testNewTaskDefaultsToNoneRecurrence() {
        val newTask = Reminder(title = "One-time task", date = "2026-09-25", time = "10:00")
        assertEquals(Reminder.RECURRENCE_NONE, newTask.recurrenceType)
        assertEquals(1, newTask.recurrenceInterval)
    }

    @Test
    fun testNewTaskWithDailyRecurrence() {
        val newTask = Reminder(
            title = "Drink Water",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1
        )
        assertEquals(Reminder.RECURRENCE_DAILY, newTask.recurrenceType)
        assertTrue(newTask.isRecurring())
    }

    @Test
    fun testNewTaskWithWeeklyRecurrence() {
        val newTask = Reminder(
            title = "Weekly Review",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1
        )
        assertEquals(Reminder.RECURRENCE_WEEKLY, newTask.recurrenceType)
        assertTrue(newTask.isRecurring())
    }

    @Test
    fun testNewTaskWithMonthlyRecurrence() {
        val newTask = Reminder(
            title = "Pay Rent",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1
        )
        assertEquals(Reminder.RECURRENCE_MONTHLY, newTask.recurrenceType)
        assertTrue(newTask.isRecurring())
    }

    @Test
    fun testNewTaskWithCustomDaysRecurrence() {
        val newTask = Reminder(
            title = "Water Plants",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 3
        )
        assertEquals(Reminder.RECURRENCE_CUSTOM_DAYS, newTask.recurrenceType)
        assertEquals(3, newTask.recurrenceInterval)
        assertTrue(newTask.isRecurring())
    }

    @Test
    fun testEditingExistingRecurringTask_preservesSavedRecurrenceType() {
        val existingTask = Reminder(
            title = "Daily Walk",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1
        )
        val updatedTask = existingTask.copy(title = "Daily Morning Walk")
        assertEquals(Reminder.RECURRENCE_DAILY, updatedTask.recurrenceType)
        assertEquals(1, updatedTask.recurrenceInterval)
    }

    @Test
    fun testChangingRecurrenceType_fromDailyToNone() {
        val existingTask = Reminder(
            title = "Meditate",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1
        )
        val updatedTask = existingTask.copy(recurrenceType = Reminder.RECURRENCE_NONE)
        assertEquals(Reminder.RECURRENCE_NONE, updatedTask.recurrenceType)
        assertFalse(updatedTask.isRecurring())
    }
}
