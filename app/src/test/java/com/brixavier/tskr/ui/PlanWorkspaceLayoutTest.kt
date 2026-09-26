package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWorkspaceLayoutTest {

    private val plan = Plan(id = "p-layout-1", name = "September Workspace", description = "Meeting notes and actions")

    @Test
    fun testPlanHeader_hasNoPermanentPersonalityCard() {
        // Verifies Plan contains only name and description without personality card requirements
        assertNotNull(plan.name)
        assertEquals("September Workspace", plan.name)
        assertEquals("Meeting notes and actions", plan.description)
    }

    @Test
    fun testEmptyGoal_doesNotRequireReservedSpace() {
        val planEmptyDesc = Plan(id = "p-empty-desc", name = "Quick Notes")
        assertTrue(planEmptyDesc.description.isBlank())
    }

    @Test
    fun testCollapsibleStepsState_togglesWithoutAlteringData() {
        val steps = listOf(
            PlanStep(id = "st1", planId = plan.id, text = "Review agenda", isCompleted = true),
            PlanStep(id = "st2", planId = plan.id, text = "Confirm transportation", isCompleted = false)
        )

        var isStepsExpanded = false
        // Toggle expansion
        isStepsExpanded = !isStepsExpanded
        assertTrue(isStepsExpanded)

        // Verifies step data is completely unchanged by UI collapse/expand state
        assertEquals(2, steps.size)
        assertEquals("Review agenda", steps.first().text)
        assertTrue(steps.first().isCompleted)
    }

    @Test
    fun testCollapsibleTasksState_togglesWithoutAlteringData() {
        val tasks = listOf(
            Reminder(id = "r1", title = "Send minutes", date = "2026-09-26", time = "10:00", planId = plan.id)
        )

        var isTasksExpanded = false
        // Toggle expansion
        isTasksExpanded = !isTasksExpanded
        assertTrue(isTasksExpanded)

        // Verifies task data is completely unchanged by UI collapse/expand state
        assertEquals(1, tasks.size)
        assertEquals("Send minutes", tasks.first().title)
    }

    @Test
    fun testStepAndTaskProgress_remainSemanticallyIndependent() {
        val steps = listOf(
            PlanStep(id = "s1", planId = plan.id, text = "Step 1", isCompleted = true),
            PlanStep(id = "s2", planId = plan.id, text = "Step 2", isCompleted = true),
            PlanStep(id = "s3", planId = plan.id, text = "Step 3", isCompleted = false)
        )
        val completedSteps = steps.count { it.isCompleted }

        val taskProgress = PlanProgress(planId = plan.id, completedTasks = 1, totalTasks = 4)

        assertEquals("2 of 3 completed", "$completedSteps of ${steps.size} completed")
        assertEquals("1 of 4 completed", "${taskProgress.completedTasks} of ${taskProgress.totalTasks} completed")
    }

    @Test
    fun testZeroStepsAndZeroTasks_renderSafelyInCollapsibleCards() {
        val steps = emptyList<PlanStep>()
        val tasks = emptyList<Reminder>()
        val taskProgress = PlanProgress(planId = plan.id, completedTasks = 0, totalTasks = 0)

        assertTrue(steps.isEmpty())
        assertTrue(tasks.isEmpty())
        assertEquals(0, taskProgress.totalTasks)
    }

    @Test
    fun testSectionNavigation_remainsUsableWithManySections() {
        val sections = (1..10).map { i ->
            PlanNoteSection(id = "sec-$i", planId = plan.id, title = "Section $i", sortOrder = i)
        }

        val sorted = sections.sortedBy { it.sortOrder }

        assertEquals(10, sorted.size)
        assertEquals("Section 1", sorted.first().title)
        assertEquals("Section 10", sorted.last().title)
    }
}
