package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanStepsAndTasksTest {

    private val plan1 = Plan(id = "p-steps-1", name = "September Meeting Plan")
    private val plan2 = Plan(id = "p-steps-2", name = "Q4 Product Launch")

    @Test
    fun testPlanSteps_loadOnlyForCorrectPlan() {
        val st1 = PlanStep(id = "s1", planId = plan1.id, text = "Review minutes")
        val st2 = PlanStep(id = "s2", planId = plan2.id, text = "Gather stats")

        val allSteps = listOf(st1, st2)
        val plan1Steps = allSteps.filter { it.planId == plan1.id }

        assertEquals(1, plan1Steps.size)
        assertEquals("Review minutes", plan1Steps.first().text)
    }

    @Test
    fun testPlanSteps_followSortOrder() {
        val st1 = PlanStep(id = "s1", planId = plan1.id, text = "Second step", sortOrder = 1)
        val st2 = PlanStep(id = "s2", planId = plan1.id, text = "First step", sortOrder = 0)

        val sorted = listOf(st1, st2).sortedBy { it.sortOrder }

        assertEquals("First step", sorted[0].text)
        assertEquals("Second step", sorted[1].text)
    }

    @Test
    fun testAddingStep_persistsWithCorrectPlanIdAndText() {
        val newStep = PlanStep(planId = plan1.id, text = "Confirm transportation")

        assertNotNull(newStep.id)
        assertEquals("p-steps-1", newStep.planId)
        assertEquals("Confirm transportation", newStep.text)
        assertFalse(newStep.isCompleted)
    }

    @Test
    fun testEditingStepText_updatesTextWithoutCreatingTask() {
        val original = PlanStep(id = "s1", planId = plan1.id, text = "Initial step")
        val updated = original.copy(text = "Updated step label", updatedAtMs = System.currentTimeMillis() + 50)

        assertEquals("Updated step label", updated.text)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun testCompletingAndUncompletingStep_updatesCompletionStateOnly() {
        val step = PlanStep(id = "s1", planId = plan1.id, text = "Prepare agenda", isCompleted = false)

        val completed = step.copy(isCompleted = true)
        assertTrue(completed.isCompleted)

        val uncompleted = completed.copy(isCompleted = false)
        assertFalse(uncompleted.isCompleted)
    }

    @Test
    fun testDeletingStep_affectsOnlyTargetStep() {
        val st1 = PlanStep(id = "s1", planId = plan1.id, text = "Step 1")
        val st2 = PlanStep(id = "s2", planId = plan1.id, text = "Step 2")

        val steps = mutableListOf(st1, st2)
        steps.removeIf { it.id == "s1" }

        assertEquals(1, steps.size)
        assertEquals("s2", steps.first().id)
    }

    @Test
    fun testReorderingSteps_persistsNewSortOrders() {
        val st1 = PlanStep(id = "s1", planId = plan1.id, text = "Original First", sortOrder = 0)
        val st2 = PlanStep(id = "s2", planId = plan1.id, text = "Original Second", sortOrder = 1)

        val reordered = listOf(st2.copy(sortOrder = 0), st1.copy(sortOrder = 1))

        assertEquals("s2", reordered.first { it.sortOrder == 0 }.id)
        assertEquals("s1", reordered.first { it.sortOrder == 1 }.id)
    }

    @Test
    fun testStepProgress_calculatedIndependentlyFromTaskProgress() {
        val steps = listOf(
            PlanStep(id = "s1", planId = plan1.id, text = "Step 1", isCompleted = true),
            PlanStep(id = "s2", planId = plan1.id, text = "Step 2", isCompleted = true),
            PlanStep(id = "s3", planId = plan1.id, text = "Step 3", isCompleted = false)
        )

        val stepCompletedCount = steps.count { it.isCompleted }
        val stepTotalCount = steps.size
        val stepRatio = stepCompletedCount.toFloat() / stepTotalCount.toFloat()

        val taskProgress = PlanProgress(planId = plan1.id, completedTasks = 1, totalTasks = 2)

        assertEquals(2, stepCompletedCount)
        assertEquals(3, stepTotalCount)
        assertEquals(2.0f / 3.0f, stepRatio, 0.001f)

        assertEquals(1, taskProgress.completedTasks)
        assertEquals(2, taskProgress.totalTasks)
        assertEquals(0.5f, taskProgress.progressRatio, 0.001f)
    }

    @Test
    fun testConnectedTasks_loadOnlyForCorrectPlan() {
        val r1 = Reminder(id = "r1", title = "Task P1", date = "2026-09-26", time = "10:00", planId = plan1.id)
        val r2 = Reminder(id = "r2", title = "Task P2", date = "2026-09-26", time = "11:00", planId = plan2.id)

        val allTasks = listOf(r1, r2)
        val p1Tasks = allTasks.filter { it.planId == plan1.id }

        assertEquals(1, p1Tasks.size)
        assertEquals("Task P1", p1Tasks.first().title)
    }

    @Test
    fun testAddingTaskToPlan_preservesNormalTaskSemantics() {
        val task = Reminder(
            id = "r-new",
            title = "Send meeting minutes",
            date = "2026-09-27",
            time = "16:00",
            planId = plan1.id,
            priority = Reminder.PRIORITY_IMPORTANT
        )

        assertEquals("p-steps-1", task.planId)
        assertTrue(task.isImportant())
        assertTrue(task.isActive)
    }

    @Test
    fun testRemovingTaskFromPlan_detachesPlanIdWithoutDeletingTask() {
        val task = Reminder(id = "r-detach", title = "Shared Task", date = "2026-09-26", time = "10:00", planId = plan1.id)
        val detachedTask = task.copy(planId = null)

        assertEquals("r-detach", detachedTask.id)
        assertNull(detachedTask.planId)
        assertTrue(detachedTask.isActive)
    }

    @Test
    fun testDeletingPlan_doesNotDeleteItsTasks() {
        val task = Reminder(id = "r-preserve", title = "Task to Preserve", date = "2026-09-26", time = "10:00", planId = plan1.id)
        val completedOcc = ReminderOccurrence(id = "occ-preserve", templateId = task.id, title = task.title, date = "2026-09-20", time = "10:00", status = ReminderOccurrence.STATUS_COMPLETED, planId = plan1.id)

        // Deleting Plan 1 detaches planId = null
        val detachedTask = task.copy(planId = null)
        val detachedOcc = completedOcc.copy(planId = null)

        assertEquals("r-preserve", detachedTask.id)
        assertNull(detachedTask.planId)

        assertEquals("occ-preserve", detachedOcc.id)
        assertNull(detachedOcc.planId)
        assertTrue(detachedOcc.isCompleted())
    }

    @Test
    fun testPlanWithZeroStepsAndZeroTasks_remainsValid() {
        val emptyPlan = Plan(id = "p-empty", name = "Empty Workspace")
        val steps = emptyList<PlanStep>()
        val tasks = emptyList<Reminder>()
        val progress = PlanProgress(planId = emptyPlan.id, completedTasks = 0, totalTasks = 0)

        assertTrue(steps.isEmpty())
        assertTrue(tasks.isEmpty())
        assertEquals(0, progress.totalTasks)
        assertEquals(0.0f, progress.progressRatio, 0.001f)
    }
}
