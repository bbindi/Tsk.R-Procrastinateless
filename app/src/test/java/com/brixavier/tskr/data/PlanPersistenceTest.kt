package com.brixavier.tskr.data

import com.brixavier.tskr.model.CategorySource
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanPersistenceTest {

    @Test
    fun testPlanCategoryExtensions_mapsCategoryStringsAccurately() {
        val plan = Plan(
            name = "Q4 Financial Budget",
            category = "FINANCE",
            categorySource = "MANUAL"
        )

        assertEquals(PlanCategory.FINANCE, plan.getPlanCategory())
        assertEquals(CategorySource.MANUAL, plan.getCategorySource())
    }

    @Test
    fun testPlanCategoryFallback_unknownValueDefaultsToPersonalAndAutomatic() {
        val planWithCorruptData = Plan(
            name = "Test Plan",
            category = "INVALID_CATEGORY_XYZ",
            categorySource = "INVALID_SOURCE_ABC"
        )

        assertEquals(PlanCategory.PERSONAL, planWithCorruptData.getPlanCategory())
        assertEquals(CategorySource.AUTOMATIC, planWithCorruptData.getCategorySource())
    }

    @Test
    fun testLegacyEmojiMapping_mapsKnownIconsToStructuredCategories() {
        val iconMap = mapOf(
            "💼" to PlanCategory.WORK,
            "🎯" to PlanCategory.GOALS,
            "💰" to PlanCategory.FINANCE,
            "🏠" to PlanCategory.HOME,
            "✈️" to PlanCategory.TRAVEL,
            "🎉" to PlanCategory.EVENTS,
            "📖" to PlanCategory.SPIRITUAL,
            "🎨" to PlanCategory.CREATIVE,
            "🛒" to PlanCategory.SHOPPING,
            "🏋️" to PlanCategory.HEALTH,
            "👨‍👩‍👧‍👦" to PlanCategory.FAMILY,
            "📁" to PlanCategory.PROJECTS,
            "📅" to PlanCategory.PLANNING
        )

        for ((emoji, expectedCategory) in iconMap) {
            val inferred = inferCategoryFromLegacyIcon(emoji)
            assertEquals(expectedCategory, inferred)
        }
    }

    @Test
    fun testUnrecognizedLegacyEmoji_fallsBackToPersonal() {
        assertEquals(PlanCategory.PERSONAL, inferCategoryFromLegacyIcon("👽"))
        assertEquals(PlanCategory.PERSONAL, inferCategoryFromLegacyIcon(""))
        assertEquals(PlanCategory.PERSONAL, inferCategoryFromLegacyIcon("   "))
    }

    private fun inferCategoryFromLegacyIcon(icon: String): PlanCategory {
        return when {
            icon.contains("💼") || icon.contains("🏢") || icon.contains("👔") -> PlanCategory.WORK
            icon.contains("🎯") || icon.contains("🏆") || icon.contains("🚩") -> PlanCategory.GOALS
            icon.contains("💰") || icon.contains("💵") || icon.contains("🏦") -> PlanCategory.FINANCE
            icon.contains("🏠") || icon.contains("🏡") || icon.contains("🧹") -> PlanCategory.HOME
            icon.contains("✈") || icon.contains("🧳") || icon.contains("🗺") -> PlanCategory.TRAVEL
            icon.contains("🎉") || icon.contains("🎂") || icon.contains("💍") -> PlanCategory.EVENTS
            icon.contains("📖") || icon.contains("🙏") || icon.contains("⛪") -> PlanCategory.SPIRITUAL
            icon.contains("🎨") || icon.contains("✍") || icon.contains("🎵") -> PlanCategory.CREATIVE
            icon.contains("🛒") || icon.contains("🛍") -> PlanCategory.SHOPPING
            icon.contains("🏋") || icon.contains("💪") || icon.contains("🏥") -> PlanCategory.HEALTH
            icon.contains("👨‍👩‍👧‍👦") || icon.contains("👶") || icon.contains("❤️") -> PlanCategory.FAMILY
            icon.contains("📁") || icon.contains("💻") || icon.contains("⚙") -> PlanCategory.PROJECTS
            icon.contains("📅") || icon.contains("📋") || icon.contains("🗓") -> PlanCategory.PLANNING
            else -> PlanCategory.PERSONAL
        }
    }
}
