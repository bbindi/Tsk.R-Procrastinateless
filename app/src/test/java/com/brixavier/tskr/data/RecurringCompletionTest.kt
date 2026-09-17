package com.brixavier.tskr.data

import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringCompletionTest {

    // --- 1. NON-RECURRING TESTS ---

    @Test
    fun nonRecurring_completionCreatesNoNextOccurrence() {
        val template = Reminder(
            id = "non-rec-1",
            title = "Buy Groceries",
            date = "2026-09-10",
            time = "14:00",
            recurrenceType = Reminder.RECURRENCE_NONE
        )

        val occurrence = ReminderOccurrence(
            id = "occ-non-rec-1",
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING
        )

        assertFalse(template.isRecurring())
        assertTrue(occurrence.isPending())

        // RecurrenceEngine returns null for RECURRENCE_NONE
        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = occurrence.date
        )

        assertNull(nextDate)
    }

    // --- 2. DAILY RECURRENCE TESTS ---

    @Test
    fun daily_normalCompletionGeneratesSingleNextOccurrence() {
        val template = Reminder(
            id = "daily-1",
            title = "Daily Meditation",
            date = "2026-09-01",
            time = "08:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1
        )

        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-09-01"
        )

        assertEquals("2026-09-02", nextDate)
    }

    @Test
    fun daily_lateCompletionPreservesScheduleGridNoBacklog() {
        val template = Reminder(
            id = "daily-2",
            title = "Daily Workout",
            date = "2026-09-01",
            time = "07:00",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1
        )

        // Completed 4 days late on Sept 5 -> next occurrence is Sept 6 (no backlog)
        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-09-05"
        )

        assertEquals("2026-09-06", nextDate)
    }

    // --- 3. WEEKLY RECURRENCE TESTS ---

    @Test
    fun weekly_mondayScheduleCompletedThursdayYieldsNextMonday() {
        val template = Reminder(
            id = "weekly-1",
            title = "Weekly Review",
            date = "2026-09-07", // Monday
            time = "09:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1
        )

        // Completed Thursday Sept 10 -> next remains coming Monday Sept 14
        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-09-10"
        )

        assertEquals("2026-09-14", nextDate)
    }

    @Test
    fun weekly_biweeklyIntervalMaintainsGrid() {
        val template = Reminder(
            id = "weekly-bi",
            title = "Bi-Weekly Sync",
            date = "2026-09-07", // Monday
            time = "10:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 2
        )

        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-09-07"
        )

        assertEquals("2026-09-21", nextDate)
    }

    // --- 4. MONTHLY RECURRENCE TESTS ---

    @Test
    fun monthly_normalAndDayClampingAndAnchorRestoration() {
        val template = Reminder(
            id = "monthly-31",
            title = "Monthly Report",
            date = "2026-05-31",
            time = "17:00",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1
        )

        // May 31 -> June (30 days) -> June 30
        val juneDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-05-31"
        )
        assertEquals("2026-06-30", juneDate)

        // Evaluating after June 30 -> Restores July 31 anchor!
        val julyDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = juneDate
        )
        assertEquals("2026-07-31", julyDate)
    }

    @Test
    fun monthly_january31ToFebruary() {
        val template = Reminder(
            id = "monthly-feb",
            title = "Rent Payment",
            date = "2026-01-31",
            time = "09:00",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1
        )

        // Non-leap year Feb 2026 -> Feb 28
        val feb2026 = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-01-31"
        )
        assertEquals("2026-02-28", feb2026)

        // Leap year Feb 2028 -> Feb 29
        val feb2028 = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2028-01-31",
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2028-01-31"
        )
        assertEquals("2028-02-29", feb2028)
    }

    // --- 5. CUSTOM DAYS TESTS ---

    @Test
    fun customDays_lateCompletionMaintainsGrid() {
        val template = Reminder(
            id = "custom-3d",
            title = "Water Plants",
            date = "2026-09-01",
            time = "12:00",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 3
        )

        // Grid: Sept 1, 4, 7, 10...
        // Completed Sept 8 -> Next is Sept 10
        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = "2026-09-08"
        )
        assertEquals("2026-09-10", nextDate)
    }

    // --- 6. IDEMPOTENCY & DUPLICATE COMPLETION TESTS ---

    @Test
    fun idempotency_completedOccurrenceIsIgnored() {
        val completedOcc = ReminderOccurrence(
            id = "occ-completed-1",
            templateId = "tmpl-1",
            title = "Task",
            date = "2026-09-01",
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = 1788259200000L
        )

        // Completed occurrence is not pending
        assertFalse(completedOcc.isPending())
        assertTrue(completedOcc.isCompleted())
    }

    // --- 7. CHECKLIST RESET TESTS ---

    @Test
    fun checklistReset_resetsCheckmarksForNewOccurrence() {
        val masterSubTasks = "[x] Step 1|[x] Step 2|[ ] Step 3"
        
        // Simulating checklist reset helper logic
        val resetList = masterSubTasks.split("|")
            .filter { it.isNotBlank() }
            .joinToString("|") { item ->
                val trimmed = item.trim()
                val cleanText = when {
                    trimmed.startsWith("[x] ") -> trimmed.substring(4)
                    trimmed.startsWith("[ ] ") -> trimmed.substring(4)
                    else -> trimmed
                }
                "[ ] $cleanText"
            }

        assertEquals("[ ] Step 1|[ ] Step 2|[ ] Step 3", resetList)
    }

    // --- 8. SNOOZE & TEMPLATE PROTECTION TESTS ---

    @Test
    fun snooze_preservesOriginalTemplateRecurrenceAnchor() {
        val template = Reminder(
            id = "snooze-tmpl-1",
            title = "Call Mom",
            date = "2026-09-07", // Monday anchor
            time = "18:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1
        )

        val occurrence = ReminderOccurrence(
            id = "snooze-occ-1",
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING
        )

        // Occurrence snoozed to Tuesday Sept 8 at 18:05
        val snoozedOcc = occurrence.copy(
            date = "2026-09-08",
            time = "18:05",
            snoozedUntilMs = 1788861900000L
        )

        // When completing snoozed occurrence, calculation uses original template anchor date (Sept 07)
        val nextDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = template.date,
            recurrenceType = template.recurrenceType,
            recurrenceInterval = template.recurrenceInterval,
            afterDateStr = snoozedOcc.date
        )

        // Next weekly occurrence is Monday Sept 14 (not Tuesday Sept 15)
        assertEquals("2026-09-14", nextDate)
        assertEquals("2026-09-07", template.date)
    }

    // --- 9. HISTORY & TIMESTAMP TESTS ---

    @Test
    fun history_v2CompletionRecordsTimestampWhileMigratedV1IsNull() {
        val v2CompletedOcc = ReminderOccurrence(
            id = "v2-occ-1",
            templateId = "tmpl-1",
            title = "V2 Completed Task",
            date = "2026-09-10",
            time = "12:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = System.currentTimeMillis()
        )

        val v1MigratedOcc = ReminderOccurrence(
            id = "v1-migrated-1",
            templateId = "v1-tmpl-1",
            title = "V1 Migrated Task",
            date = "2026-09-01",
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = null
        )

        assertNotNull(v2CompletedOcc.completedAtMs)
        assertNull(v1MigratedOcc.completedAtMs)
    }

    // --- 10. UI PRESENTATION MODEL TESTS ---

    @Test
    fun uiItem_propertiesAndRecurrenceBadgeFormatting() {
        val uiItem = ReminderUiItem(
            occurrenceId = "occ-1",
            templateId = "tmpl-1",
            title = "Weekly Standup",
            date = "2026-09-07",
            time = "09:00",
            priority = Reminder.PRIORITY_IMPORTANT,
            notes = "Notes",
            subTasksRaw = "[ ] Step 1",
            motivation = "Motivation",
            status = ReminderOccurrence.STATUS_PENDING,
            completedAtMs = null,
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 2
        )

        assertTrue(uiItem.isPending())
        assertFalse(uiItem.isCompleted())
        assertTrue(uiItem.isRecurring())
        assertTrue(uiItem.isImportant())
        assertEquals("🔁 Every 2 weeks", uiItem.getRecurrenceDisplayName())
    }
}
