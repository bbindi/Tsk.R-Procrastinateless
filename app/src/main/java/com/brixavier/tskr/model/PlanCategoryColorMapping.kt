package com.brixavier.tskr.model

/**
 * Deterministic mapping linking every [PlanCategory] to its default semantic [PlanColor].
 */
object PlanCategoryColorMapping {

    fun getDefaultColor(category: PlanCategory): PlanColor {
        return when (category) {
            PlanCategory.WORK -> PlanColor.BLUE
            PlanCategory.PERSONAL -> PlanColor.SLATE
            PlanCategory.PLANNING -> PlanColor.INDIGO
            PlanCategory.FINANCE -> PlanColor.TEAL
            PlanCategory.HOME -> PlanColor.GREEN
            PlanCategory.FAMILY -> PlanColor.ROSE
            PlanCategory.HEALTH -> PlanColor.AMBER
            PlanCategory.TRAVEL -> PlanColor.CYAN
            PlanCategory.EVENTS -> PlanColor.VIOLET
            PlanCategory.PROJECTS -> PlanColor.INDIGO
            PlanCategory.SPIRITUAL -> PlanColor.VIOLET
            PlanCategory.CREATIVE -> PlanColor.ROSE
            PlanCategory.SHOPPING -> PlanColor.ORANGE
            PlanCategory.GOALS -> PlanColor.AMBER
        }
    }
}
