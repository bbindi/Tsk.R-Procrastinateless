package com.brixavier.tskr.data

import com.brixavier.tskr.model.PlanProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanProgressTest {

    @Test
    fun planProgress_zeroTasks_calculatesZeroRatioSafely() {
        val progress = PlanProgress(planId = "plan-1", completedTasks = 0, totalTasks = 0)
        
        assertEquals(0, progress.totalTasks)
        assertEquals(0, progress.completedTasks)
        assertEquals(0.0f, progress.progressRatio, 0.001f)
    }

    @Test
    fun planProgress_oneIncompleteTask_calculatesZeroRatio() {
        val progress = PlanProgress(planId = "plan-1", completedTasks = 0, totalTasks = 1)
        
        assertEquals(1, progress.totalTasks)
        assertEquals(0, progress.completedTasks)
        assertEquals(0.0f, progress.progressRatio, 0.001f)
    }

    @Test
    fun planProgress_oneCompletedTask_calculatesFullRatio() {
        val progress = PlanProgress(planId = "plan-1", completedTasks = 1, totalTasks = 1)
        
        assertEquals(1, progress.totalTasks)
        assertEquals(1, progress.completedTasks)
        assertEquals(1.0f, progress.progressRatio, 0.001f)
    }

    @Test
    fun planProgress_multipleTasks_calculatesRatioCorrectly() {
        // 3 of 4 tasks completed (75%)
        val progress = PlanProgress(planId = "plan-1", completedTasks = 3, totalTasks = 4)
        
        assertEquals(4, progress.totalTasks)
        assertEquals(3, progress.completedTasks)
        assertEquals(0.75f, progress.progressRatio, 0.001f)
    }
}
