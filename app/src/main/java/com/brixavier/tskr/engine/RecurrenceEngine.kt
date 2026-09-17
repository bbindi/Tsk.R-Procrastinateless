package com.brixavier.tskr.engine

import com.brixavier.tskr.model.Reminder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic, pure Kotlin calculation engine for recurring reminder schedules.
 *
 * Contains zero Android UI, Room, AlarmManager, or notification dependencies.
 * Guarantees strict schedule anchoring and single-occurrence (no backlog) output.
 */
object RecurrenceEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    /**
     * Calculates the next scheduled recurrence date string (YYYY-MM-DD) based on:
     * - [baseDateStr]: The original anchor/scheduled date string (YYYY-MM-DD).
     * - [recurrenceType]: "NONE", "DAILY", "WEEKLY", "MONTHLY", or "CUSTOM_DAYS".
     * - [recurrenceInterval]: Interval multiplier (e.g. 1 for every unit, 2 for every 2 units).
     * - [afterDateStr]: Reference date string (YYYY-MM-DD) after which the next occurrence must fall.
     *   Defaults to current date if null or empty.
     *
     * Returns null if [recurrenceType] is "NONE" or invalid.
     */
    fun calculateNextDueDate(
        baseDateStr: String,
        recurrenceType: String,
        recurrenceInterval: Int,
        afterDateStr: String? = null
    ): String? {
        if (recurrenceType == Reminder.RECURRENCE_NONE || recurrenceType.isBlank()) {
            return null
        }

        val baseDate = parseDateOrNull(baseDateStr) ?: return null
        val afterDate = if (!afterDateStr.isNullOrBlank()) {
            parseDateOrNull(afterDateStr) ?: LocalDate.now()
        } else {
            LocalDate.now()
        }

        val safeInterval = if (recurrenceInterval < 1) 1 else recurrenceInterval

        val nextDate = calculateNextLocalDate(baseDate, recurrenceType, safeInterval, afterDate)
        return nextDate?.format(dateFormatter)
    }

    /**
     * Internal calculation operating on LocalDate instances.
     * Guaranteed to produce a LocalDate strictly AFTER [afterDate].
     */
    fun calculateNextLocalDate(
        baseDate: LocalDate,
        recurrenceType: String,
        recurrenceInterval: Int,
        afterDate: LocalDate
    ): LocalDate? {
        val safeInterval = if (recurrenceInterval < 1) 1 else recurrenceInterval

        if (recurrenceType == Reminder.RECURRENCE_NONE || recurrenceType.isBlank()) {
            return null
        }

        var candidate = baseDate

        when (recurrenceType) {
            Reminder.RECURRENCE_DAILY, Reminder.RECURRENCE_CUSTOM_DAYS -> {
                while (!candidate.isAfter(afterDate)) {
                    candidate = candidate.plusDays(safeInterval.toLong())
                }
            }
            Reminder.RECURRENCE_WEEKLY -> {
                while (!candidate.isAfter(afterDate)) {
                    candidate = candidate.plusWeeks(safeInterval.toLong())
                }
            }
            Reminder.RECURRENCE_MONTHLY -> {
                var currentMonth = YearMonth.from(candidate)
                while (!candidate.isAfter(afterDate)) {
                    currentMonth = currentMonth.plusMonths(safeInterval.toLong())
                    val clampedDay = minOf(baseDate.dayOfMonth, currentMonth.lengthOfMonth())
                    candidate = currentMonth.atDay(clampedDay)
                }
            }
            else -> return null
        }

        return candidate
    }

    private fun parseDateOrNull(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
