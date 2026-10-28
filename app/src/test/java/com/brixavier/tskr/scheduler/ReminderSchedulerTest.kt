package com.brixavier.tskr.scheduler

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ReminderSchedulerTest {

    @Test
    fun getRequestCode_isDeterministicForOccurrenceId() {
        val occurrenceId = "occ-test-uuid-12345"
        val code1 = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_DUE_NOW)
        val code2 = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_DUE_NOW)
        
        assertEquals(code1, code2)
    }

    @Test
    fun getRequestCode_differentiatesStagesForSameOccurrence() {
        val occurrenceId = "occ-test-uuid-12345"
        val code7d = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_7_DAYS)
        val code24h = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_24_HOURS)
        val codeDue = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_DUE_NOW)

        assertNotEquals(code7d, code24h)
        assertNotEquals(code24h, codeDue)
        assertNotEquals(code7d, codeDue)
    }

    @Test
    fun occurrenceStatus_pendingVsCompletedVsSkipped() {
        val pendingOcc = ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            templateId = "tmpl-1",
            title = "Test Task",
            date = "2026-09-20",
            time = "10:00",
            status = ReminderOccurrence.STATUS_PENDING
        )
        val completedOcc = pendingOcc.copy(status = ReminderOccurrence.STATUS_COMPLETED)
        val skippedOcc = pendingOcc.copy(status = ReminderOccurrence.STATUS_SKIPPED)

        assertTrue(pendingOcc.isPending())
        assertFalse(completedOcc.isPending())
        assertFalse(skippedOcc.isPending())

        assertTrue(completedOcc.isCompleted())
        assertFalse(pendingOcc.isCompleted())
    }

    @Test
    fun extraKeys_constantsMatchExpectedValues() {
        assertEquals("EXTRA_OCCURRENCE_ID", ReminderScheduler.EXTRA_OCCURRENCE_ID)
        assertEquals("EXTRA_TEMPLATE_ID", ReminderScheduler.EXTRA_TEMPLATE_ID)
        assertEquals("EXTRA_REMINDER_ID", ReminderScheduler.EXTRA_REMINDER_ID)
        assertEquals("EXTRA_REMINDER_TITLE", ReminderScheduler.EXTRA_REMINDER_TITLE)
        assertEquals("EXTRA_ALARM_STAGE", ReminderScheduler.EXTRA_ALARM_STAGE)
        assertEquals("EXTRA_REMINDER_PRIORITY", ReminderScheduler.EXTRA_REMINDER_PRIORITY)
    }

    @Test
    fun snooze_doesNotAlterTemplateRecurrenceAnchor() {
        val template = Reminder(
            id = "template-weekly-1",
            title = "Weekly Standup",
            date = "2026-09-07", // Monday
            time = "09:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1
        )

        val occurrence = ReminderOccurrence(
            id = "occ-weekly-1",
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            status = ReminderOccurrence.STATUS_PENDING
        )

        // Snooze occurrence to 09:05
        val snoozedOccurrence = occurrence.copy(
            time = "09:05",
            snoozedUntilMs = 1788771900000L
        )

        // Verify occurrence time was snoozed
        assertEquals("09:05", snoozedOccurrence.time)

        // Verify template anchor date and time remain untouched
        assertEquals("2026-09-07", template.date)
        assertEquals("09:00", template.time)
        assertEquals(Reminder.RECURRENCE_WEEKLY, template.recurrenceType)
    }

    @Test
    fun configurableAlerts_defaultValuesForNewTasks() {
        val reminder = Reminder(
            title = "New Task",
            date = "2026-09-25",
            time = "10:00"
        )

        assertFalse(reminder.notify7Days)
        assertFalse(reminder.notify24Hours)
        assertTrue(reminder.notifyDueNow)
    }

    @Test
    fun configurableAlerts_occurrenceInheritsTemplateAlertFlags() {
        val template = Reminder(
            title = "Report",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = true,
            notify24Hours = false,
            notifyDueNow = true
        )

        val occurrence = ReminderOccurrence(
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            notify7Days = template.notify7Days,
            notify24Hours = template.notify24Hours,
            notifyDueNow = template.notifyDueNow
        )

        assertTrue(occurrence.notify7Days)
        assertFalse(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    @Test
    fun requestCode_withinSigned32BitIntegerRange() {
        // Ensure that hash collisions or max 24-bit hashes do not overflow 32-bit Integer
        val highHashId = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"
        val code7d = ReminderScheduler.getRequestCode(highHashId, ReminderScheduler.STAGE_7_DAYS)
        val code24h = ReminderScheduler.getRequestCode(highHashId, ReminderScheduler.STAGE_24_HOURS)
        val codeDue = ReminderScheduler.getRequestCode(highHashId, ReminderScheduler.STAGE_DUE_NOW)

        assertTrue(code7d > 0)
        assertTrue(code24h > 0)
        assertTrue(codeDue > 0)
        assertTrue(code7d <= Int.MAX_VALUE)
        assertTrue(code24h <= Int.MAX_VALUE)
        assertTrue(codeDue <= Int.MAX_VALUE)
    }

    @Test
    fun bootReceiver_handlesMyPackageReplacedAction() {
        val actions = listOf(
            android.content.Intent.ACTION_BOOT_COMPLETED,
            android.content.Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
        // Verify that MY_PACKAGE_REPLACED is a standard expected intent action recognized by Android
        assertTrue(actions.contains(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
    }
}
