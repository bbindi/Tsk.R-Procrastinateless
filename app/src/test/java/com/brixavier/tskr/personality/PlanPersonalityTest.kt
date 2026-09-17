package com.brixavier.tskr.personality

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlanPersonalityTest {

    private val engine = PersonalityEngine()

    @Test
    fun testPlanCreatedPersonality() {
        val context = PersonalityContext(actionType = ActionType.PLAN_CREATED)
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_CREATED)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanNoTasksPersonality() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_NO_TASKS,
            planTotalTasks = 0
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_NO_TASKS)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanProgressStartPersonality() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_PROGRESS_MILESTONE,
            planProgressRatio = 0.25f,
            planTotalTasks = 4,
            planCompletedTasks = 1
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_PROGRESS_START)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanProgressMidPersonality() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_PROGRESS_MILESTONE,
            planProgressRatio = 0.5f,
            planTotalTasks = 4,
            planCompletedTasks = 2
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_PROGRESS_MID)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanProgressCompletePersonality() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_PROGRESS_MILESTONE,
            planProgressRatio = 1.0f,
            planTotalTasks = 4,
            planCompletedTasks = 4
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_PROGRESS_COMPLETE)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanDeadlineApproaching() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_DEADLINE,
            planDaysUntilDeadline = 2
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_DEADLINE_APPROACHING)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanDeadlineToday() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_DEADLINE,
            planDaysUntilDeadline = 0
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_DEADLINE_TODAY)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanDeadlinePassed() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_DEADLINE,
            planDaysUntilDeadline = -2
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_DEADLINE_PASSED)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanCompletedStatus() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_COMPLETED,
            planStatus = PlanStatus.COMPLETED
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_COMPLETED)
        Assert.assertTrue(messages.contains(content.text))
    }

    @Test
    fun testPlanArchivedStatus() {
        val context = PersonalityContext(
            actionType = ActionType.PLAN_ARCHIVED,
            planStatus = PlanStatus.ARCHIVED
        )
        val content = engine.getMessage(context)
        assertNotNull(content.text)
        val messages = MessagePools.getPool(MessagePools.PLAN_ARCHIVED)
        Assert.assertTrue(messages.contains(content.text))
    }
}
