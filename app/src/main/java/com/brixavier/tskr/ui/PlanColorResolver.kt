package com.brixavier.tskr.ui

import androidx.compose.ui.graphics.Color
import com.brixavier.tskr.model.PlanColor

/**
 * Theme-aware rendering abstraction resolving a [PlanColor] into a Compose [Color]
 * suitable for light or dark themes with accessible contrast.
 */
object PlanColorResolver {

    fun resolve(color: PlanColor, isDark: Boolean): Color {
        return when (color) {
            PlanColor.BLUE -> if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
            PlanColor.GREEN -> if (isDark) Color(0xFF81C784) else Color(0xFF388E3C)
            PlanColor.VIOLET -> if (isDark) Color(0xFFBA68C8) else Color(0xFF7B1FA2)
            PlanColor.ORANGE -> if (isDark) Color(0xFFFFB74D) else Color(0xFFF57C00)
            PlanColor.TEAL -> if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
            PlanColor.ROSE -> if (isDark) Color(0xFFF06292) else Color(0xFFC2185B)
            PlanColor.INDIGO -> if (isDark) Color(0xFF7986CB) else Color(0xFF3F51B5)
            PlanColor.AMBER -> if (isDark) Color(0xFFFFD54F) else Color(0xFFFFA000)
            PlanColor.CYAN -> if (isDark) Color(0xFF4DD0E1) else Color(0xFF0097A7)
            PlanColor.SLATE -> if (isDark) Color(0xFF90A4AE) else Color(0xFF546E7A)
        }
    }
}
