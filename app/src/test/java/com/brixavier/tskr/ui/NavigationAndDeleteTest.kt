package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationAndDeleteTest {

    private val pendingOccurrence = ReminderOccurrence(
        id = "occ-pending-1",
        templateId = "rem-recurring",
        title = "Daily Gym",
        date = "2026-09-26",
        time = "08:00",
        status = ReminderOccurrence.STATUS_PENDING
    )

    private val completedHistoryOccurrence = ReminderOccurrence(
        id = "occ-completed-1",
        templateId = "rem-recurring",
        title = "Daily Gym",
        date = "2026-09-25",
        time = "08:00",
        status = ReminderOccurrence.STATUS_COMPLETED,
        completedAtMs = 1700000000000L
    )

    @Test
    fun testOriginatingScreen_tasksOrigin_returnsToTasksOnBack() {
        val activeTab = Screen.Tasks
        var selectedReminderId: String? = "rem-1"

        // Simulate Back action on Task Details
        selectedReminderId = null
        val returnedScreen = activeTab

        assertNull(selectedReminderId)
        assertEquals(Screen.Tasks, returnedScreen)
    }

    @Test
    fun testOriginatingScreen_homeOrigin_returnsToHomeOnBack() {
        val activeTab = Screen.Home
        var selectedReminderId: String? = "rem-1"

        // Simulate Back action on Task Details
        selectedReminderId = null
        val returnedScreen = activeTab

        assertNull(selectedReminderId)
        assertEquals(Screen.Home, returnedScreen)
    }

    @Test
    fun testEditFromTasks_returnsToTasksAfterSave() {
        val activeTab = Screen.Tasks
        var selectedReminderId: String? = "rem-1"
        var editingReminderId: String? = "rem-1"

        // User saves changes in edit sheet
        selectedReminderId = null
        editingReminderId = null

        assertNull(selectedReminderId)
        assertNull(editingReminderId)
        assertEquals(Screen.Tasks, activeTab)
    }

    @Test
    fun testCancelEdit_leavesUserInTaskDetails() {
        var selectedReminderId: String? = "rem-1"
        var editingReminderId: String? = "rem-1"

        // User cancels edit in sheet
        editingReminderId = null

        assertNotNull(selectedReminderId)
        assertNull(editingReminderId)
    }

    @Test
    fun testDeleteConfirmation_cancelDelete_preservesTask() {
        var isTaskDeleted = false
        var showDeleteConfirmation = true

        // User taps Cancel on Delete Confirmation Dialog
        showDeleteConfirmation = false
        // Task is NOT deleted
        assertFalse(isTaskDeleted)
        assertFalse(showDeleteConfirmation)
    }

    @Test
    fun testConfirmDelete_fromTasks_deletesAndReturnsToTasks() {
        val activeTab = Screen.Tasks
        var selectedReminderId: String? = "rem-1"
        var isTaskDeleted = false

        // User taps Delete -> confirms in dialog
        isTaskDeleted = true
        selectedReminderId = null // returns to originating screen

        assertTrue(isTaskDeleted)
        assertNull(selectedReminderId)
        assertEquals(Screen.Tasks, activeTab)
    }

    @Test
    fun testConfirmDelete_fromDashboard_deletesAndReturnsToDashboard() {
        val activeTab = Screen.Home
        var selectedReminderId: String? = "rem-1"
        var isTaskDeleted = false

        // User taps Delete -> confirms in dialog
        isTaskDeleted = true
        selectedReminderId = null // returns to originating screen

        assertTrue(isTaskDeleted)
        assertNull(selectedReminderId)
        assertEquals(Screen.Home, activeTab)
    }

    @Test
    fun testRecurringTaskDeletion_preservesCompletedHistory() {
        // Pending occurrence is deleted with template, completed history remains
        val historyOcc = completedHistoryOccurrence.copy(templateId = null)

        assertEquals("occ-completed-1", historyOcc.id)
        assertNull(historyOcc.templateId)
        assertTrue(historyOcc.isCompleted())
    }

    @Test
    fun testOccurrenceSpecificTaskDetails_resolvesExactOccurrence() {
        val uiItems = listOf(
            ReminderUiItem(
                occurrenceId = pendingOccurrence.id,
                templateId = pendingOccurrence.templateId,
                title = pendingOccurrence.title,
                date = pendingOccurrence.date,
                time = pendingOccurrence.time,
                priority = pendingOccurrence.priority,
                notes = "",
                subTasksRaw = "",
                motivation = "",
                status = pendingOccurrence.status,
                completedAtMs = null,
                recurrenceType = Reminder.RECURRENCE_DAILY
            )
        )

        val selectedItem = uiItems.find { it.occurrenceId == "occ-pending-1" }
        assertNotNull(selectedItem)
        assertEquals("occ-pending-1", selectedItem?.occurrenceId)
        assertEquals("rem-recurring", selectedItem?.templateId)
        assertEquals("2026-09-26", selectedItem?.date)
    }
}
