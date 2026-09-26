package com.brixavier.tskr.model

/**
 * Strongly-typed domain enum distinguishing whether a Plan's category was assigned
 * automatically by Tsk.R or explicitly selected by the user.
 */
enum class CategorySource {
    AUTOMATIC,
    MANUAL;

    companion object {
        /**
         * Safely parses a category source string, falling back to [AUTOMATIC] for null, blank, or unknown values.
         */
        fun fromString(value: String?): CategorySource {
            if (value.isNullOrBlank()) return AUTOMATIC
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: Exception) {
                AUTOMATIC
            }
        }
    }
}
