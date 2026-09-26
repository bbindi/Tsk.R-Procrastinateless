package com.brixavier.tskr.model

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class MonthlyMode {
    DAY_OF_MONTH,
    ORDINAL_WEEKDAY
}

enum class YearlyMode {
    SPECIFIC_DATE,
    ORDINAL_WEEKDAY
}

enum class RecurrenceOrdinal {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    LAST
}

enum class RecurrenceDayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

/**
 * Domain model representing a complete, normalized recurrence rule.
 */
data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1,
    val daysOfWeek: Set<RecurrenceDayOfWeek> = emptySet(),
    val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    val yearlyMode: YearlyMode = YearlyMode.SPECIFIC_DATE,
    val ordinal: RecurrenceOrdinal? = null,
    val weekday: RecurrenceDayOfWeek? = null,
    val yearlyMonth: Int = 0 // 1..12
) {
    fun isRecurring(): Boolean = type != RecurrenceType.NONE

    /**
     * Serializes selected weekdays to Monday -> Sunday ordered string: "MON|WED|FRI".
     */
    fun getSerializedDaysOfWeek(): String {
        if (daysOfWeek.isEmpty()) return ""
        val sortedList = RecurrenceDayOfWeek.entries.filter { daysOfWeek.contains(it) }
        return sortedList.joinToString("|") { it.name.take(3) }
    }

    /**
     * Domain validation for RecurrenceRule.
     */
    fun validate(): Result {
        if (type == RecurrenceType.NONE) return Result.Success

        if (interval < 1) return Result.Error("Interval must be at least 1.")

        return when (type) {
            RecurrenceType.NONE -> Result.Success
            RecurrenceType.DAILY -> Result.Success
            RecurrenceType.WEEKLY -> {
                if (daysOfWeek.isEmpty()) {
                    Result.Error("At least one weekday must be selected for weekly recurrence.")
                } else {
                    Result.Success
                }
            }
            RecurrenceType.MONTHLY -> {
                if (monthlyMode == MonthlyMode.ORDINAL_WEEKDAY) {
                    if (ordinal == null || weekday == null) {
                        Result.Error("Ordinal and Weekday are required for ordinal monthly recurrence.")
                    } else {
                        Result.Success
                    }
                } else {
                    Result.Success
                }
            }
            RecurrenceType.YEARLY -> {
                if (yearlyMode == YearlyMode.ORDINAL_WEEKDAY) {
                    if (ordinal == null || weekday == null || yearlyMonth !in 1..12) {
                        Result.Error("Month, Ordinal, and Weekday are required for ordinal yearly recurrence.")
                    } else {
                        Result.Success
                    }
                } else {
                    if (yearlyMonth !in 1..12) {
                        Result.Error("Valid month (1-12) is required for yearly recurrence.")
                    } else {
                        Result.Success
                    }
                }
            }
        }
    }

    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        fun parseDaysOfWeek(serialized: String): Set<RecurrenceDayOfWeek> {
            if (serialized.isBlank()) return emptySet()
            return serialized.split("|")
                .mapNotNull { token ->
                    when (token.uppercase()) {
                        "MON" -> RecurrenceDayOfWeek.MONDAY
                        "TUE" -> RecurrenceDayOfWeek.TUESDAY
                        "WED" -> RecurrenceDayOfWeek.WEDNESDAY
                        "THU" -> RecurrenceDayOfWeek.THURSDAY
                        "FRI" -> RecurrenceDayOfWeek.FRIDAY
                        "SAT" -> RecurrenceDayOfWeek.SATURDAY
                        "SUN" -> RecurrenceDayOfWeek.SUNDAY
                        else -> null
                    }
                }.toSet()
        }
    }
}
