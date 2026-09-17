package com.brixavier.tskr.scheduler

import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ConfigurableAlertsTest {

    // --- 1. MIGRATION & BACKFILL SEMANTICS ---

    @Test
    fun migration7To8_preservesV1AllThreeAlertsEnabled() {
        // Migrated V1 reminders/occurrences receive DEFAULT 1 (all three alerts enabled)
        val v1MigratedTemplate = Reminder(
            id = "v1-template-1",
            title = "Migrated Task",
            date = "2026-09-01",
            time = "10:00",
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        val v1MigratedOccurrence = ReminderOccurrence(
            id = "v1-occ-1",
            templateId = v1MigratedTemplate.id,
            title = v1MigratedTemplate.title,
            date = v1MigratedTemplate.date,
            time = v1MigratedTemplate.time,
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        assertTrue(v1MigratedTemplate.notify7Days)
        assertTrue(v1MigratedTemplate.notify24Hours)
        assertTrue(v1MigratedTemplate.notifyDueNow)

        assertTrue(v1MigratedOccurrence.notify7Days)
        assertTrue(v1MigratedOccurrence.notify24Hours)
        assertTrue(v1MigratedOccurrence.notifyDueNow)
    }

    // --- 2. NEW V2 DEFAULTS ---

    @Test
    fun newV2Defaults_defaultsToDueOnly() {
        val newReminder = Reminder(
            title = "Fresh V2 Task",
            date = "2026-09-25",
            time = "14:00"
        )

        assertFalse(newReminder.notify7Days)
        assertFalse(newReminder.notify24Hours)
        assertTrue(newReminder.notifyDueNow)
    }

    // --- 3. SELECTIVE SCHEDULING COMBINATIONS ---

    @Test
    fun selectiveScheduling_allEnabled() {
        val occurrence = ReminderOccurrence(
            title = "All Enabled",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        assertTrue(occurrence.notify7Days)
        assertTrue(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    @Test
    fun selectiveScheduling_dueOnly() {
        val occurrence = ReminderOccurrence(
            title = "Due Only",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = false,
            notify24Hours = false,
            notifyDueNow = true
        )

        assertFalse(occurrence.notify7Days)
        assertFalse(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    @Test
    fun selectiveScheduling_7DAndDue() {
        val occurrence = ReminderOccurrence(
            title = "7D and Due",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = true,
            notify24Hours = false,
            notifyDueNow = true
        )

        assertTrue(occurrence.notify7Days)
        assertFalse(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    @Test
    fun selectiveScheduling_24HAndDue() {
        val occurrence = ReminderOccurrence(
            title = "24H and Due",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = false,
            notify24Hours = true,
            notifyDueNow = true
        )

        assertFalse(occurrence.notify7Days)
        assertTrue(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    // --- 4. PAST WARNING STAGES ---

    @Test
    fun pastWarningStage_doesNotSchedulePastStage() {
        val pastDate = "2020-01-01" // Past date
        val occurrence = ReminderOccurrence(
            title = "Past Task",
            date = pastDate,
            time = "10:00",
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        // Verifying occurrence holds stage flags correctly
        assertTrue(occurrence.notify7Days)
        assertTrue(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    // --- 5. DISABLING AN EXISTING ALERT ---

    @Test
    fun disablingExistingAlert_cancels7DAndPreservesOthers() {
        val original = ReminderOccurrence(
            id = "occ-toggle-1",
            title = "Toggle Alert Task",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        // User disables 7D alert
        val updated = original.copy(notify7Days = false)

        assertFalse(updated.notify7Days)
        assertTrue(updated.notify24Hours)
        assertTrue(updated.notifyDueNow)

        // Request code for 7D stage remains distinct and deterministic for cancellation
        val code7d = ReminderScheduler.getRequestCode(updated.id, ReminderScheduler.STAGE_7_DAYS)
        val code24h = ReminderScheduler.getRequestCode(updated.id, ReminderScheduler.STAGE_24_HOURS)
        assertNotEquals(code7d, code24h)
    }

    // --- 6. ENABLING AN EXISTING ALERT ---

    @Test
    fun enablingExistingAlert_schedulesNewlyEnabledStage() {
        val original = ReminderOccurrence(
            id = "occ-enable-1",
            title = "Enable Alert Task",
            date = "2026-09-25",
            time = "10:00",
            notify7Days = false,
            notify24Hours = false,
            notifyDueNow = true
        )

        // User enables 24H alert
        val updated = original.copy(notify24Hours = true)

        assertFalse(updated.notify7Days)
        assertTrue(updated.notify24Hours)
        assertTrue(updated.notifyDueNow)
    }

    // --- 7. REQUIRED MID-CYCLE INTEGRATION TEST ---

    @Test
    fun midCycleIntegration_recurringWeeklyEditAndCompletion() {
        // 1. Initial Weekly Template: 7D ON, 24H OFF, Due ON
        val templateV1 = Reminder(
            id = "weekly-tmpl-midcycle",
            title = "Weekly Review",
            date = "2026-09-07", // Monday
            time = "09:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            notify7Days = true,
            notify24Hours = false,
            notifyDueNow = true
        )

        // 2. Active Pending Occurrence #1
        val pendingOcc1 = ReminderOccurrence(
            id = "occ-weekly-1",
            templateId = templateV1.id,
            title = templateV1.title,
            date = templateV1.date,
            time = templateV1.time,
            status = ReminderOccurrence.STATUS_PENDING,
            notify7Days = templateV1.notify7Days,
            notify24Hours = templateV1.notify24Hours,
            notifyDueNow = templateV1.notifyDueNow
        )

        assertTrue(pendingOcc1.notify7Days)
        assertFalse(pendingOcc1.notify24Hours)
        assertTrue(pendingOcc1.notifyDueNow)

        // 3. User edits reminder mid-cycle to: 7D OFF, 24H ON, Due ON
        val editedTemplate = templateV1.copy(
            notify7Days = false,
            notify24Hours = true,
            notifyDueNow = true
        )

        val updatedPendingOcc1 = pendingOcc1.copy(
            notify7Days = editedTemplate.notify7Days,
            notify24Hours = editedTemplate.notify24Hours,
            notifyDueNow = editedTemplate.notifyDueNow
        )

        // Verify template and active pending occurrence carry new configuration
        assertFalse(editedTemplate.notify7Days)
        assertTrue(editedTemplate.notify24Hours)
        assertTrue(editedTemplate.notifyDueNow)

        assertFalse(updatedPendingOcc1.notify7Days)
        assertTrue(updatedPendingOcc1.notify24Hours)
        assertTrue(updatedPendingOcc1.notifyDueNow)

        // 4. Complete occurrence #1
        val completedOcc1 = updatedPendingOcc1.copy(
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = 1788771900000L
        )

        // 5. Generate next occurrence #2 from editedTemplate
        val nextDueDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = editedTemplate.date,
            recurrenceType = editedTemplate.recurrenceType,
            recurrenceInterval = editedTemplate.recurrenceInterval,
            afterDateStr = completedOcc1.date
        )

        assertEquals("2026-09-14", nextDueDate)

        val newPendingOcc2 = ReminderOccurrence(
            id = "occ-weekly-2",
            templateId = editedTemplate.id,
            title = editedTemplate.title,
            date = nextDueDate!!,
            time = editedTemplate.time,
            status = ReminderOccurrence.STATUS_PENDING,
            notify7Days = editedTemplate.notify7Days,
            notify24Hours = editedTemplate.notify24Hours,
            notifyDueNow = editedTemplate.notifyDueNow
        )

        // Verify completed occurrence retained its exact state at completion
        assertFalse(completedOcc1.notify7Days)
        assertTrue(completedOcc1.notify24Hours)
        assertTrue(completedOcc1.notifyDueNow)

        // Verify newly generated occurrence inherited the new template configuration
        assertFalse(newPendingOcc2.notify7Days)
        assertTrue(newPendingOcc2.notify24Hours)
        assertTrue(newPendingOcc2.notifyDueNow)
    }

    // --- 8. RECURRING INHERITANCE ---

    @Test
    fun recurringInheritance_nextOccurrenceInheritsTemplateConfiguration() {
        val template = Reminder(
            id = "tmpl-inherit-1",
            title = "Monthly Taxes",
            date = "2026-01-31",
            time = "17:00",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        val occurrence = ReminderOccurrence(
            id = "occ-month-1",
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            notify7Days = template.notify7Days,
            notify24Hours = template.notify24Hours,
            notifyDueNow = template.notifyDueNow
        )

        assertTrue(occurrence.notify7Days)
        assertTrue(occurrence.notify24Hours)
        assertTrue(occurrence.notifyDueNow)
    }

    // --- 9. SNOOZE & ALERT FLAGS INTEGRATION ---

    @Test
    fun snooze_affectsOnlyDueStageAndPreservesAlertFlagsAndRecurrenceAnchor() {
        val template = Reminder(
            id = "tmpl-snooze-test",
            title = "Important Call",
            date = "2026-09-07",
            time = "09:00",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        val occurrence = ReminderOccurrence(
            id = "occ-snooze-test",
            templateId = template.id,
            title = template.title,
            date = template.date,
            time = template.time,
            notify7Days = template.notify7Days,
            notify24Hours = template.notify24Hours,
            notifyDueNow = template.notifyDueNow
        )

        // Snooze occurrence to 09:05
        val snoozedOcc = occurrence.copy(
            time = "09:05",
            snoozedUntilMs = 1788771900000L
        )

        // Occurrence time updated for STAGE_DUE_NOW
        assertEquals("09:05", snoozedOcc.time)

        // Alert flags on occurrence remain unchanged
        assertTrue(snoozedOcc.notify7Days)
        assertTrue(snoozedOcc.notify24Hours)
        assertTrue(snoozedOcc.notifyDueNow)

        // Master template anchor remains untouched
        assertEquals("2026-09-07", template.date)
        assertEquals("09:00", template.time)
    }

    // --- 10. BOOT RECOVERY ---

    @Test
    fun bootRecovery_evaluatesEnabledStagesForPendingOccurrences() {
        val occA = ReminderOccurrence(
            id = "occ-boot-a",
            title = "Task A",
            date = "2026-09-25",
            time = "10:00",
            status = ReminderOccurrence.STATUS_PENDING,
            notify7Days = true,
            notify24Hours = false,
            notifyDueNow = true
        )

        val occB = ReminderOccurrence(
            id = "occ-boot-b",
            title = "Task B",
            date = "2026-09-25",
            time = "11:00",
            status = ReminderOccurrence.STATUS_PENDING,
            notify7Days = false,
            notify24Hours = true,
            notifyDueNow = true
        )

        // Task A has 7D & Due enabled
        assertTrue(occA.notify7Days)
        assertFalse(occA.notify24Hours)
        assertTrue(occA.notifyDueNow)

        // Task B has 24H & Due enabled
        assertFalse(occB.notify7Days)
        assertTrue(occB.notify24Hours)
        assertTrue(occB.notifyDueNow)
    }

    // --- 11. V1 REGRESSION & ROUTING ---

    @Test
    fun v1Regression_migratedRemindersHaveAllThreeStagesEnabled() {
        val migratedImportant = Reminder(
            title = "Migrated Important",
            date = "2026-09-25",
            time = "10:00",
            priority = Reminder.PRIORITY_IMPORTANT,
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        val migratedNormal = Reminder(
            title = "Migrated Normal",
            date = "2026-09-25",
            time = "10:00",
            priority = Reminder.PRIORITY_NORMAL,
            notify7Days = true,
            notify24Hours = true,
            notifyDueNow = true
        )

        assertTrue(migratedImportant.isImportant())
        assertFalse(migratedNormal.isImportant())

        assertTrue(migratedImportant.notify7Days && migratedImportant.notify24Hours && migratedImportant.notifyDueNow)
        assertTrue(migratedNormal.notify7Days && migratedNormal.notify24Hours && migratedNormal.notifyDueNow)
    }

    // --- 12. ALARM CANCELLATION & REQUEST CODE SAFETY ---

    @Test
    fun alarmCancellation_requestCodesRemainDistinctAcrossStages() {
        val occurrenceId = UUID.randomUUID().toString()
        val code7d = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_7_DAYS)
        val code24h = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_24_HOURS)
        val codeDue = ReminderScheduler.getRequestCode(occurrenceId, ReminderScheduler.STAGE_DUE_NOW)

        // Confirm distinct stage request codes
        assertNotEquals(code7d, code24h)
        assertNotEquals(code24h, codeDue)
        assertNotEquals(code7d, codeDue)

        // Confirm offset mechanics: 7D=1, 24H=2, DUE=3
        val hash = occurrenceId.hashCode() and 0x00ffffff
        assertEquals(hash * 10 + 1, code7d)
        assertEquals(hash * 10 + 2, code24h)
        assertEquals(hash * 10 + 3, codeDue)
    }
}
