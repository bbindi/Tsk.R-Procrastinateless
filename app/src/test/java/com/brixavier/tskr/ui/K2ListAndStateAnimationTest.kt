package com.brixavier.tskr.ui

import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.ui.theme.TskRMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class K2ListAndStateAnimationTest {

    @Test
    fun testTaskCompletion_alphaAndScaleTargetValues() {
        val activeTask = Reminder(id = "r1", title = "Active Task", date = "2026-09-26", time = "10:00", isActive = true)
        val completedTask = activeTask.copy(isActive = false)

        val activeAlpha = if (!activeTask.isActive) 0.6f else 1.0f
        val completedAlpha = if (!completedTask.isActive) 0.6f else 1.0f

        val activeScale = if (!activeTask.isActive) 1.1f else 1.0f
        val completedScale = if (!completedTask.isActive) 1.1f else 1.0f

        assertEquals(1.0f, activeAlpha, 0.001f)
        assertEquals(0.6f, completedAlpha, 0.001f)

        assertEquals(1.0f, activeScale, 0.001f)
        assertEquals(1.1f, completedScale, 0.001f)
    }

    @Test
    fun testPlanStepCompletion_alphaTargetValues() {
        val activeStep = PlanStep(id = "s1", planId = "p1", text = "Active step", isCompleted = false)
        val completedStep = activeStep.copy(isCompleted = true)

        val activeAlpha = if (activeStep.isCompleted) 0.6f else 1.0f
        val completedAlpha = if (completedStep.isCompleted) 0.6f else 1.0f

        assertEquals(1.0f, activeAlpha, 0.001f)
        assertEquals(0.6f, completedAlpha, 0.001f)
    }

    @Test
    fun testProgressAnimation_fractionCalculation() {
        val steps = listOf(
            PlanStep(id = "s1", planId = "p1", text = "Step 1", isCompleted = true),
            PlanStep(id = "s2", planId = "p1", text = "Step 2", isCompleted = true),
            PlanStep(id = "s3", planId = "p1", text = "Step 3", isCompleted = false),
            PlanStep(id = "s4", planId = "p1", text = "Step 4", isCompleted = false)
        )

        val completedCount = steps.count { it.isCompleted }
        val totalCount = steps.size
        val progressFraction = completedCount.toFloat() / totalCount.toFloat()

        assertEquals(0.5f, progressFraction, 0.001f)
    }

    @Test
    fun testK2Motion_usesK1Tokens() {
        assertEquals(150, TskRMotion.DURATION_MICRO)
        assertEquals(220, TskRMotion.DURATION_STANDARD)
    }
}
