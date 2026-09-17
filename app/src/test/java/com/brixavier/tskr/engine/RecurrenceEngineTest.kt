package com.brixavier.tskr.engine

import com.brixavier.tskr.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceEngineTest {

    // --- DAILY RECURRENCE TESTS ---

    @Test
    fun daily_normalNextOccurrence() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-02", next)
    }

    @Test
    fun daily_completionBeforeNextScheduledOccurrence() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-02", next)
    }

    @Test
    fun daily_completionAfterOneMissedOccurrence() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-03"
        )
        assertEquals("2026-09-04", next)
    }

    @Test
    fun daily_multipleMissedPeriods() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-10"
        )
        assertEquals("2026-09-11", next)
    }

    // --- WEEKLY RECURRENCE TESTS ---

    @Test
    fun weekly_sameWeekCompletion() {
        // Monday 2026-09-07
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-07",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-07"
        )
        assertEquals("2026-09-14", next)
    }

    @Test
    fun weekly_completionSeveralDaysLate() {
        // Monday 2026-09-07, completed Thursday 2026-09-10
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-07",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-10"
        )
        // Next occurrence remains coming Monday 2026-09-14
        assertEquals("2026-09-14", next)
    }

    @Test
    fun weekly_intervalGreaterThanOneWeek() {
        // Every 2 weeks starting Monday 2026-09-07
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-07",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 2,
            afterDateStr = "2026-09-07"
        )
        assertEquals("2026-09-21", next)
    }

    @Test
    fun weekly_weekdayPreservation() {
        // Wednesday 2026-09-02, evaluated past two weeks on 2026-09-15
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-02",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-15"
        )
        // Next Wednesday is 2026-09-16
        assertEquals("2026-09-16", next)
    }

    // --- MONTHLY RECURRENCE TESTS ---

    @Test
    fun monthly_normalMonth() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-01-15",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-01-15"
        )
        assertEquals("2026-02-15", next)
    }

    @Test
    fun monthly_31stTo30DayMonth() {
        // May 31 to June (30 days)
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-05-31",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-05-31"
        )
        assertEquals("2026-06-30", next)
    }

    @Test
    fun monthly_31stToFebruaryNonLeapYear() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-01-31",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-01-31"
        )
        assertEquals("2026-02-28", next)
    }

    @Test
    fun monthly_31stToFebruaryLeapYear() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2028-01-31",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2028-01-31"
        )
        assertEquals("2028-02-29", next)
    }

    @Test
    fun monthly_returnFrom30DayMonthToOriginal31stAnchor() {
        // Base is May 31. June occurrence was June 30.
        // Evaluating after June 30 must restore July 31!
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-05-31",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-06-30"
        )
        assertEquals("2026-07-31", next)
    }

    @Test
    fun monthly_multipleMissedMonths() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-01-31",
            recurrenceType = Reminder.RECURRENCE_MONTHLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-05-15"
        )
        // Next future occurrence in May is May 31
        assertEquals("2026-05-31", next)
    }

    // --- CUSTOM_DAYS RECURRENCE TESTS ---

    @Test
    fun customDays_normalInterval() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 3,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-04", next)
    }

    @Test
    fun customDays_lateCompletion() {
        // Grid: Sept 1, 4, 7, 10...
        // Completed late on Sept 8 -> Next is Sept 10 (grid preserved!)
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 3,
            afterDateStr = "2026-09-08"
        )
        assertEquals("2026-09-10", next)
    }

    @Test
    fun customDays_multipleMissedPeriods() {
        // Grid: Sept 1, 4, 7, 10...
        // Evaluated on Sept 9 -> Next is Sept 10
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 3,
            afterDateStr = "2026-09-09"
        )
        assertEquals("2026-09-10", next)
    }

    @Test
    fun customDays_intervalGreaterThanOne() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_CUSTOM_DAYS,
            recurrenceInterval = 5,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-06", next)
    }

    // --- NO BACKLOG & STRICT ANCHORING TESTS ---

    @Test
    fun noBacklog_returnsSingleNextFutureOccurrence() {
        // Overdue task from Jan 1 evaluated in September
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-01-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-10"
        )
        // Directly advances to single next future date Sept 11
        assertEquals("2026-09-11", next)
    }

    @Test
    fun strictAnchoring_completionDateDoesNotShiftGrid() {
        // Weekly Monday 2026-09-07 task completed Thursday 2026-09-10
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-07",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-10"
        )
        // Does NOT return Thursday 2026-09-17; returns Monday 2026-09-14!
        assertEquals("2026-09-14", next)
    }

    @Test
    fun snooze_recurrenceAnchorUnchanged() {
        // Master anchor date is Monday 2026-09-07
        // Occurrence was snoozed to Tuesday 2026-09-08
        // When calculating next recurrence from base anchor, it yields Monday 2026-09-14
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-07",
            recurrenceType = Reminder.RECURRENCE_WEEKLY,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-08"
        )
        assertEquals("2026-09-14", next)
    }

    // --- INVALID INTERVAL TESTS ---

    @Test
    fun invalidInterval_zeroIntervalNormalizedToOne() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 0,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-02", next)
    }

    @Test
    fun invalidInterval_negativeIntervalNormalizedToOne() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = -5,
            afterDateStr = "2026-09-01"
        )
        assertEquals("2026-09-02", next)
    }

    // --- RECURRENCE NONE TESTS ---

    @Test
    fun none_returnsNull() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-09-01",
            recurrenceType = Reminder.RECURRENCE_NONE,
            recurrenceInterval = 1,
            afterDateStr = "2026-09-01"
        )
        assertNull(next)
    }

    // --- DATE BOUNDARY & LEAP YEAR TESTS ---

    @Test
    fun dateEdgeCase_monthBoundary() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-01-30",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-01-31"
        )
        assertEquals("2026-02-01", next)
    }

    @Test
    fun dateEdgeCase_yearBoundary() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2026-12-30",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2026-12-31"
        )
        assertEquals("2027-01-01", next)
    }

    @Test
    fun dateEdgeCase_leapYearFebruary29() {
        val next = RecurrenceEngine.calculateNextDueDate(
            baseDateStr = "2028-02-28",
            recurrenceType = Reminder.RECURRENCE_DAILY,
            recurrenceInterval = 1,
            afterDateStr = "2028-02-28"
        )
        assertEquals("2028-02-29", next)
    }
}
