package com.brixavier.tskr.model

/**
 * Stable semantic Plan Color identity tokens.
 * Independent of raw hex strings or localized display names.
 */
enum class PlanColor(
    val displayName: String
) {
    BLUE("Blue"),
    GREEN("Green"),
    VIOLET("Violet"),
    ORANGE("Orange"),
    TEAL("Teal"),
    ROSE("Rose"),
    INDIGO("Indigo"),
    AMBER("Amber"),
    CYAN("Cyan"),
    SLATE("Slate");

    companion object {
        /**
         * Safely parses a color string, falling back to [BLUE] for null, blank, or unknown values.
         */
        fun fromString(value: String?): PlanColor {
            if (value.isNullOrBlank()) return BLUE
            return try {
                valueOf(value.trim().uppercase())
            } catch (_: Exception) {
                BLUE
            }
        }
    }
}
