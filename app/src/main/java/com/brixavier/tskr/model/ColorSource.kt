package com.brixavier.tskr.model

/**
 * Strongly-typed domain enum distinguishing whether a Plan's color is derived
 * from its category ([CATEGORY]) or explicitly selected by the user ([MANUAL]).
 */
enum class ColorSource {
    CATEGORY,
    MANUAL;

    companion object {
        /**
         * Safely parses a color source string, falling back to [CATEGORY] for null, blank, or unknown values.
         */
        fun fromString(value: String?): ColorSource {
            if (value.isNullOrBlank()) return CATEGORY
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: Exception) {
                CATEGORY
            }
        }
    }
}
