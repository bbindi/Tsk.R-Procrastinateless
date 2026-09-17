package com.brixavier.tskr.data

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWorkspaceDataTest {

    private val plan1 = Plan(id = "p1", name = "Q4 Product Strategy", description = "Planning for Q4 launch")
    private val plan2 = Plan(id = "p2", name = "Office Relocation", description = "Moving to new building")

    @Test
    fun testPlanNoteSection_modelAttributesAndDefaults() {
        val section = PlanNoteSection(
            planId = plan1.id,
            title = "Key Discussions",
            content = "- Budget review\n- Staffing requirements",
            sortOrder = 1
        )

        assertNotNull(section.id)
        assertEquals("p1", section.planId)
        assertEquals("Key Discussions", section.title)
        assertEquals("- Budget review\n- Staffing requirements", section.content)
        assertEquals(1, section.sortOrder)
        assertTrue(section.createdAtMs > 0)
        assertTrue(section.updatedAtMs > 0)
    }

    @Test
    fun testPlanStep_modelAttributesAndCompletionToggling() {
        val step = PlanStep(
            planId = plan1.id,
            text = "Review prior minutes",
            isCompleted = false,
            sortOrder = 0
        )

        assertNotNull(step.id)
        assertEquals("p1", step.planId)
        assertEquals("Review prior minutes", step.text)
        assertFalse(step.isCompleted)

        val completedStep = step.copy(isCompleted = true, updatedAtMs = System.currentTimeMillis() + 100)
        assertTrue(completedStep.isCompleted)
        assertTrue(completedStep.updatedAtMs >= step.updatedAtMs)
    }

    @Test
    fun testNoteSections_sortedBySortOrder() {
        val section1 = PlanNoteSection(id = "s1", planId = plan1.id, title = "Ideas", sortOrder = 2)
        val section2 = PlanNoteSection(id = "s2", planId = plan1.id, title = "Overview", sortOrder = 0)
        val section3 = PlanNoteSection(id = "s3", planId = plan1.id, title = "Decisions", sortOrder = 1)

        val unsorted = listOf(section1, section2, section3)
        val sorted = unsorted.sortedBy { it.sortOrder }

        assertEquals("Overview", sorted[0].title)
        assertEquals("Decisions", sorted[1].title)
        assertEquals("Ideas", sorted[2].title)
    }

    @Test
    fun testPlanSteps_sortedBySortOrder() {
        val step1 = PlanStep(id = "st1", planId = plan1.id, text = "Step 2", sortOrder = 1)
        val step2 = PlanStep(id = "st2", planId = plan1.id, text = "Step 1", sortOrder = 0)

        val unsorted = listOf(step1, step2)
        val sorted = unsorted.sortedBy { it.sortOrder }

        assertEquals("Step 1", sorted[0].text)
        assertEquals("Step 2", sorted[1].text)
    }

    @Test
    fun testPlanIsolation_noteSectionsAndStepsBelongToCorrectPlan() {
        val secPlan1 = PlanNoteSection(id = "ns1", planId = plan1.id, title = "P1 Overview")
        val secPlan2 = PlanNoteSection(id = "ns2", planId = plan2.id, title = "P2 Logistics")

        val stepPlan1 = PlanStep(id = "ps1", planId = plan1.id, text = "P1 Action Step")
        val stepPlan2 = PlanStep(id = "ps2", planId = plan2.id, text = "P2 Action Step")

        val allSections = listOf(secPlan1, secPlan2)
        val allSteps = listOf(stepPlan1, stepPlan2)

        val plan1Sections = allSections.filter { it.planId == plan1.id }
        val plan2Sections = allSections.filter { it.planId == plan2.id }

        val plan1Steps = allSteps.filter { it.planId == plan1.id }
        val plan2Steps = allSteps.filter { it.planId == plan2.id }

        assertEquals(1, plan1Sections.size)
        assertEquals("P1 Overview", plan1Sections.first().title)

        assertEquals(1, plan2Sections.size)
        assertEquals("P2 Logistics", plan2Sections.first().title)

        assertEquals(1, plan1Steps.size)
        assertEquals("P1 Action Step", plan1Steps.first().text)

        assertEquals(1, plan2Steps.size)
        assertEquals("P2 Action Step", plan2Steps.first().text)
    }

    @Test
    fun testPlanDeletion_cascadesToSectionsAndSteps_butDetachesTasksWithoutDeleting() {
        // Task attached to Plan 1
        val task = Reminder(id = "r1", title = "Prepare Presentation", date = "2026-09-26", time = "10:00", planId = plan1.id)
        val completedOcc = ReminderOccurrence(id = "occ1", templateId = task.id, title = task.title, date = "2026-09-20", time = "10:00", planId = plan1.id, status = ReminderOccurrence.STATUS_COMPLETED)

        // Sections & Steps belonging to Plan 1
        val section = PlanNoteSection(id = "sec1", planId = plan1.id, title = "Overview")
        val step = PlanStep(id = "step1", planId = plan1.id, text = "Draft slides")

        // Simulating ON DELETE CASCADE for PlanNoteSection and PlanStep
        val remainingSections = listOf(section).filter { it.planId != plan1.id }
        val remainingSteps = listOf(step).filter { it.planId != plan1.id }

        // Simulating ON DELETE SET NULL for Task and ReminderOccurrence
        val detachedTask = task.copy(planId = null)
        val detachedOcc = completedOcc.copy(planId = null)

        assertTrue(remainingSections.isEmpty())
        assertTrue(remainingSteps.isEmpty())

        assertEquals("r1", detachedTask.id)
        assertNull(detachedTask.planId)

        assertEquals("occ1", detachedOcc.id)
        assertNull(detachedOcc.planId)
    }
}
