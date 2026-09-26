package com.brixavier.tskr.engine

import com.brixavier.tskr.model.MonthlyMode
import com.brixavier.tskr.model.RecurrenceDayOfWeek
import com.brixavier.tskr.model.RecurrenceOrdinal
import com.brixavier.tskr.model.RecurrenceRule
import com.brixavier.tskr.model.RecurrenceType
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.YearlyMode
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

    // --- APPROVED A0.5C EXECUTABLE EXAMPLES ---

    @Test
    fun a0_5c_example1_weeklyIntervalTwo() {
        // Selected start: Tue Sep 29, 2026. Rule: Every 2 weeks on Monday
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY)
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-29",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-10-12", first)
    }

    @Test
    fun a0_5c_example2_monthlyOrdinalLastThursday() {
        // Selected start: Tue Sep 29, 2026. Rule: Every 3 months on Last Thursday
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            interval = 3,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.LAST,
            weekday = RecurrenceDayOfWeek.THURSDAY
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-29",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-12-31", first)
    }

    @Test
    fun a0_5c_example3_weeklyEveryWednesday() {
        // Selected start: Tue Sep 29, 2026. Rule: Every week on Wednesday
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 1,
            daysOfWeek = setOf(RecurrenceDayOfWeek.WEDNESDAY)
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-29",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-09-30", first)
    }

    @Test
    fun a0_5c_example4_monthlyDayFifteenEveryMonth() {
        // Selected start: Tue Sep 29, 2026. Rule: Day 15 of every 1 month
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            interval = 1,
            monthlyMode = MonthlyMode.DAY_OF_MONTH
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-15",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-10-15", first)
    }

    @Test
    fun a0_5c_example4_monthlyDayFifteenEveryTwoMonths() {
        // Selected start: Tue Sep 29, 2026. Rule: Day 15 of every 2 months
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            interval = 2,
            monthlyMode = MonthlyMode.DAY_OF_MONTH
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-15",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-11-15", first)
    }

    @Test
    fun a0_5c_example5_monthlyLastFridayEveryMonth() {
        // Selected start: Tue Sep 29, 2026. Rule: Every month on Last Friday
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            interval = 1,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.LAST,
            weekday = RecurrenceDayOfWeek.FRIDAY
        )

        val first = RecurrenceEngine.calculateFirstDueDate(
            baseDateStr = "2026-09-29",
            rule = rule,
            searchFromDateStr = "2026-09-29"
        )
        assertEquals("2026-10-30", first)
    }

    @Test
    fun a0_5c_example6_weeklyMultiDaySequence() {
        // Selected start: Tue Sep 29, 2026. Rule: Every 2 weeks on Monday + Wednesday
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY, RecurrenceDayOfWeek.WEDNESDAY)
        )

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

    // --- RECURRENCE 2.0 YEARLY TESTS ---

    @Test
    fun yearly_specificDate_leapYearSequence() {
        val rule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            interval = 1,
            yearlyMode = YearlyMode.SPECIFIC_DATE,
            yearlyMonth = 2
        )

        val y2028 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, "2028-02-29")
        assertEquals("2029-02-28", y2028)

        val y2029 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2028)
        assertEquals("2030-02-28", y2029)

        val y2030 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2029)
        assertEquals("2031-02-28", y2030)

        val y2031 = RecurrenceEngine.calculateNextDueDate("2028-02-29", rule, y2030)
        assertEquals("2032-02-29", y2031)
    }

    @Test
    fun yearly_ordinalWeekday_thanksgivingFourthThursdayInNovember() {
        // Thanksgiving: Fourth Thursday of November
        val rule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            interval = 1,
            yearlyMode = YearlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.FOURTH,
            weekday = RecurrenceDayOfWeek.THURSDAY,
            yearlyMonth = 11
        )

        val y2026 = RecurrenceEngine.calculateFirstDueDate("2026-01-01", rule, "2026-01-01")
        assertEquals("2026-11-26", y2026)

        val y2027 = RecurrenceEngine.calculateNextDueDate("2026-01-01", rule, y2026)
        assertEquals("2027-11-25", y2027)
    }
}
