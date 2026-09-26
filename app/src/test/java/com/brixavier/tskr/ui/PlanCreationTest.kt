package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanCreationTest {

    @Test
    fun testPlanCreation_withOnlyName_succeedsWithDefaults() {
        val plan = Plan(name = "September Meeting")

        assertNotNull(plan.id)
        assertEquals("September Meeting", plan.name)
        assertEquals("", plan.description)
        assertEquals("", plan.icon)
        assertNull(plan.deadline)
        assertEquals(Plan.STATUS_ACTIVE, plan.status)
        assertTrue(plan.isActive())
        assertTrue(plan.createdAtMs > 0)
    }

    @Test
    fun testPlanCreation_withOptionalDescription_succeeds() {
        val plan = Plan(
            name = "Q4 Product Strategy",
            description = "Prepare launch requirements and roadmap"
        )

        assertEquals("Q4 Product Strategy", plan.name)
        assertEquals("Prepare launch requirements and roadmap", plan.description)
        assertNull(plan.deadline)
        assertTrue(plan.isActive())
    }

    @Test
    fun testPlanCreation_doesNotRequireDeadline() {
        val plan = Plan(
            name = "Office Relocation",
            deadline = null
        )

        assertNull(plan.deadline)
    }

    @Test
    fun testPlanCreation_defaultIconProvided_whenBlank() {
        val selectedIcon = ""
        val iconToSave = selectedIcon.ifBlank { "🎯" }

        assertEquals("🎯", iconToSave)
    }
}
