package com.brixavier.tskr.model

import com.brixavier.tskr.ui.TskRPlanIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlanCategoryClassifierTest {

    @Test
    fun testCategoryEnum_hasExactly14Categories() {
        assertEquals(14, PlanCategory.entries.size)
    }

    @Test
    fun testCategorySerialization_isStableAndParsesCorrectly() {
        assertEquals(PlanCategory.TRAVEL, PlanCategory.fromString("TRAVEL"))
        assertEquals(PlanCategory.WORK, PlanCategory.fromString("work"))
        assertEquals(PlanCategory.FINANCE, PlanCategory.fromString("FINANCE"))
    }

    @Test
    fun testCategoryParsing_nullBlankAndUnknownFallbackToPersonal() {
        assertEquals(PlanCategory.PERSONAL, PlanCategory.fromString(null))
        assertEquals(PlanCategory.PERSONAL, PlanCategory.fromString(""))
        assertEquals(PlanCategory.PERSONAL, PlanCategory.fromString("   "))
        assertEquals(PlanCategory.PERSONAL, PlanCategory.fromString("UNKNOWN_CATEGORY_123"))
    }

    @Test
    fun testCategorySource_parsesCorrectlyAndFallsBackToAutomatic() {
        assertEquals(CategorySource.AUTOMATIC, CategorySource.fromString("AUTOMATIC"))
        assertEquals(CategorySource.MANUAL, CategorySource.fromString("manual"))
        assertEquals(CategorySource.AUTOMATIC, CategorySource.fromString(null))
        assertEquals(CategorySource.AUTOMATIC, CategorySource.fromString(""))
        assertEquals(CategorySource.AUTOMATIC, CategorySource.fromString("INVALID_SOURCE"))
    }

    @Test
    fun testClassifier_emptyOrUnknownTitle_returnsPersonal() {
        assertEquals(PlanCategory.PERSONAL, PlanCategoryClassifier.classify(""))
        assertEquals(PlanCategory.PERSONAL, PlanCategoryClassifier.classify("   "))
        assertEquals(PlanCategory.PERSONAL, PlanCategoryClassifier.classify("XYZ123456789"))
    }

    @Test
    fun testClassifier_caseAndPunctuationInsensitive() {
        val res1 = PlanCategoryClassifier.classify("Plan my trip to Ghana")
        val res2 = PlanCategoryClassifier.classify("PLAN MY TRIP TO GHANA!")
        val res3 = PlanCategoryClassifier.classify("  plan... my??? trip-- to, Ghana   ")

        assertEquals(PlanCategory.TRAVEL, res1)
        assertEquals(res1, res2)
        assertEquals(res2, res3)
    }

    @Test
    fun testClassifier_genericVerbsDoNotDominate() {
        // "Plan" and "prepare" should have zero weight and not force PLANNING
        val result = PlanCategoryClassifier.classify("Plan and prepare house cleaning")
        assertEquals(PlanCategory.HOME, result)
    }

    @Test
    fun testClassifier_realisticPlanTitleExamples() {
        assertEquals(PlanCategory.TRAVEL, PlanCategoryClassifier.classify("Plan my family trip to Ghana"))
        assertEquals(PlanCategory.SPIRITUAL, PlanCategoryClassifier.classify("Prepare for the convention"))
        assertEquals(PlanCategory.FINANCE, PlanCategoryClassifier.classify("Save money for a Pixel phone"))
        assertEquals(PlanCategory.HOME, PlanCategoryClassifier.classify("Clean and reorganize the house"))
        assertEquals(PlanCategory.WORK, PlanCategoryClassifier.classify("Study for my exam"))
        assertEquals(PlanCategory.EVENTS, PlanCategoryClassifier.classify("Prepare wedding invitation"))
        assertEquals(PlanCategory.HEALTH, PlanCategoryClassifier.classify("Exercise regularly"))
        assertEquals(PlanCategory.SHOPPING, PlanCategoryClassifier.classify("Buy new office chair"))
        assertEquals(PlanCategory.SPIRITUAL, PlanCategoryClassifier.classify("Read the Bible more regularly"))
        assertEquals(PlanCategory.GOALS, PlanCategoryClassifier.classify("Plan my goals for 2027"))
        assertEquals(PlanCategory.PROJECTS, PlanCategoryClassifier.classify("Build Budget.R app feature"))
        assertEquals(PlanCategory.CREATIVE, PlanCategoryClassifier.classify("Write new design blog post"))
    }

    @Test
    fun testClassifier_isDeterministicForRepeatedInput() {
        val title = "Organize family reunion event"
        val first = PlanCategoryClassifier.classify(title)
        val second = PlanCategoryClassifier.classify(title)
        val third = PlanCategoryClassifier.classify(title)

        assertEquals(first, second)
        assertEquals(second, third)
    }

    @Test
    fun testTskRPlanIcons_mapsEveryCategoryToNonNullVector() {
        for (category in PlanCategory.entries) {
            val icon = TskRPlanIcons.getIcon(category)
            assertNotNull(icon)
            assertNotNull(icon.name)
        }
    }
}
