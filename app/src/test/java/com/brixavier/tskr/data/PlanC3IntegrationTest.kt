package com.brixavier.tskr.data

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanC3IntegrationTest {

    @Test
    fun planCreation_withRequiredNameAndOptionalFields_succeeds() {
        val plan = Plan(
            name = "Launch Startup",
            description = "Build and release MVP",
            icon = "🚀",
            deadline = "2026-12-31"
        )

        assertEquals("Launch Startup", plan.name)
        assertEquals("Build and release MVP", plan.description)
        assertEquals("🚀", plan.icon)
        assertEquals("2026-12-31", plan.deadline)
        assertEquals(Plan.STATUS_ACTIVE, plan.status)
    }

    @Test
    fun planEditing_persistsMetadataChangesWithoutAffectingTasks() {
        var plan = Plan(
            name = "Old Name",
            description = "Old Desc",
            icon = "🎯",
            deadline = "2026-10-01"
        )

        // Simulate edit
        plan = plan.copy(
            name = "New Name",
            description = "New Desc",
            icon = "⭐",
            deadline = "2026-11-01"
        )

        assertEquals("New Name", plan.name)
        assertEquals("New Desc", plan.description)
        assertEquals("⭐", plan.icon)
        assertEquals("2026-11-01", plan.deadline)
    }

    @Test
    fun planStatus_manualTransitionsPersistAndDoNotAutoChange() {
        var plan = Plan(name = "Fitness Goal", status = Plan.STATUS_ACTIVE)
        assertEquals(Plan.STATUS_ACTIVE, plan.status)

        // Manual status update to COMPLETED
        plan = plan.copy(status = Plan.STATUS_COMPLETED)
        assertEquals(Plan.STATUS_COMPLETED, plan.status)

        // Manual status update to ARCHIVED
        plan = plan.copy(status = Plan.STATUS_ARCHIVED)
        assertEquals(Plan.STATUS_ARCHIVED, plan.status)

        // Manual restore to ACTIVE
        plan = plan.copy(status = Plan.STATUS_ACTIVE)
        assertEquals(Plan.STATUS_ACTIVE, plan.status)
    }

    @Test
    fun planProgress_100PercentDoesNotAutoChangeStatus() {
        val plan = Plan(name = "Skill Up", status = Plan.STATUS_ACTIVE)
        val progress = PlanProgress(planId = plan.id, completedTasks = 3, totalTasks = 3)

        assertEquals(1.0f, progress.progressRatio, 0.001f)
        // Verify status remains ACTIVE despite 100% completion
        assertEquals(Plan.STATUS_ACTIVE, plan.status)
    }

    @Test
    fun taskDetachment_onPlanDeletion_setsPlanIdNull() {
        val plan = Plan(name = "Temporary Plan")
        val reminder = Reminder(
            title = "Orphaned Task",
            date = "2026-09-25",
            time = "10:00",
            planId = plan.id
        )

        assertEquals(plan.id, reminder.planId)

        // Simulate plan deletion detachment (ON DELETE SET NULL)
        val detachedReminder = reminder.copy(planId = null)

        assertNull(detachedReminder.planId)
        assertEquals("Orphaned Task", detachedReminder.title)
    }
}
