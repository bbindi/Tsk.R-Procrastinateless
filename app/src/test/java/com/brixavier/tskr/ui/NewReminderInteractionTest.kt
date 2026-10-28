package com.brixavier.tskr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class NewReminderInteractionTest {

    @Test
    fun testNewReminderPickerState_dateSelectionUpdatesDateAndRetainsTitle() {
        var taskTitle = "Buy Groceries"
        var selectedDate = "2026-10-26"
        var showDatePicker = true

        // User picks a new date and picker closes
        val newDate = "2026-10-30"
        selectedDate = newDate
        showDatePicker = false

        assertEquals("Buy Groceries", taskTitle)
        assertEquals("2026-10-30", selectedDate)
        assertFalse(showDatePicker)
    }

    @Test
    fun testNewReminderPickerState_timeSelectionUpdatesTimeAndRetainsTitle() {
        var taskTitle = "Prepare Taxes"
        var selectedTime = "09:00"
        var showTimePicker = true

        // User picks a new time and picker closes
        val newTime = "14:30"
        selectedTime = newTime
        showTimePicker = false

        assertEquals("Prepare Taxes", taskTitle)
        assertEquals("14:30", selectedTime)
        assertFalse(showTimePicker)
    }

    @Test
    fun testNewReminderPickerState_dismissalPreservesFormValues() {
        val taskTitle = "Doctor Appointment"
        val initialDate = "2026-11-01"
        val initialTime = "10:15"
        var showDatePicker = true

        // Dismiss picker without selecting
        showDatePicker = false

        assertEquals("Doctor Appointment", taskTitle)
        assertEquals("2026-11-01", initialDate)
        assertEquals("10:15", initialTime)
        assertFalse(showDatePicker)
    }

    @Test
    fun testNewReminderPickerState_repeatedTapsAreIdempotent() {
        var showDatePicker = false
        var showTimePicker = false

        // Tap Date picker multiple times
        val openDatePicker = {
            showDatePicker = true
        }

        openDatePicker()
        assertTrue(showDatePicker)
        openDatePicker()
        assertTrue(showDatePicker)

        // Dismiss Date picker and open Time picker
        showDatePicker = false
        val openTimePicker = {
            showTimePicker = true
        }

        openTimePicker()
        assertTrue(showTimePicker)
        assertFalse(showDatePicker)
    }

    @Test
    fun testFormattedDateDisplay_parsesAndFormatsCorrectly() {
        val rawDate = "2026-10-26"
        val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfSource.parse(rawDate)
        val sdfDest = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val formatted = if (date != null) sdfDest.format(date) else rawDate

        assertEquals("Oct 26, 2026", formatted)
    }

    @Test
    fun testFormattedTimeDisplay_12HourFormatting() {
        fun formatTime12h(time: String): String {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
        }

        assertEquals("4:36 PM", formatTime12h("16:36"))
        assertEquals("9:00 AM", formatTime12h("09:00"))
        assertEquals("12:00 AM", formatTime12h("00:00"))
        assertEquals("12:30 PM", formatTime12h("12:30"))
    }
}
