package com.brixavier.tskr.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.brixavier.tskr.model.PlanCategory

/**
 * Material 3 / Tsk.R Outlined Vector Icon abstraction mapping each [PlanCategory]
 * to its official, consistent vector icon.
 */
object TskRPlanIcons {

    /**
     * Resolves a non-null M3 vector icon for a given [PlanCategory].
     */
    fun getIcon(category: PlanCategory): ImageVector {
        return when (category) {
            PlanCategory.WORK -> Icons.Outlined.WorkOutline
            PlanCategory.PERSONAL -> Icons.Outlined.PersonOutline
            PlanCategory.PLANNING -> Icons.Outlined.CalendarMonth
            PlanCategory.FINANCE -> Icons.Outlined.AccountBalanceWallet
            PlanCategory.HOME -> Icons.Outlined.HomeWork
            PlanCategory.FAMILY -> Icons.Outlined.PeopleOutline
            PlanCategory.HEALTH -> Icons.Outlined.FitnessCenter
            PlanCategory.TRAVEL -> Icons.Outlined.FlightTakeoff
            PlanCategory.EVENTS -> Icons.Outlined.Celebration
            PlanCategory.PROJECTS -> Icons.Outlined.FolderSpecial
            PlanCategory.SPIRITUAL -> Icons.Outlined.AutoAwesome
            PlanCategory.CREATIVE -> Icons.Outlined.Palette
            PlanCategory.SHOPPING -> Icons.Outlined.ShoppingBag
            PlanCategory.GOALS -> Icons.Outlined.EmojiEvents
        }
    }
}
