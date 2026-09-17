package com.brixavier.tskr.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyProgressTest {

    // Reference Friday date: 2026-09-25
    // Current Week Bounds: Monday 2026-09-21 to Sunday 2026-09-27
    private val refDate = LocalDate.of(2026, 9, 25)
    private val mondayStr = "2026-09-21"
    private val sundayStr = "2026-09-27"

    @Test
    fun testGetWeekBounds_returnsCorrectMondayToSundayDates() {
        val (start, end) = WeeklyProgressCalculator.getWeekBounds(refDate)
        assertEquals(mondayStr, start)
        assertEquals(sundayStr, end)
    }

    @Test
    fun testEmptyWeek_returnsZeroTotalAndZeroRatio_noDivideByZero() {
        val result = WeeklyProgressCalculator.calculate(
            occurrences = emptyList(),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(0, result.completedCount)
        assertEquals(0, result.totalScheduledCount)
        assertEquals(0.0f, result.progressRatio, 0.001f)
        assertEquals(0, result.progressPercent)
    }

    @Test
    fun testOneScheduledOneCompletedOccurrence_calculatesFiftyPercentRatio() {
        val occ1 = ReminderOccurrence(id = "o1", title = "Task 1", date = "2026-09-22", time = "10:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val occ2 = ReminderOccurrence(id = "o2", title = "Task 2", date = "2026-09-23", time = "14:00", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(occ1, occ2),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(1, result.completedCount)
        assertEquals(2, result.totalScheduledCount)
        assertEquals(0.5f, result.progressRatio, 0.001f)
        assertEquals(50, result.progressPercent)
    }

    @Test
    fun testMixedCompletedAndPendingOccurrences_calculatesCorrectRatio() {
        val occ1 = ReminderOccurrence(id = "o1", title = "Task 1", date = "2026-09-21", time = "09:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val occ2 = ReminderOccurrence(id = "o2", title = "Task 2", date = "2026-09-22", time = "10:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val occ3 = ReminderOccurrence(id = "o3", title = "Task 3", date = "2026-09-24", time = "11:00", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(occ1, occ2, occ3),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(2, result.completedCount)
        assertEquals(3, result.totalScheduledCount)
        assertEquals(2.0f / 3.0f, result.progressRatio, 0.001f)
        assertEquals(66, result.progressPercent)
    }

    @Test
    fun testRecurringTaskWithMultipleOccurrencesInSameWeek_countsAllOccurrences() {
        // Daily recurring task generating 7 occurrences in Monday-Sunday week
        val dailyOccurrences = (21..27).map { day ->
            val dateStr = "2026-09-$day"
            ReminderOccurrence(
                id = "occ-daily-$day",
                templateId = "daily-template",
                title = "Daily Gym",
                date = dateStr,
                time = "08:00",
                status = if (day <= 24) ReminderOccurrence.STATUS_COMPLETED else ReminderOccurrence.STATUS_PENDING
            )
        }

        val result = WeeklyProgressCalculator.calculate(
            occurrences = dailyOccurrences,
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(4, result.completedCount)
        assertEquals(7, result.totalScheduledCount)
        assertEquals(4.0f / 7.0f, result.progressRatio, 0.001f)
        assertEquals(57, result.progressPercent)
    }

    @Test
    fun testOccurrencesOutsideCurrentWeek_areExcludedFromCalculation() {
        val priorWeekOcc = ReminderOccurrence(id = "o-prev", title = "Old Task", date = "2026-09-20", time = "10:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val currentWeekOcc = ReminderOccurrence(id = "o-curr", title = "Current Task", date = "2026-09-23", time = "10:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val nextWeekOcc = ReminderOccurrence(id = "o-next", title = "Future Task", date = "2026-09-28", time = "10:00", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(priorWeekOcc, currentWeekOcc, nextWeekOcc),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(1, result.completedCount)
        assertEquals(1, result.totalScheduledCount)
        assertEquals(1.0f, result.progressRatio, 0.001f)
    }

    @Test
    fun testMondayBoundary_includesOccurrenceOnMonday() {
        val mondayOcc = ReminderOccurrence(id = "o-mon", title = "Monday Task", date = "2026-09-21", time = "00:00", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(mondayOcc),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(1, result.totalScheduledCount)
    }

    @Test
    fun testSundayBoundary_includesOccurrenceOnSunday() {
        val sundayOcc = ReminderOccurrence(id = "o-sun", title = "Sunday Task", date = "2026-09-27", time = "23:59", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(sundayOcc),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(1, result.totalScheduledCount)
    }

    @Test
    fun testNextMondayStartsNewWeek_excludesPreviousWeekOccurrences() {
        val nextMondayRef = LocalDate.of(2026, 9, 28)
        val (nextMonStr, nextSunStr) = WeeklyProgressCalculator.getWeekBounds(nextMondayRef)

        assertEquals("2026-09-28", nextMonStr)
        assertEquals("2026-10-04", nextSunStr)

        val lastWeekOcc = ReminderOccurrence(id = "o-last", title = "Last Week Task", date = "2026-09-27", time = "20:00", status = ReminderOccurrence.STATUS_COMPLETED)
        val newWeekOcc = ReminderOccurrence(id = "o-new", title = "New Week Task", date = "2026-09-28", time = "09:00", status = ReminderOccurrence.STATUS_PENDING)

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(lastWeekOcc, newWeekOcc),
            mondayStr = nextMonStr,
            sundayStr = nextSunStr
        )

        assertEquals(0, result.completedCount)
        assertEquals(1, result.totalScheduledCount)
        assertEquals(0.0f, result.progressRatio, 0.001f)
    }

    @Test
    fun testCompletedOccurrencesFromPreviousWeeks_doNotCountInCurrentWeek() {
        val oldCompletedOcc = ReminderOccurrence(
            id = "o-old-completed",
            title = "Completed Long Ago",
            date = "2026-09-14", // Previous Monday
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = 1700000000000L
        )

        val currentPendingOcc = ReminderOccurrence(
            id = "o-curr-pending",
            title = "Pending Today",
            date = "2026-09-25",
            time = "12:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val result = WeeklyProgressCalculator.calculate(
            occurrences = listOf(oldCompletedOcc, currentPendingOcc),
            mondayStr = mondayStr,
            sundayStr = sundayStr
        )

        assertEquals(0, result.completedCount)
        assertEquals(1, result.totalScheduledCount)
        assertEquals(0.0f, result.progressRatio, 0.001f)
    }
}
