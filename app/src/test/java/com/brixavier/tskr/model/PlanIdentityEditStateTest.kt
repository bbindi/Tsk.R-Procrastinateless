package com.brixavier.tskr.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanIdentityEditStateTest {

    @Test
    fun testA_newPlanDefaultsToAutomaticAndCategoryColor() {
        val name = "Trip to Ghana"
        val suggestedCategory = PlanCategoryClassifier.classify(name)
        val plan = Plan(name = name, category = suggestedCategory.name)
        assertEquals(CategorySource.AUTOMATIC, plan.getCategorySource())
        assertEquals(ColorSource.CATEGORY, plan.getColorSource())
        assertEquals(PlanCategory.TRAVEL, plan.getPlanCategory())
        assertEquals(PlanColor.CYAN, plan.resolveEffectiveColor())
    }

    @Test
    fun testB_automaticCategoryReclassificationOnNameChange() {
        var plan = Plan(name = "Trip to Ghana", category = "TRAVEL", categorySource = "AUTOMATIC")
        
        // Name changes to financial budget
        val newName = "Monthly budget"
        val newCategory = PlanCategoryClassifier.classify(newName)
        plan = plan.copy(name = newName, category = newCategory.name)

        assertEquals(PlanCategory.FINANCE, plan.getPlanCategory())
        assertEquals(PlanColor.TEAL, plan.resolveEffectiveColor())
    }

    @Test
    fun testC_manualCategoryProtection_doesNotReclassifyOnRename() {
        val plan = Plan(name = "Trip to Ghana", category = "FAMILY", categorySource = "MANUAL")
        
        // Name changes, but categorySource is MANUAL
        // Since categorySource == MANUAL, system does NOT run classifier
        // Category remains FAMILY
        assertEquals(PlanCategory.FAMILY, plan.getPlanCategory())
        assertEquals(CategorySource.MANUAL, plan.getCategorySource())
    }

    @Test
    fun testD_categoryDerivedColor_followsCategoryChanges() {
        var plan = Plan(
            name = "Family Trip",
            category = "FAMILY",
            categorySource = "AUTOMATIC",
            colorSource = "CATEGORY"
        )
        assertEquals(PlanColor.ROSE, plan.resolveEffectiveColor())

        plan = plan.copy(category = "TRAVEL")
        assertEquals(PlanColor.CYAN, plan.resolveEffectiveColor())
    }

    @Test
    fun testE_manualColorProtection_preservesManualColorOnCategoryChange() {
        var plan = Plan(
            name = "Family Trip",
            category = "FAMILY",
            categorySource = "AUTOMATIC",
            color = "BLUE",
            colorSource = "MANUAL"
        )
        assertEquals(PlanColor.BLUE, plan.resolveEffectiveColor())

        // Change category to TRAVEL, but manual color is preserved
        plan = plan.copy(category = "TRAVEL")
        assertEquals(PlanColor.BLUE, plan.resolveEffectiveColor())
        assertEquals(ColorSource.MANUAL, plan.getColorSource())
    }

    @Test
    fun testF_returnToCategoryColor_restoresDerivedColor() {
        var plan = Plan(
            name = "Family Trip",
            category = "TRAVEL",
            categorySource = "MANUAL",
            color = "BLUE",
            colorSource = "MANUAL"
        )
        assertEquals(PlanColor.BLUE, plan.resolveEffectiveColor())

        // User chooses "Use category color"
        plan = plan.copy(colorSource = "CATEGORY")
        assertEquals(PlanColor.CYAN, plan.resolveEffectiveColor())
        assertEquals(ColorSource.CATEGORY, plan.getColorSource())
    }

    @Test
    fun testG_persistenceRoundTrip_survivesEntityCopyAndMapping() {
        val original = Plan(
            name = "Test Plan",
            category = "WORK",
            categorySource = "MANUAL",
            color = "GREEN",
            colorSource = "MANUAL"
        )

        val restored = original.copy()
        assertEquals("WORK", restored.category)
        assertEquals("MANUAL", restored.categorySource)
        assertEquals("GREEN", restored.color)
        assertEquals("MANUAL", restored.colorSource)
        assertEquals(PlanCategory.WORK, restored.getPlanCategory())
        assertEquals(CategorySource.MANUAL, restored.getCategorySource())
        assertEquals(PlanColor.GREEN, restored.resolveEffectiveColor())
    }
}
