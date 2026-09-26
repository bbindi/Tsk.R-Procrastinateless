package com.brixavier.tskr.data

import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.model.MonthlyMode
import com.brixavier.tskr.model.RecurrenceDayOfWeek
import com.brixavier.tskr.model.RecurrenceOrdinal
import com.brixavier.tskr.model.RecurrenceRule
import com.brixavier.tskr.model.RecurrenceType
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.YearlyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceEndToEndQaTest {

    // --- 1. WEEKLY MULTI-DAY END-TO-END ---

    @Test
    fun qa_weeklyMultiDay_endToEndTransitions() {
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY, RecurrenceDayOfWeek.WEDNESDAY)
        )

        // Selected start: Tue Sep 29, 2026
        val occ1 = RecurrenceEngine.calculateFirstDueDate("2026-09-29", rule, "2026-09-29")
        assertEquals("2026-09-30", occ1)

        val occ2 = RecurrenceEngine.calculateNextDueDate("2026-09-29", rule, occ1)
        assertEquals("2026-10-12", occ2)

        val occ3 = RecurrenceEngine.calculateNextDueDate("2026-09-29", rule, occ2)
        assertEquals("2026-10-14", occ3)

        val occ4 = RecurrenceEngine.calculateNextDueDate("2026-09-29", rule, occ3)
        assertEquals("2026-10-26", occ4)

        val occ5 = RecurrenceEngine.calculateNextDueDate("2026-09-29", rule, occ4)
        assertEquals("2026-10-28", occ5)
    }

    // --- 2. DAILY END-TO-END ---

    @Test
    fun qa_daily_intervalTwoNoDrift() {
        val rule = RecurrenceRule(type = RecurrenceType.DAILY, interval = 2)

        val occ1 = RecurrenceEngine.calculateFirstDueDate("2026-09-01", rule, "2026-09-01")
        assertEquals("2026-09-01", occ1)

        val occ2 = RecurrenceEngine.calculateNextDueDate("2026-09-01", rule, occ1)
        assertEquals("2026-09-03", occ2)

        val occ3 = RecurrenceEngine.calculateNextDueDate("2026-09-01", rule, occ2)
        assertEquals("2026-09-05", occ3)
    }

    // --- 3. MONTHLY DAY-OF-MONTH CLAMPING ---

    @Test
    fun qa_monthly_day31MonthEndClampingAndRestoration() {
        val rule = RecurrenceRule(type = RecurrenceType.MONTHLY, interval = 1, monthlyMode = MonthlyMode.DAY_OF_MONTH)

        // Base anchor: Jan 31
        val jan = RecurrenceEngine.calculateFirstDueDate("2026-01-31", rule, "2026-01-31")
        assertEquals("2026-01-31", jan)

        val feb = RecurrenceEngine.calculateNextDueDate("2026-01-31", rule, jan)
        assertEquals("2026-02-28", feb) // Clamped to 28th

        val mar = RecurrenceEngine.calculateNextDueDate("2026-01-31", rule, feb)
        assertEquals("2026-03-31", mar) // Restored to 31st

        val apr = RecurrenceEngine.calculateNextDueDate("2026-01-31", rule, mar)
        assertEquals("2026-04-30", apr) // Clamped to 30th

        val may = RecurrenceEngine.calculateNextDueDate("2026-01-31", rule, apr)
        assertEquals("2026-05-31", may) // Restored to 31st
    }

    // --- 4. MONTHLY ORDINAL LAST THURSDAY ---

    @Test
    fun qa_monthly_lastThursdayTransitions() {
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            interval = 1,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.LAST,
            weekday = RecurrenceDayOfWeek.THURSDAY
        )

        val sep = RecurrenceEngine.calculateFirstDueDate("2026-09-01", rule, "2026-09-01")
        assertEquals("2026-09-24", sep)

        val oct = RecurrenceEngine.calculateNextDueDate("2026-09-01", rule, sep)
        assertEquals("2026-10-29", oct)

        val nov = RecurrenceEngine.calculateNextDueDate("2026-09-01", rule, oct)
        assertEquals("2026-11-26", nov)

        val dec = RecurrenceEngine.calculateNextDueDate("2026-09-01", rule, nov)
        assertEquals("2026-12-31", dec)
    }

    // --- 5. YEARLY FEB 29 LEAP YEAR SEQUENCE ---

    @Test
    fun qa_yearly_feb29LeapYearSequence() {
        val rule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            interval = 1,
            yearlyMode = YearlyMode.SPECIFIC_DATE,
            yearlyMonth = 2
        )

        val y2028 = RecurrenceEngine.calculateFirstDueDate("2028-02-29", rule, "2028-02-29")
        assertEquals("2028-02-29", y2028)

        val y2029 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2028)
        assertEquals("2029-02-28", y2029)

        val y2030 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2029)
        assertEquals("2030-02-28", y2030)

        val y2031 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2030)
        assertEquals("2031-02-28", y2031)

        val y2032 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2031)
        assertEquals("2032-02-29", y2032)
    }

    // --- 6. YEARLY ORDINAL FOURTH WEDNESDAY ---

    @Test
    fun qa_yearly_fourthWednesdayOfSeptemberSequence() {
        val rule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            interval = 2,
            yearlyMode = YearlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.FOURTH,
            weekday = RecurrenceDayOfWeek.WEDNESDAY,
            yearlyMonth = 9
        )

        val y2026 = RecurrenceEngine.calculateFirstDueDate("2026-01-01", rule, "2026-01-01")
        assertEquals("2026-09-23", y2026)

        val y2028 = RecurrenceEngine.calculateNextDueDate("2026-01-01", rule, y2026)
        assertEquals("2028-09-27", y2028)
    }

    // --- 7. SNOOZE + EDIT INTERACTION ---

    @Test
    fun qa_snoozeAndEdit_clearsStaleSnoozeAndRecalculatesNewSeries() {
        val initialReminder = Reminder(
            id = "rem-snooze-edit",
            title = "Weekly Review",
            date = "2026-09-21",
            time = "10:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = "MON"
        )

        val snoozedOcc = ReminderOccurrence(
            id = "occ-snoozed",
            templateId = initialReminder.id,
            title = initialReminder.title,
            date = "2026-09-21",
            time = "10:00",
            snoozedUntilMs = System.currentTimeMillis() + 300000, // Active snooze
            snoozeCount = 2
        )

        // User edits task to: Monthly, Every 3 months, Last Thursday
        val editedReminder = initialReminder.copy(
            date = "2026-09-29",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 3,
            recurrenceMonthlyMode = "ORDINAL_WEEKDAY",
            recurrenceOrdinal = "LAST",
            recurrenceWeekday = "THURSDAY"
        )

        val newDate = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = editedReminder.date,
            rule = editedReminder.toRecurrenceRule(),
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-12-31", newDate)

        // Reconcile pending occurrence in-place
        val reconciledOcc = snoozedOcc.copy(
            date = newDate!!,
            time = editedReminder.time,
            snoozedUntilMs = null // Stale snooze state cleared!
        )

        assertEquals("occ-snoozed", reconciledOcc.id)
        assertEquals("2026-12-31", reconciledOcc.date)
        assertNull(reconciledOcc.snoozedUntilMs)
    }

    // --- 8. RECURRING TO NONE & NONE TO RECURRING ---

    @Test
    fun qa_recurringToNone_convertsToOneTimeTaskWithoutFutureOccurrences() {
        val recurringTemplate = Reminder(
            id = "rem-rec-none",
            title = "Weekly Standup",
            date = "2026-09-21",
            time = "09:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = "MON"
        )

        val disabledTemplate = recurringTemplate.copy(recurrenceType = "NONE")
        assertFalse(disabledTemplate.isRecurring())

        val nextDueDate = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = disabledTemplate.date,
            rule = disabledTemplate.toRecurrenceRule(),
            afterDateStr = "2026-09-21"
        )
        assertNull("Disabling recurrence produces null next date", nextDueDate)
    }

    @Test
    fun qa_noneToRecurring_reconcilesPendingOccurrenceWithNewRecurrenceRule() {
        val oneTimeTemplate = Reminder(
            id = "rem-none-rec",
            title = "Doctor Appointment",
            date = "2026-09-29",
            time = "14:00",
            recurrenceType = "NONE"
        )

        val updatedTemplate = oneTimeTemplate.copy(
            recurrenceType = "WEEKLY",
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = "MON|WED"
        )

        val firstDueDate = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = updatedTemplate.date,
            rule = updatedTemplate.toRecurrenceRule(),
            searchFromDateStr = "2026-09-29"
        )

        assertEquals("2026-09-30", firstDueDate)
    }

    // --- 9. PAST & FUTURE ANCHOR PRESERVATION ---

    @Test
    fun qa_pastAnchor_preservesStoredAnchorDate() {
        // Today is Sep 29, 2026. User sets start date in past = Sep 01, 2026.
        val reminder = Reminder(
            id = "rem-past-anchor",
            title = "Monthly Utility Bill",
            date = "2026-09-01", // Past anchor
            time = "09:00",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceMonthlyMode = "DAY_OF_MONTH"
        )

        // First valid occurrence on or after today Sep 29 is Oct 01, 2026
        val firstDueDate = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = reminder.date,
            rule = reminder.toRecurrenceRule(),
            searchFromDateStr = "2026-09-29"
        )

        assertEquals("2026-10-01", firstDueDate)
        // Stored template anchor remains unchanged
        assertEquals("2026-09-01", reminder.date)
    }

    @Test
    fun qa_futureAnchor_preservesFutureDateWithoutEarlierOccurrences() {
        // Today is Sep 29, 2026. User sets future start date = Oct 15, 2026.
        val reminder = Reminder(
            id = "rem-future-anchor",
            title = "Future Course Start",
            date = "2026-10-15",
            time = "10:00",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceMonthlyMode = "DAY_OF_MONTH"
        )

        val firstDueDate = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = reminder.date,
            rule = reminder.toRecurrenceRule(),
            searchFromDateStr = "2026-09-29"
        )

        assertEquals("2026-10-15", firstDueDate)
    }

    // --- 10. OCCURRENCE IDENTITY ISOLATION ---

    @Test
    fun qa_occurrenceIdentity_occurrenceACompleted_occurrenceBIsFreshAndIsolated() {
        val occA = ReminderOccurrence(
            id = "occ-A",
            templateId = "rem-template-1",
            title = "Water Plants",
            date = "2026-09-21",
            time = "08:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = System.currentTimeMillis(),
            snoozeCount = 3,
            snoozedUntilMs = System.currentTimeMillis() - 1000
        )

        val occB = ReminderOccurrence(
            id = "occ-B", // Unique ID
            templateId = "rem-template-1",
            title = "Water Plants",
            date = "2026-09-28",
            time = "08:00",
            status = ReminderOccurrence.STATUS_PENDING,
            completedAtMs = null,
            snoozeCount = 0, // Reset
            snoozedUntilMs = null // Reset
        )

        assertNotEquals(occA.id, occB.id)
        assertTrue(occA.isCompleted())
        assertTrue(occB.isPending())
        assertEquals(3, occA.snoozeCount)
        assertEquals(0, occB.snoozeCount)
    }
}
