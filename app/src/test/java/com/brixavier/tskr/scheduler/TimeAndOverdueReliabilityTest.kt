package com.brixavier.tskr.scheduler

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.ui.getRelativeTimeSpan
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TimeAndOverdueReliabilityTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    @Test
    fun testMissionMode_recurringOccurrence_usesOccurrenceDateTimeForRelativeDisplay() {
        // Master template created on 2026-09-20 at 18:00
        val template = Reminder(
            id = "template-1",
            title = "Daily Evening Routine",
            date = "2026-09-20",
            time = "18:00",
            recurrenceType = Reminder.RECURRENCE_DAILY
        )

        // Current active occurrence scheduled for 2026-09-26 at 10:00
        val occurrence = ReminderOccurrence(
            id = "occ-2",
            templateId = template.id,
            title = template.title,
            date = "2026-09-26",
            time = "10:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        // Reference evaluation time: 2026-09-26 at 10:05 (5 minutes after occurrence trigger)
        val now = sdf.parse("2026-09-26 10:05")!!

        // Evaluating relative time span using the master template's date/time (THE BUG)
        val wrongDisplay = getRelativeTimeSpan(template.date, template.time, now)

        // Evaluating relative time span using the active occurrence's date/time (THE FIX)
        val activeDisplay = getRelativeTimeSpan(occurrence.date, occurrence.time, now)

        // The active occurrence relative display must be "Overdue by 5 minutes"
        assertEquals("Overdue by 5 minutes", activeDisplay)

        // The active display must NOT use the template's date/time ("Overdue by 136 hours" or "16 hours")
        assertNotEquals(wrongDisplay, activeDisplay)
        assertFalse(activeDisplay.contains("136 hours") || activeDisplay.contains("16 hours"))
    }

    private fun assertFalse(condition: Boolean) {
        Assert.assertFalse(condition)
    }

    @Test
    fun test1_justBeforeDue_future5Minutes() {
        val now = sdf.parse("2026-09-26 10:00")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:05", now)
        assertEquals("Starts in 5 minutes", display)
    }

    @Test
    fun test2_exactlyDue_0Minutes() {
        val now = sdf.parse("2026-09-26 10:00")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Starts in less than a minute", display)
    }

    @Test
    fun test3_aFewMinutesOverdue_5MinutesPast() {
        val now = sdf.parse("2026-09-26 10:05")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Overdue by 5 minutes", display)
    }

    @Test
    fun test4_59MinutesOverdue() {
        val now = sdf.parse("2026-09-26 10:59")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Overdue by 59 minutes", display)
    }

    @Test
    fun test5_60MinutesOverdue() {
        val now = sdf.parse("2026-09-26 11:00")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Overdue by 60 minutes", display)
    }

    @Test
    fun test6_1Hour59MinutesOverdue_119Minutes() {
        val now = sdf.parse("2026-09-26 11:59")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        // Eliminates misleading truncation ("Overdue by 1 hour") -> reports exact 119 minutes
        assertEquals("Overdue by 119 minutes", display)
    }

    @Test
    fun test7_2HoursOverdue_120Minutes() {
        val now = sdf.parse("2026-09-26 12:00")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Overdue by 2 hours", display)
    }

    @Test
    fun test8_multipleHoursOverdue_5Hours() {
        val now = sdf.parse("2026-09-26 15:00")!!
        val display = getRelativeTimeSpan("2026-09-26", "10:00", now)
        assertEquals("Overdue by 5 hours", display)
    }

    @Test
    fun test9_aroundDayBoundary_yesterday() {
        val now = sdf.parse("2026-09-26 10:00")!!
        val display = getRelativeTimeSpan("2026-09-25", "10:00", now)
        assertEquals("Yesterday", display)
    }

    @Test
    fun test10_multipleDays_3DaysOverdue() {
        val now = sdf.parse("2026-09-26 10:00")!!
        val display = getRelativeTimeSpan("2026-09-23", "10:00", now)
        assertEquals("Overdue by 3 days", display)
    }

    @Test
    fun testFutureTime_tomorrowAndMultipleDays() {
        val now = sdf.parse("2026-09-26 10:00")!!
        assertEquals("Tomorrow", getRelativeTimeSpan("2026-09-27", "10:00", now))
        assertEquals("In 3 days", getRelativeTimeSpan("2026-09-29", "10:00", now))
    }
}
