package com.brixavier.tskr.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Domain model representing the user's Weekly Progress (Monday through Sunday).
 */
data class WeeklyProgress(
    val completedCount: Int = 0,
    val totalScheduledCount: Int = 0,
    val startOfWeekDate: String = "",
    val endOfWeekDate: String = ""
) {
    val progressRatio: Float
        get() = if (totalScheduledCount > 0) completedCount.toFloat() / totalScheduledCount.toFloat() else 0.0f

    val progressPercent: Int
        get() = (progressRatio * 100).toInt()
}

/**
 * Domain calculator for Weekly Progress (Monday through Sunday).
 */
object WeeklyProgressCalculator {

    /**
     * Returns the Monday (start) and Sunday (end) date strings formatted as YYYY-MM-DD for the given reference date.
     */
    fun getWeekBounds(referenceDate: LocalDate = LocalDate.now()): Pair<String, String> {
        val monday = referenceDate.with(DayOfWeek.MONDAY)
        val sunday = referenceDate.with(DayOfWeek.SUNDAY)
        return Pair(monday.toString(), sunday.toString())
    }

    /**
     * Calculates WeeklyProgress for occurrences falling within the Monday-Sunday week bounds.
     */
    fun calculate(
        occurrences: List<ReminderOccurrence>,
        mondayStr: String,
        sundayStr: String
    ): WeeklyProgress {
        val weekOccurrences = occurrences.filter { occ ->
            occ.date in mondayStr..sundayStr
        }
        val completed = weekOccurrences.count { it.status == ReminderOccurrence.STATUS_COMPLETED }
        val total = weekOccurrences.size

        return WeeklyProgress(
            completedCount = completed,
            totalScheduledCount = total,
            startOfWeekDate = mondayStr,
            endOfWeekDate = sundayStr
        )
    }
}
