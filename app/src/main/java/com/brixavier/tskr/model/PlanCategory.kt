package com.brixavier.tskr.model

/**
 * Strongly-typed domain enum representing the 14 approved Plan categories.
 * Provides stable, localization-independent serialization values and user-facing display names.
 */
enum class PlanCategory(
    val displayName: String
) {
    WORK("Work"),
    PERSONAL("Personal"),
    PLANNING("Planning"),
    FINANCE("Finance"),
    HOME("Home"),
    FAMILY("Family"),
    HEALTH("Health"),
    TRAVEL("Travel"),
    EVENTS("Events"),
    PROJECTS("Projects"),
    SPIRITUAL("Spiritual"),
    CREATIVE("Creative"),
    SHOPPING("Shopping"),
    GOALS("Goals");

    companion object {
        /**
         * Safely parses a category string, falling back to [PERSONAL] for null, blank, or unknown values.
         */
        fun fromString(value: String?): PlanCategory {
            if (value.isNullOrBlank()) return PERSONAL
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: Exception) {
                PERSONAL
            }
        }
    }
}
