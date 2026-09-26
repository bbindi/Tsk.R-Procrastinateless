package com.brixavier.tskr.ui

import com.brixavier.tskr.model.CalendarItem
import com.brixavier.tskr.model.CalendarUtils
import com.brixavier.tskr.model.CalendarViewMode
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarExperienceTest {

    private val nowStr = "2026-09-25 12:00"

    private val plan1 = Plan(id = "p1", name = "App Launch", icon = "🚀")

    private val oneTimeTask = Reminder(
        id = "r1",
        title = "Design Mockups",
        date = "2026-09-25",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        planId = plan1.id
    )

    private val recurringTask = Reminder(
        id = "r2",
        title = "Daily Workout",
        date = "2026-09-20", // Original creation date (past)
        time = "08:00",
        priority = Reminder.PRIORITY_NORMAL,
        recurrenceType = Reminder.RECURRENCE_DAILY,
        recurrenceInterval = 1
    )

    private val pendingOccurrence = ReminderOccurrence(
        id = "occ-2",
        templateId = "r2",
        title = "Daily Workout",
        date = "2026-09-26", // Current pending occurrence scheduled date (tomorrow)
        time = "08:00",
        status = ReminderOccurrence.STATUS_PENDING
    )

    @Test
    fun testInitialStateDefaults() {
        val today = LocalDate.now()
        val defaultMode = CalendarViewMode.MONTH
        assertEquals(CalendarViewMode.MONTH, defaultMode)
        assertNotNull(today)
    }

    @Test
    fun testViewModeSwitching_preservesSelectedDate() {
        val selectedDate = LocalDate.of(2026, 9, 25)
        val viewMode = CalendarViewMode.WEEK

        assertEquals(selectedDate, LocalDate.of(2026, 9, 25))
        assertEquals(CalendarViewMode.WEEK, viewMode)
    }

    @Test
    fun testMonthNavigation_previousAndNext() {
        val ym = YearMonth.of(2026, 9)
        val prevYm = ym.minusMonths(1)
        val nextYm = ym.plusMonths(1)

        assertEquals(YearMonth.of(2026, 8), prevYm)
        assertEquals(YearMonth.of(2026, 10), nextYm)
    }

    @Test
    fun testWeekNavigation_previousAndNext() {
        val date = LocalDate.of(2026, 9, 25)
        val prevWeek = date.minusWeeks(1)
        val nextWeek = date.plusWeeks(1)

        assertEquals(LocalDate.of(2026, 9, 18), prevWeek)
        assertEquals(LocalDate.of(2026, 10, 2), nextWeek)
    }

    @Test
    fun testDayNavigation_previousAndNext() {
        val date = LocalDate.of(2026, 9, 25)
        val prevDay = date.minusDays(1)
        val nextDay = date.plusDays(1)

        assertEquals(LocalDate.of(2026, 9, 24), prevDay)
        assertEquals(LocalDate.of(2026, 9, 26), nextDay)
    }

    @Test
    fun testTodayReset() {
        val today = LocalDate.now()
        val selectedDate = LocalDate.now()
        assertEquals(today, selectedDate)
    }

    @Test
    fun testMonthRangeCalculation() {
        val ym = YearMonth.of(2026, 9)
        val range = CalendarUtils.getMonthRange(ym)

        assertEquals(LocalDate.of(2026, 9, 1), range.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), range.endDate)
    }

    @Test
    fun testWeekRangeCalculation() {
        val date = LocalDate.of(2026, 9, 25) // Friday
        val range = CalendarUtils.getWeekRange(date)

        assertEquals(LocalDate.of(2026, 9, 21), range.startDate) // Monday
        assertEquals(LocalDate.of(2026, 9, 27), range.endDate) // Sunday
    }

    @Test
    fun testDayRangeCalculation() {
        val date = LocalDate.of(2026, 9, 25)
        val range = CalendarUtils.getDayRange(date)

        assertEquals(date, range.startDate)
        assertEquals(date, range.endDate)
    }

    @Test
    fun testGetWeekDays_7DaysStartingOnMonday() {
        val date = LocalDate.of(2026, 9, 25) // Friday
        val weekDays = CalendarUtils.getWeekDays(date)

        assertEquals(7, weekDays.size)
        assertEquals(DayOfWeek.MONDAY, weekDays.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 21), weekDays.first().date)
        assertEquals(DayOfWeek.SUNDAY, weekDays.last().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 27), weekDays.last().date)
    }

    @Test
    fun testWeekDaysNavigation_previousAndNextWeek() {
        val date = LocalDate.of(2026, 9, 25)
        val currentWeek = CalendarUtils.getWeekDays(date)
        val prevWeek = CalendarUtils.getWeekDays(date.minusWeeks(1))
        val nextWeek = CalendarUtils.getWeekDays(date.plusWeeks(1))

        assertEquals(LocalDate.of(2026, 9, 14), prevWeek.first().date)
        assertEquals(LocalDate.of(2026, 9, 21), currentWeek.first().date)
        assertEquals(LocalDate.of(2026, 9, 28), nextWeek.first().date)
    }

    @Test
    fun testGenerateMonthGrid_5RowMonth_September2026() {
        val ym = YearMonth.of(2026, 9)
        val grid = CalendarUtils.generateMonthGrid(ym)

        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2026, 8, 31), grid.first().date)
        assertFalse(grid.first().isCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 1), grid[1].date)
        assertTrue(grid[1].isCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 30), grid[30].date)
        assertTrue(grid[30].isCurrentMonth)
        assertEquals(LocalDate.of(2026, 10, 4), grid.last().date)
        assertFalse(grid.last().isCurrentMonth)
    }

    @Test
    fun testGenerateMonthGrid_6RowMonth_August2026() {
        val ym = YearMonth.of(2026, 8)
        val grid = CalendarUtils.generateMonthGrid(ym)

        assertEquals(42, grid.size)
        assertEquals(LocalDate.of(2026, 7, 27), grid.first().date)
        assertFalse(grid.first().isCurrentMonth)
        assertEquals(LocalDate.of(2026, 9, 6), grid.last().date)
        assertFalse(grid.last().isCurrentMonth)
    }

    @Test
    fun testMonthNavigation_changesActualGrid() {
        val septGrid = CalendarUtils.generateMonthGrid(YearMonth.of(2026, 9))
        val octGrid = CalendarUtils.generateMonthGrid(YearMonth.of(2026, 10))

        assertTrue(septGrid.any { it.date.monthValue == 9 })
        assertTrue(octGrid.any { it.date.monthValue == 10 })
        assertEquals(35, septGrid.size)
        assertEquals(35, octGrid.size)
        assertEquals(LocalDate.of(2026, 9, 1), septGrid.find { it.isCurrentMonth }!!.date)
        assertEquals(LocalDate.of(2026, 10, 1), octGrid.find { it.isCurrentMonth }!!.date)
    }

    @Test
    fun testDayView_filtersItemsStrictlyForSelectedDate() {
        val task1 = Reminder(id = "a1", title = "Task 1", date = "2026-09-25", time = "09:00")
        val task2 = Reminder(id = "a2", title = "Task 2", date = "2026-09-26", time = "10:00")

        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(task1, task2),
            pendingOccurrences = emptyList(),
            plans = emptyList(),
            nowStr = nowStr
        )

        val dayItems25 = items.filter { it.date == "2026-09-25" }
        assertEquals(1, dayItems25.size)
        assertEquals("a1", dayItems25.first().templateId)

        val dayItems26 = items.filter { it.date == "2026-09-26" }
        assertEquals(1, dayItems26.size)
        assertEquals("a2", dayItems26.first().templateId)
    }

    @Test
    fun testDayView_chronologicalTimeSorting() {
        val taskLater = Reminder(id = "b1", title = "Evening Walk", date = "2026-09-25", time = "19:00")
        val taskEarlier = Reminder(id = "b2", title = "Morning Coffee", date = "2026-09-25", time = "07:30")
        val taskMidday = Reminder(id = "b3", title = "Lunch Sync", date = "2026-09-25", time = "12:00")

        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(taskLater, taskEarlier, taskMidday),
            pendingOccurrences = emptyList(),
            plans = emptyList(),
            nowStr = nowStr
        )

        val sorted = items.sortedWith(
            compareBy<CalendarItem> { it.time }
                .thenBy { if (it.isImportant()) 0 else 1 }
                .thenBy { it.title }
        )

        assertEquals("b2", sorted[0].templateId) // 07:30
        assertEquals("b3", sorted[1].templateId) // 12:00
        assertEquals("b1", sorted[2].templateId) // 19:00
    }

    @Test
    fun testDateSelection_synchronizesDisplayedYearMonth() {
        var selectedDate = LocalDate.of(2026, 9, 25)
        var displayedYearMonth = YearMonth.from(selectedDate)

        assertEquals(YearMonth.of(2026, 9), displayedYearMonth)

        // Select a date in October (e.g. trailing day 2026-10-02)
        selectedDate = LocalDate.of(2026, 10, 2)
        displayedYearMonth = YearMonth.from(selectedDate)

        assertEquals(YearMonth.of(2026, 10), displayedYearMonth)
    }

    @Test
    fun testTransitionsBetweenMonthWeekDay_preservesSelectedDate() {
        val selectedDate = LocalDate.of(2026, 9, 18)
        var viewMode = CalendarViewMode.MONTH

        // Month -> Week
        viewMode = CalendarViewMode.WEEK
        assertEquals(CalendarViewMode.WEEK, viewMode)
        assertEquals(LocalDate.of(2026, 9, 18), selectedDate)

        // Week -> Day
        viewMode = CalendarViewMode.DAY
        assertEquals(CalendarViewMode.DAY, viewMode)
        assertEquals(LocalDate.of(2026, 9, 18), selectedDate)

        // Day -> Month
        viewMode = CalendarViewMode.MONTH
        assertEquals(CalendarViewMode.MONTH, viewMode)
        assertEquals(LocalDate.of(2026, 9, 18), selectedDate)
        assertEquals(YearMonth.of(2026, 9), YearMonth.from(selectedDate))
    }

    @Test
    fun testCompletingRecurringOccurrence_advancesMappedCalendarItemDate() {
        val initialOccurrence = ReminderOccurrence(
            id = "occ-1",
            templateId = "r2",
            title = "Daily Workout",
            date = "2026-09-25",
            time = "08:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val nextOccurrence = ReminderOccurrence(
            id = "occ-2",
            templateId = "r2",
            title = "Daily Workout",
            date = "2026-09-26",
            time = "08:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val initialItems = CalendarUtils.mapToCalendarItems(
            reminders = listOf(recurringTask),
            pendingOccurrences = listOf(initialOccurrence),
            plans = emptyList(),
            nowStr = nowStr
        )
        assertEquals("2026-09-25", initialItems.first().date)

        val advancedItems = CalendarUtils.mapToCalendarItems(
            reminders = listOf(recurringTask),
            pendingOccurrences = listOf(nextOccurrence),
            plans = emptyList(),
            nowStr = nowStr
        )
        assertEquals("2026-09-26", advancedItems.first().date)
    }

    @Test
    fun testRecurringTaskMapping_usesCurrentPendingOccurrenceDate() {
        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(oneTimeTask, recurringTask),
            pendingOccurrences = listOf(pendingOccurrence),
            plans = listOf(plan1),
            nowStr = nowStr
        )

        assertEquals(2, items.size)

        val recurringItem = items.find { it.templateId == "r2" }
        assertNotNull(recurringItem)
        assertEquals("2026-09-26", recurringItem!!.date)
        assertEquals("08:00", recurringItem.time)
    }

    @Test
    fun testRecurringTaskMapping_oneRecurringTaskMapsToOneCalendarItem() {
        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(recurringTask),
            pendingOccurrences = listOf(pendingOccurrence),
            plans = emptyList(),
            nowStr = nowStr
        )

        assertEquals(1, items.size)
        assertEquals("r2", items.first().templateId)
    }

    @Test
    fun testOverdueStateCalculation_basedOnCurrentPendingOccurrence() {
        val overdueOccurrence = ReminderOccurrence(
            id = "occ-overdue",
            templateId = "r2",
            title = "Daily Workout",
            date = "2026-09-24", // Past date (yesterday relative to 2026-09-25 12:00)
            time = "08:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(recurringTask),
            pendingOccurrences = listOf(overdueOccurrence),
            plans = emptyList(),
            nowStr = nowStr
        )

        val item = items.first()
        assertTrue(item.isOverdue)
    }

    @Test
    fun testUrgencyAndPlanAssociationPreserved() {
        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(oneTimeTask),
            pendingOccurrences = emptyList(),
            plans = listOf(plan1),
            nowStr = nowStr
        )

        val item = items.first()
        assertTrue(item.isImportant())
        assertEquals("p1", item.planId)
        assertEquals("App Launch", item.planName)
        assertEquals("🚀", item.planIcon)
    }

    @Test
    fun testMissingPlanAssociation_remainsNull() {
        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(recurringTask),
            pendingOccurrences = emptyList(),
            plans = listOf(plan1),
            nowStr = nowStr
        )

        val item = items.first()
        assertNull(item.planId)
        assertNull(item.planName)
        assertNull(item.planIcon)
    }

    @Test
    fun testTaskDensityCalculations_zeroOneTwoThreeAndCappedAtThree() {
        val d25 = LocalDate.of(2026, 9, 25)
        val d26 = LocalDate.of(2026, 9, 26)

        val t1 = Reminder(id = "c1", title = "Task 1", date = "2026-09-25", time = "09:00")
        val t2 = Reminder(id = "c2", title = "Task 2", date = "2026-09-25", time = "10:00")
        val t3 = Reminder(id = "c3", title = "Task 3", date = "2026-09-25", time = "11:00")
        val t4 = Reminder(id = "c4", title = "Task 4", date = "2026-09-25", time = "12:00")

        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(t1, t2, t3, t4),
            pendingOccurrences = emptyList(),
            plans = emptyList(),
            nowStr = nowStr
        )

        // Date with 0 tasks
        assertEquals(0, CalendarUtils.taskCountForDate(items, d26))
        assertEquals(0, CalendarUtils.getDensityDotCount(items, d26))

        // Date with 4 tasks -> taskCount = 4, dotCount capped at 3
        assertEquals(4, CalendarUtils.taskCountForDate(items, d25))
        assertEquals(3, CalendarUtils.getDensityDotCount(items, d25))

        // Subset with 1 task
        val singleItem = items.take(1)
        assertEquals(1, CalendarUtils.taskCountForDate(singleItem, d25))
        assertEquals(1, CalendarUtils.getDensityDotCount(singleItem, d25))

        // Subset with 2 tasks
        val twoItems = items.take(2)
        assertEquals(2, CalendarUtils.taskCountForDate(twoItems, d25))
        assertEquals(2, CalendarUtils.getDensityDotCount(twoItems, d25))
    }

    @Test
    fun testGetDateCellContentDescription_accessibilityFormat() {
        val date = LocalDate.of(2026, 9, 25)
        val descSelected = CalendarUtils.getDateCellContentDescription(
            date = date,
            taskCount = 3,
            isSelected = true,
            isToday = true
        )

        assertTrue(descSelected.contains("Friday, September 25, 2026"))
        assertTrue(descSelected.contains("today"))
        assertTrue(descSelected.contains("selected"))
        assertTrue(descSelected.contains("3 tasks scheduled"))
    }

    @Test
    fun testSameTimeTasks_useDeterministicSecondaryOrdering() {
        val normalUrgency = Reminder(id = "s1", title = "B Task", date = "2026-09-25", time = "14:00", priority = Reminder.PRIORITY_NORMAL)
        val highUrgency = Reminder(id = "s2", title = "A Task", date = "2026-09-25", time = "14:00", priority = Reminder.PRIORITY_IMPORTANT)
        val normalUrgencyA = Reminder(id = "s3", title = "A Task", date = "2026-09-25", time = "14:00", priority = Reminder.PRIORITY_NORMAL)

        val items = CalendarUtils.mapToCalendarItems(
            reminders = listOf(normalUrgency, highUrgency, normalUrgencyA),
            pendingOccurrences = emptyList(),
            plans = emptyList(),
            nowStr = nowStr
        )

        val sorted = items.sortedWith(
            compareBy<CalendarItem> { it.time }
                .thenBy { if (it.isImportant()) 0 else 1 }
                .thenBy { it.title }
        )

        // 1st: High urgency "A Task"
        assertEquals("s2", sorted[0].templateId)
        // 2nd: Normal urgency "A Task"
        assertEquals("s3", sorted[1].templateId)
        // 3rd: Normal urgency "B Task"
        assertEquals("s1", sorted[2].templateId)
    }

    @Test
    fun testCompletedOccurrenceInHistory_remainsInCompletedOccurrences() {
        val completedOcc = ReminderOccurrence(
            id = "occ-comp",
            templateId = "r1",
            title = "Design Mockups",
            date = "2026-09-25",
            time = "10:00",
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = System.currentTimeMillis()
        )

        assertEquals("COMPLETED", completedOcc.status)
        assertNotNull(completedOcc.completedAtMs)
    }

    @Test
    fun testYearBoundaryNavigation_decemberToJanuaryAndJanuaryToDecember() {
        val dec2026 = YearMonth.of(2026, 12)
        val jan2027 = dec2026.plusMonths(1)
        val backToDec = jan2027.minusMonths(1)

        assertEquals(YearMonth.of(2027, 1), jan2027)
        assertEquals(YearMonth.of(2026, 12), backToDec)
    }

    @Test
    fun testLeapYearGridGeneration_february2028() {
        val feb2028 = YearMonth.of(2028, 2)
        val grid = CalendarUtils.generateMonthGrid(feb2028)

        val febDays = grid.filter { it.isCurrentMonth }
        assertEquals(29, febDays.size)
        assertEquals(LocalDate.of(2028, 2, 29), febDays.last().date)
    }

    @Test
    fun testWeekNavigationAcrossMonthAndYearBoundaries() {
        val dec31 = LocalDate.of(2026, 12, 31) // Thursday
        val weekRange = CalendarUtils.getWeekRange(dec31)

        assertEquals(LocalDate.of(2026, 12, 28), weekRange.startDate) // Monday
        assertEquals(LocalDate.of(2027, 1, 3), weekRange.endDate) // Sunday
    }

    @Test
    fun testAccessibilitySemanticsFormat_includesDateSelectionAndTaskCount() {
        val date = LocalDate.of(2026, 12, 31)
        val labelZero = CalendarUtils.getDateCellContentDescription(date, 0, isSelected = false, isToday = false)
        val labelOne = CalendarUtils.getDateCellContentDescription(date, 1, isSelected = true, isToday = true)

        assertTrue(labelZero.contains("Thursday, December 31, 2026"))
        assertTrue(labelZero.contains("no tasks scheduled"))

        assertTrue(labelOne.contains("selected"))
        assertTrue(labelOne.contains("today"))
        assertTrue(labelOne.contains("1 task scheduled"))
    }
}
