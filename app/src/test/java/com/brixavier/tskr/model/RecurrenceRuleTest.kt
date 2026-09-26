package com.brixavier.tskr.model

import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.ui.buildRecurrenceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceRuleTest {

    @Test
    fun getSerializedDaysOfWeek_sortsDaysInMondayToSundayOrder() {
        val days = setOf(
            RecurrenceDayOfWeek.FRIDAY,
            RecurrenceDayOfWeek.MONDAY,
            RecurrenceDayOfWeek.WEDNESDAY
        )
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 1,
            daysOfWeek = days
        )

        assertEquals("MON|WED|FRI", rule.getSerializedDaysOfWeek())
    }

    @Test
    fun parseDaysOfWeek_parsesPipeDelimitedStringCorrectly() {
        val serialized = "MON|WED|FRI"
        val parsed = RecurrenceRule.parseDaysOfWeek(serialized)

        assertEquals(3, parsed.size)
        assertTrue(parsed.contains(RecurrenceDayOfWeek.MONDAY))
        assertTrue(parsed.contains(RecurrenceDayOfWeek.WEDNESDAY))
        assertTrue(parsed.contains(RecurrenceDayOfWeek.FRIDAY))
        assertFalse(parsed.contains(RecurrenceDayOfWeek.TUESDAY))
    }

    @Test
    fun validate_none_returnsSuccess() {
        val rule = RecurrenceRule(type = RecurrenceType.NONE)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun validate_daily_validInterval_returnsSuccess() {
        val rule = RecurrenceRule(type = RecurrenceType.DAILY, interval = 2)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun validate_weekly_emptyDaysOfWeek_returnsError() {
        val rule = RecurrenceRule(type = RecurrenceType.WEEKLY, interval = 1, daysOfWeek = emptySet())
        val result = rule.validate()
        assertTrue(result is RecurrenceRule.Result.Error)
        assertEquals("At least one weekday must be selected for weekly recurrence.", (result as RecurrenceRule.Result.Error).message)
    }

    @Test
    fun validate_weekly_validDaysOfWeek_returnsSuccess() {
        val rule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY, RecurrenceDayOfWeek.WEDNESDAY)
        )
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun validate_monthlyOrdinal_missingOrdinal_returnsError() {
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = null,
            weekday = RecurrenceDayOfWeek.THURSDAY
        )
        val result = rule.validate()
        assertTrue(result is RecurrenceRule.Result.Error)
    }

    @Test
    fun validate_monthlyOrdinal_valid_returnsSuccess() {
        val rule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.LAST,
            weekday = RecurrenceDayOfWeek.THURSDAY
        )
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun validate_yearlyOrdinal_valid_returnsSuccess() {
        val rule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            yearlyMode = YearlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.FOURTH,
            weekday = RecurrenceDayOfWeek.WEDNESDAY,
            yearlyMonth = 9
        )
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun validate_invalidInterval_returnsError() {
        val rule = RecurrenceRule(type = RecurrenceType.DAILY, interval = 0)
        val result = rule.validate()
        assertTrue(result is RecurrenceRule.Result.Error)
    }

    @Test
    fun reminderToRecurrenceRule_mapsEntityFieldsAccurately() {
        val reminder = Reminder(
            title = "Pay Rent",
            date = "2026-10-01",
            time = "09:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = "MON|WED",
            recurrenceMonthlyMode = "DAY_OF_MONTH",
            recurrenceYearlyMode = "SPECIFIC_DATE"
        )

        val rule = reminder.toRecurrenceRule()

        assertEquals(RecurrenceType.WEEKLY, rule.type)
        assertEquals(2, rule.interval)
        assertEquals(2, rule.daysOfWeek.size)
        assertTrue(rule.daysOfWeek.contains(RecurrenceDayOfWeek.MONDAY))
        assertTrue(rule.daysOfWeek.contains(RecurrenceDayOfWeek.WEDNESDAY))
    }

    // --- 11 BOUNDARY VERIFICATION TESTS ---

    @Test
    fun boundaryTest_1_none_sentinelDefaultsDoNotInterfere() {
        val reminder = Reminder(
            title = "One-Time Task",
            date = "2026-09-25",
            time = "10:00",
            recurrenceType = "NONE"
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.NONE, rule.type)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_2_daily_sentinelDefaultsDoNotInterfere() {
        val reminder = Reminder(
            title = "Daily Workout",
            date = "2026-09-25",
            time = "07:00",
            recurrenceType = "DAILY",
            recurrenceInterval = 2
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.DAILY, rule.type)
        assertEquals(2, rule.interval)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_3_weeklySingleDay_legacyBlankDaysOfWeekInfersFromDate() {
        val reminder = Reminder(
            title = "Weekly Team Sync",
            date = "2026-09-21", // 2026-09-21 is Monday
            time = "10:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = "" // Legacy blank daysOfWeek
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.WEEKLY, rule.type)
        assertEquals(1, rule.daysOfWeek.size)
        assertTrue(rule.daysOfWeek.contains(RecurrenceDayOfWeek.MONDAY))
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_4_weeklyMultipleDays_mapsCorrectly() {
        val reminder = Reminder(
            title = "Study Session",
            date = "2026-09-21",
            time = "18:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = "MON|WED|FRI"
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.WEEKLY, rule.type)
        assertEquals(3, rule.daysOfWeek.size)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_5_monthlyDayOfMonth_mapsCorrectly() {
        val reminder = Reminder(
            title = "Pay Credit Card",
            date = "2026-09-15",
            time = "09:00",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceMonthlyMode = "DAY_OF_MONTH"
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.MONTHLY, rule.type)
        assertEquals(MonthlyMode.DAY_OF_MONTH, rule.monthlyMode)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_6_monthlyOrdinalWeekday_mapsCorrectly() {
        val reminder = Reminder(
            title = "Board Meeting",
            date = "2026-09-24",
            time = "14:00",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceMonthlyMode = "ORDINAL_WEEKDAY",
            recurrenceOrdinal = "LAST",
            recurrenceWeekday = "THURSDAY"
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.MONTHLY, rule.type)
        assertEquals(MonthlyMode.ORDINAL_WEEKDAY, rule.monthlyMode)
        assertEquals(RecurrenceOrdinal.LAST, rule.ordinal)
        assertEquals(RecurrenceDayOfWeek.THURSDAY, rule.weekday)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_7_yearlySpecificDate_infersYearlyMonthFromDateWhenDefaultZero() {
        val reminder = Reminder(
            title = "Anniversary",
            date = "2026-09-23", // Month = 9
            time = "12:00",
            recurrenceType = "YEARLY",
            recurrenceInterval = 1,
            recurrenceYearlyMode = "SPECIFIC_DATE",
            recurrenceYearlyMonth = 0 // Room sentinel default
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.YEARLY, rule.type)
        assertEquals(YearlyMode.SPECIFIC_DATE, rule.yearlyMode)
        assertEquals(9, rule.yearlyMonth)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_8_yearlyOrdinalWeekday_mapsCorrectly() {
        val reminder = Reminder(
            title = "Thanksgiving",
            date = "2026-11-26",
            time = "11:00",
            recurrenceType = "YEARLY",
            recurrenceInterval = 1,
            recurrenceYearlyMode = "ORDINAL_WEEKDAY",
            recurrenceOrdinal = "FOURTH",
            recurrenceWeekday = "THURSDAY",
            recurrenceYearlyMonth = 11
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.YEARLY, rule.type)
        assertEquals(YearlyMode.ORDINAL_WEEKDAY, rule.yearlyMode)
        assertEquals(RecurrenceOrdinal.FOURTH, rule.ordinal)
        assertEquals(RecurrenceDayOfWeek.THURSDAY, rule.weekday)
        assertEquals(11, rule.yearlyMonth)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_9_legacyCustomDays_mapsToDailyWithInterval() {
        val reminder = Reminder(
            title = "Water Plants",
            date = "2026-09-25",
            time = "08:00",
            recurrenceType = "CUSTOM_DAYS",
            recurrenceInterval = 3
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.DAILY, rule.type)
        assertEquals(3, rule.interval)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_10_february29YearlyRule_isRepresentableAndValid() {
        val reminder = Reminder(
            title = "Leap Day Celebration",
            date = "2028-02-29",
            time = "12:00",
            recurrenceType = "YEARLY",
            recurrenceInterval = 1,
            recurrenceYearlyMonth = 2
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.YEARLY, rule.type)
        assertEquals(2, rule.yearlyMonth)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun boundaryTest_11_monthlyDay31Rule_isRepresentableAndValid() {
        val reminder = Reminder(
            title = "End of Month Review",
            date = "2026-01-31",
            time = "17:00",
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceMonthlyMode = "DAY_OF_MONTH"
        )
        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.MONTHLY, rule.type)
        assertEquals(MonthlyMode.DAY_OF_MONTH, rule.monthlyMode)
        assertTrue(rule.validate() is RecurrenceRule.Result.Success)
    }

    @Test
    fun buildRecurrenceSummary_generatesHumanReadableStringsForRules() {
        val dailyRule = RecurrenceRule(type = RecurrenceType.DAILY, interval = 2)
        assertEquals("Repeats every 2 days", buildRecurrenceSummary(dailyRule))

        val weeklyRule = RecurrenceRule(
            type = RecurrenceType.WEEKLY,
            interval = 2,
            daysOfWeek = setOf(RecurrenceDayOfWeek.MONDAY, RecurrenceDayOfWeek.WEDNESDAY)
        )
        assertEquals("Repeats every 2 weeks on MON, WED", buildRecurrenceSummary(weeklyRule))

        val monthlyOrdinalRule = RecurrenceRule(
            type = RecurrenceType.MONTHLY,
            monthlyMode = MonthlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.LAST,
            weekday = RecurrenceDayOfWeek.THURSDAY
        )
        assertEquals("Repeats monthly on the Last Thursday",
            buildRecurrenceSummary(monthlyOrdinalRule)
        )

        val yearlyOrdinalRule = RecurrenceRule(
            type = RecurrenceType.YEARLY,
            yearlyMode = YearlyMode.ORDINAL_WEEKDAY,
            ordinal = RecurrenceOrdinal.FOURTH,
            weekday = RecurrenceDayOfWeek.WEDNESDAY,
            yearlyMonth = 9
        )
        assertEquals("Repeats yearly on the Fourth Wednesday of September",
            com.brixavier.tskr.ui.buildRecurrenceSummary(yearlyOrdinalRule)
        )
    }

    // --- EDIT ROUND-TRIP PERSISTENCE TESTS ---

    @Test
    fun editPersistence_sequence1_weeklyToMonthlyLastThursday_preservesAllFieldsAndAnchorDate() {
        // 1. Initial Weekly Task: Start Sep 29, 2026, Every 2 weeks, Mon + Wed
        val initialReminder = Reminder(
            id = "rem-1",
            title = "Team Planning",
            date = "2026-09-29",
            time = "10:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = "MON|WED"
        )

        val initialRule = initialReminder.toRecurrenceRule()
        assertEquals(RecurrenceType.WEEKLY, initialRule.type)
        assertEquals(2, initialRule.interval)
        assertEquals(2, initialRule.daysOfWeek.size)

        // 2. User edits task to: Monthly, Every 3 months, Last Thursday
        val editedReminder = initialReminder.copy(
            recurrenceType = "MONTHLY",
            recurrenceInterval = 3,
            recurrenceMonthlyMode = "ORDINAL_WEEKDAY",
            recurrenceOrdinal = "LAST",
            recurrenceWeekday = "THURSDAY"
        )

        val reloadedRule = editedReminder.toRecurrenceRule()
        assertEquals(RecurrenceType.MONTHLY, reloadedRule.type)
        assertEquals(3, reloadedRule.interval)
        assertEquals(MonthlyMode.ORDINAL_WEEKDAY, reloadedRule.monthlyMode)
        assertEquals(RecurrenceOrdinal.LAST, reloadedRule.ordinal)
        assertEquals(RecurrenceDayOfWeek.THURSDAY, reloadedRule.weekday)

        // Selected start date is preserved permanently
        assertEquals("2026-09-29", editedReminder.date)
    }

    @Test
    fun editPersistence_sequence2_yearlyFourthWednesdayOfSeptember_roundTripIsLossless() {
        val reminder = Reminder(
            id = "rem-2",
            title = "Annual Review",
            date = "2026-09-29",
            time = "14:00",
            recurrenceType = "YEARLY",
            recurrenceInterval = 2,
            recurrenceYearlyMode = "ORDINAL_WEEKDAY",
            recurrenceOrdinal = "FOURTH",
            recurrenceWeekday = "WEDNESDAY",
            recurrenceYearlyMonth = 9
        )

        val rule = reminder.toRecurrenceRule()
        assertEquals(RecurrenceType.YEARLY, rule.type)
        assertEquals(2, rule.interval)
        assertEquals(YearlyMode.ORDINAL_WEEKDAY, rule.yearlyMode)
        assertEquals(RecurrenceOrdinal.FOURTH, rule.ordinal)
        assertEquals(RecurrenceDayOfWeek.WEDNESDAY, rule.weekday)
        assertEquals(9, rule.yearlyMonth)

        assertEquals("Repeats every 2 years on the Fourth Wednesday of September", buildRecurrenceSummary(rule))
        assertEquals("2026-09-29", reminder.date)
    }

    // --- A1D ATOMIC EDIT RECONCILIATION TESTS ---

    @Test
    fun editReconciliation_weeklyToMonthlyLastThursday_recalculatesPendingOccurrenceInPlace() {
        val initialReminder = Reminder(
            id = "rem-a1d-1",
            title = "Project Sync",
            date = "2026-09-29",
            time = "10:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = "MON|WED"
        )

        val pendingOcc = ReminderOccurrence(
            id = "occ-pending-1",
            templateId = initialReminder.id,
            title = initialReminder.title,
            date = "2026-09-30",
            time = initialReminder.time,
            status = ReminderOccurrence.STATUS_PENDING,
            snoozedUntilMs = System.currentTimeMillis() + 300000 // Snoozed state
        )

        // Edit task to: Monthly, Every 3 months, Last Thursday
        val editedReminder = initialReminder.copy(
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
        val reconciledOcc = pendingOcc.copy(
            title = editedReminder.title,
            priority = editedReminder.priority,
            date = newDate!!,
            time = editedReminder.time,
            snoozedUntilMs = null // Stale snooze state cleared
        )

        // Occurrence ID preserved
        assertEquals("occ-pending-1", reconciledOcc.id)
        assertEquals("2026-12-31", reconciledOcc.date)
        assertEquals("10:00", reconciledOcc.time)
        assertEquals(null, reconciledOcc.snoozedUntilMs)
    }

    @Test
    fun editReconciliation_recurringToNone_preservesPendingOccurrenceAsOneTimeTask() {
        val recurringReminder = Reminder(
            id = "rem-a1d-2",
            title = "Weekly Call",
            date = "2026-09-29",
            time = "14:00",
            recurrenceType = "WEEKLY",
            recurrenceInterval = 1,
            recurrenceDaysOfWeek = "MON"
        )

        val pendingOcc = ReminderOccurrence(
            id = "occ-pending-2",
            templateId = recurringReminder.id,
            title = recurringReminder.title,
            date = "2026-10-05",
            time = "14:00"
        )

        val disabledReminder = recurringReminder.copy(recurrenceType = "NONE")

        // Non-recurring edit uses selected date
        val reconciledOcc = pendingOcc.copy(
            title = disabledReminder.title,
            date = disabledReminder.date,
            time = disabledReminder.time
        )

        assertEquals("occ-pending-2", reconciledOcc.id)
        assertEquals("2026-09-29", reconciledOcc.date)
        assertEquals("14:00", reconciledOcc.time)
    }
}
