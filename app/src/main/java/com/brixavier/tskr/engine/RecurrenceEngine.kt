package com.brixavier.tskr.engine

import com.brixavier.tskr.model.MonthlyMode
import com.brixavier.tskr.model.RecurrenceDayOfWeek
import com.brixavier.tskr.model.RecurrenceOrdinal
import com.brixavier.tskr.model.RecurrenceRule
import com.brixavier.tskr.model.RecurrenceType
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.YearlyMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Deterministic, pure Kotlin calculation engine for recurring reminder schedules (Recurrence 2.0).
 *
 * Contains zero Android UI, Room, AlarmManager, or notification dependencies.
 * Guarantees strict schedule anchoring and single-occurrence (no backlog) output.
 */
object RecurrenceEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    /**
     * Backward-compatible calculation operating on legacy recurrenceType string constants.
     */
    fun calculateNextDueDate(
        baseDateStr: String,
        recurrenceType: String,
        recurrenceInterval: Int,
        afterDateStr: String? = null
    ): String? {
        val typeEnum = try {
            RecurrenceType.valueOf(recurrenceType)
        } catch (_: Exception) {
            if (recurrenceType == Reminder.RECURRENCE_CUSTOM_DAYS) RecurrenceType.DAILY else RecurrenceType.NONE
        }

        val safeInterval = if (recurrenceInterval < 1) 1 else recurrenceInterval

        val baseDate = parseDateOrNull(baseDateStr)
        val daysOfWeek = if (typeEnum == RecurrenceType.WEEKLY && baseDate != null) {
            setOf(toRecurrenceDayOfWeek(baseDate.dayOfWeek))
        } else {
            emptySet()
        }

        val rule = RecurrenceRule(
            type = typeEnum,
            interval = safeInterval,
            daysOfWeek = daysOfWeek
        )

        return calculateNextDueDate(baseDateStr, rule, afterDateStr)
    }

    /**
     * Calculates the next scheduled recurrence date string (YYYY-MM-DD) strictly AFTER [afterDateStr].
     */
    fun calculateNextDueDate(
        baseDateStr: String,
        rule: RecurrenceRule,
        afterDateStr: String? = null
    ): String? {
        if (!rule.isRecurring()) return null
        if (rule.validate() !is RecurrenceRule.Result.Success) return null

        val baseDate = parseDateOrNull(baseDateStr) ?: return null
        val afterDate = if (!afterDateStr.isNullOrBlank()) {
            parseDateOrNull(afterDateStr) ?: baseDate
        } else {
            baseDate
        }

        val nextLocalDate = calculateNextLocalDate(baseDate, rule, afterDate)
        return nextLocalDate?.format(dateFormatter)
    }

    /**
     * Calculates the first scheduled occurrence date string (YYYY-MM-DD) on or after [searchFromDateStr].
     */
    fun calculateFirstDueDate(
        baseDateStr: String,
        rule: RecurrenceRule,
        searchFromDateStr: String? = null
    ): String? {
        if (!rule.isRecurring()) return null
        if (rule.validate() !is RecurrenceRule.Result.Success) return null

        val baseDate = parseDateOrNull(baseDateStr) ?: return null
        val searchFromDate = if (!searchFromDateStr.isNullOrBlank()) {
            parseDateOrNull(searchFromDateStr) ?: baseDate
        } else {
            baseDate
        }

        val effectiveSearchFrom = if (searchFromDate.isBefore(baseDate)) baseDate else searchFromDate

        val firstLocalDate = calculateFirstLocalDate(baseDate, rule, effectiveSearchFrom)
        return firstLocalDate?.format(dateFormatter)
    }

    /**
     * Core calculation logic returning a LocalDate strictly AFTER [afterDate].
     */
    fun calculateNextLocalDate(
        baseDate: LocalDate,
        rule: RecurrenceRule,
        afterDate: LocalDate
    ): LocalDate? {
        if (!rule.isRecurring()) return null
        val interval = if (rule.interval < 1) 1 else rule.interval

        return when (rule.type) {
            RecurrenceType.NONE -> null

            RecurrenceType.DAILY -> {
                var candidate = baseDate
                while (!candidate.isAfter(afterDate)) {
                    candidate = candidate.plusDays(interval.toLong())
                }
                candidate
            }

            RecurrenceType.WEEKLY -> {
                val selectedDays = if (rule.daysOfWeek.isEmpty()) {
                    setOf(toRecurrenceDayOfWeek(baseDate.dayOfWeek))
                } else {
                    rule.daysOfWeek
                }

                // Monday of anchor week
                val anchorMonday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                // Sort selected weekdays Mon -> Sun
                val sortedDays = RecurrenceDayOfWeek.entries.filter { selectedDays.contains(it) }

                var weekIndex = 0L
                var resultDate: LocalDate? = null
                while (resultDate == null) {
                    val candidateWeekMonday = anchorMonday.plusWeeks(weekIndex * interval)
                    for (dayEnum in sortedDays) {
                        val targetDay = toJavaDayOfWeek(dayEnum)
                        val candidateDate = candidateWeekMonday.with(TemporalAdjusters.nextOrSame(targetDay))
                        if (candidateDate.isAfter(afterDate)) {
                            resultDate = candidateDate
                            break
                        }
                    }
                    weekIndex++
                }
                resultDate
            }

            RecurrenceType.MONTHLY -> {
                if (rule.monthlyMode == MonthlyMode.ORDINAL_WEEKDAY && rule.ordinal != null && rule.weekday != null) {
                    var monthIndex = 0L
                    val anchorMonth = YearMonth.from(baseDate)
                    var resultDate: LocalDate? = null
                    while (resultDate == null) {
                        val currentMonth = anchorMonth.plusMonths(monthIndex * interval)
                        val candidateDate = calculateOrdinalWeekday(currentMonth, rule.ordinal, rule.weekday)
                        if (candidateDate != null && candidateDate.isAfter(afterDate)) {
                            resultDate = candidateDate
                        }
                        monthIndex++
                    }
                    resultDate
                } else {
                    // Day of Month Mode with month-end clamping
                    var monthIndex = 0L
                    val anchorMonth = YearMonth.from(baseDate)
                    val anchorDay = baseDate.dayOfMonth
                    var resultDate: LocalDate? = null
                    while (resultDate == null) {
                        val currentMonth = anchorMonth.plusMonths(monthIndex * interval)
                        val clampedDay = minOf(anchorDay, currentMonth.lengthOfMonth())
                        val candidateDate = currentMonth.atDay(clampedDay)
                        if (candidateDate.isAfter(afterDate)) {
                            resultDate = candidateDate
                        }
                        monthIndex++
                    }
                    resultDate
                }
            }

            RecurrenceType.YEARLY -> {
                if (rule.yearlyMode == YearlyMode.ORDINAL_WEEKDAY && rule.ordinal != null && rule.weekday != null && rule.yearlyMonth in 1..12) {
                    var yearIndex = 0L
                    val anchorYear = baseDate.year
                    var resultDate: LocalDate? = null
                    while (resultDate == null) {
                        val currentYear = anchorYear + (yearIndex * interval).toInt()
                        val currentMonth = YearMonth.of(currentYear, rule.yearlyMonth)
                        val candidateDate = calculateOrdinalWeekday(currentMonth, rule.ordinal, rule.weekday)
                        if (candidateDate != null && candidateDate.isAfter(afterDate)) {
                            resultDate = candidateDate
                        }
                        yearIndex++
                    }
                    resultDate
                } else {
                    // Specific Date Mode with Feb 29 leap-day clamping
                    var yearIndex = 0L
                    val anchorYear = baseDate.year
                    val targetMonth = if (rule.yearlyMonth in 1..12) rule.yearlyMonth else baseDate.monthValue
                    val anchorDay = baseDate.dayOfMonth

                    var resultDate: LocalDate? = null
                    while (resultDate == null) {
                        val currentYear = anchorYear + (yearIndex * interval).toInt()
                        val currentMonth = YearMonth.of(currentYear, targetMonth)
                        val clampedDay = minOf(anchorDay, currentMonth.lengthOfMonth())
                        val candidateDate = currentMonth.atDay(clampedDay)
                        if (candidateDate.isAfter(afterDate)) {
                            resultDate = candidateDate
                        }
                        yearIndex++
                    }
                    resultDate
                }
            }
        }
    }

    /**
     * Core calculation logic returning a LocalDate on or after [searchFromDate].
     */
    fun calculateFirstLocalDate(
        baseDate: LocalDate,
        rule: RecurrenceRule,
        searchFromDate: LocalDate
    ): LocalDate? {
        if (!rule.isRecurring()) return null
        val effectiveSearchFrom = if (searchFromDate.isBefore(baseDate)) baseDate else searchFromDate

        // Check if baseDate itself is valid
        val baseDateMinusOne = effectiveSearchFrom.minusDays(1)
        val candidate = calculateNextLocalDate(baseDate, rule, baseDateMinusOne)
        if (candidate != null && !candidate.isBefore(effectiveSearchFrom)) {
            return candidate
        }
        return calculateNextLocalDate(baseDate, rule, effectiveSearchFrom)
    }

    /**
     * Helper to compute ordinal weekdays in a given YearMonth (e.g. Last Thursday, Fourth Wednesday, First Monday).
     */
    fun calculateOrdinalWeekday(
        yearMonth: YearMonth,
        ordinal: RecurrenceOrdinal,
        weekday: RecurrenceDayOfWeek
    ): LocalDate? {
        val targetDayOfWeek = toJavaDayOfWeek(weekday)
        val matchingDays = (1..yearMonth.lengthOfMonth())
            .map { yearMonth.atDay(it) }
            .filter { it.dayOfWeek == targetDayOfWeek }

        if (matchingDays.isEmpty()) return null

        return when (ordinal) {
            RecurrenceOrdinal.FIRST -> matchingDays.getOrNull(0)
            RecurrenceOrdinal.SECOND -> matchingDays.getOrNull(1)
            RecurrenceOrdinal.THIRD -> matchingDays.getOrNull(2)
            RecurrenceOrdinal.FOURTH -> matchingDays.getOrNull(3)
            RecurrenceOrdinal.LAST -> matchingDays.lastOrNull()
        }
    }

    private fun toJavaDayOfWeek(day: RecurrenceDayOfWeek): DayOfWeek {
        return when (day) {
            RecurrenceDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            RecurrenceDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            RecurrenceDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            RecurrenceDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            RecurrenceDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            RecurrenceDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            RecurrenceDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }

    private fun toRecurrenceDayOfWeek(day: DayOfWeek): RecurrenceDayOfWeek {
        return when (day) {
            DayOfWeek.MONDAY -> RecurrenceDayOfWeek.MONDAY
            DayOfWeek.TUESDAY -> RecurrenceDayOfWeek.TUESDAY
            DayOfWeek.WEDNESDAY -> RecurrenceDayOfWeek.WEDNESDAY
            DayOfWeek.THURSDAY -> RecurrenceDayOfWeek.THURSDAY
            DayOfWeek.FRIDAY -> RecurrenceDayOfWeek.FRIDAY
            DayOfWeek.SATURDAY -> RecurrenceDayOfWeek.SATURDAY
            DayOfWeek.SUNDAY -> RecurrenceDayOfWeek.SUNDAY
        }
    }

    private fun parseDateOrNull(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
