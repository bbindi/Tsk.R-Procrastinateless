package com.brixavier.tskr.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class CalendarViewMode {
    MONTH,
    WEEK,
    DAY
}

/**
 * Derived presentation model for Calendar items that maps active master task templates
 * to their current pending occurrence scheduled date/time, retaining urgency, Plan, and recurrence metadata.
 */
data class CalendarItem(
    val id: String,
    val templateId: String,
    val occurrenceId: String?,
    val title: String,
    val date: String,
    val time: String,
    val priority: String,
    val notes: String,
    val subTasksRaw: String,
    val recurrenceType: String,
    val recurrenceInterval: Int,
    val planId: String?,
    val planName: String?,
    val planIcon: String?,
    val isActive: Boolean,
    val isOverdue: Boolean
) {
    fun isImportant(): Boolean = Reminder.isImportant(priority)
    fun isRecurring(): Boolean = recurrenceType != Reminder.RECURRENCE_NONE
}

/**
 * Data class representing a date cell in the Month View grid.
 */
data class MonthGridDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

/**
 * Data class representing a single day in the Week View strip.
 */
data class WeekDay(
    val date: LocalDate,
    val dayName: String,
    val dayNum: Int,
    val isToday: Boolean
)

/**
 * Data class representing a date range for Calendar views.
 */
data class CalendarDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

/**
 * Helper utilities for Calendar date range calculations, date navigation, month grid generation, week generation, and occurrence mapping.
 */
object CalendarUtils {

    fun getMonthRange(yearMonth: YearMonth): CalendarDateRange {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        return CalendarDateRange(start, end)
    }

    fun getWeekRange(date: LocalDate): CalendarDateRange {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return CalendarDateRange(start, end)
    }

    fun getDayRange(date: LocalDate): CalendarDateRange {
        return CalendarDateRange(date, date)
    }

    /**
     * Generates a list of 7 WeekDay objects for the Monday-to-Sunday week containing the specified date.
     */
    fun getWeekDays(date: LocalDate, today: LocalDate = LocalDate.now()): List<WeekDay> {
        val weekRange = getWeekRange(date)
        val formatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
        return (0..6).map { i ->
            val d = weekRange.startDate.plusDays(i.toLong())
            WeekDay(
                date = d,
                dayName = d.format(formatter),
                dayNum = d.dayOfMonth,
                isToday = d == today
            )
        }
    }

    /**
     * Generates a complete 7-column grid of days for the specified YearMonth,
     * including leading days from the previous month and trailing days from the next month.
     */
    fun generateMonthGrid(yearMonth: YearMonth, today: LocalDate = LocalDate.now()): List<MonthGridDay> {
        val firstOfMonth = yearMonth.atDay(1)
        val lastOfMonth = yearMonth.atEndOfMonth()

        val firstDayOfWeekVal = firstOfMonth.dayOfWeek.value // Monday = 1 ... Sunday = 7
        val leadingDaysCount = firstDayOfWeekVal - 1

        val gridDays = mutableListOf<MonthGridDay>()

        // Leading days from previous month
        for (i in leadingDaysCount downTo 1) {
            val date = firstOfMonth.minusDays(i.toLong())
            gridDays.add(MonthGridDay(date, isCurrentMonth = false, isToday = date == today))
        }

        // Days of current month
        for (dayNum in 1..lastOfMonth.dayOfMonth) {
            val date = yearMonth.atDay(dayNum)
            gridDays.add(MonthGridDay(date, isCurrentMonth = true, isToday = date == today))
        }

        // Trailing days to complete 7-column rows
        val remainder = gridDays.size % 7
        if (remainder != 0) {
            val trailingDaysCount = 7 - remainder
            for (i in 1..trailingDaysCount) {
                val date = lastOfMonth.plusDays(i.toLong())
                gridDays.add(MonthGridDay(date, isCurrentMonth = false, isToday = date == today))
            }
        }

        return gridDays
    }

    /**
     * Maps master templates and pending occurrences to CalendarItems.
     * For active recurring tasks, the scheduled date/time comes from the current pending occurrence.
     */
    fun mapToCalendarItems(
        reminders: List<Reminder>,
        pendingOccurrences: List<ReminderOccurrence>,
        plans: List<Plan>,
        nowStr: String
    ): List<CalendarItem> {
        val pendingOccMap = pendingOccurrences.associateBy { it.templateId }
        val planMap = plans.associateBy { it.id }

        return reminders.map { rem ->
            val pendingOcc = pendingOccMap[rem.id]
            val effectiveDate = if (rem.isActive && pendingOcc != null) pendingOcc.date else rem.date
            val effectiveTime = if (rem.isActive && pendingOcc != null) pendingOcc.time else rem.time
            val effectiveSubTasks = if (rem.isActive && pendingOcc != null) pendingOcc.subTasksRaw else rem.subTasksRaw
            val plan = rem.planId?.let { planMap[it] }

            val isOverdue = rem.isActive && "$effectiveDate $effectiveTime" < nowStr

            CalendarItem(
                id = pendingOcc?.id ?: rem.id,
                templateId = rem.id,
                occurrenceId = pendingOcc?.id,
                title = rem.title,
                date = effectiveDate,
                time = effectiveTime,
                priority = rem.priority,
                notes = rem.notes,
                subTasksRaw = effectiveSubTasks,
                recurrenceType = rem.recurrenceType,
                recurrenceInterval = rem.recurrenceInterval,
                planId = plan?.id,
                planName = plan?.name,
                planIcon = plan?.icon,
                isActive = rem.isActive,
                isOverdue = isOverdue
            )
        }
    }
}
