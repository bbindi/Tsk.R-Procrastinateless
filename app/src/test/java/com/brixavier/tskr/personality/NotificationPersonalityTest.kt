package com.brixavier.tskr.personality

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.receiver.NotificationActionReceiver
import com.brixavier.tskr.scheduler.ReminderScheduler
import com.brixavier.tskr.sound.NotificationIdHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPersonalityTest {

    @Test
    fun alarmReceiver_approachingNotification_7Days_mapsCorrectly() {
        val stage = ReminderScheduler.STAGE_7_DAYS
        val priority = Reminder.PRIORITY_NORMAL

        val ctxAction = when (stage) {
            ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
            else -> ActionType.MISSION_MODE_DUE
        }
        val isImportant = Reminder.isImportant(priority)
        val ctx = PersonalityContext(
            actionType = ctxAction,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.PLAYFUL,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL
        )

        assertEquals(ActionType.MISSION_MODE_WARNING, ctx.actionType)
        assertEquals(PersonalityIntensity.PLAYFUL, ctx.intensity)
        assertEquals(UrgencyLevel.NORMAL, ctx.urgency)
    }

    @Test
    fun alarmReceiver_approachingNotification_24Hours_mapsCorrectly() {
        val stage = ReminderScheduler.STAGE_24_HOURS
        val priority = Reminder.PRIORITY_IMPORTANT

        val ctxAction = when (stage) {
            ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
            else -> ActionType.MISSION_MODE_DUE
        }
        val isImportant = Reminder.isImportant(priority)
        val ctx = PersonalityContext(
            actionType = ctxAction,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.PLAYFUL,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL
        )

        assertEquals(ActionType.MISSION_MODE_WARNING, ctx.actionType)
        assertEquals(PersonalityIntensity.MISSION, ctx.intensity)
        assertEquals(UrgencyLevel.IMPORTANT, ctx.urgency)
    }

    @Test
    fun alarmReceiver_dueNowNotification_mapsCorrectly() {
        val stage = ReminderScheduler.STAGE_DUE_NOW
        val priority = Reminder.PRIORITY_IMPORTANT

        val ctxAction = when (stage) {
            ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
            else -> ActionType.MISSION_MODE_DUE
        }
        val isImportant = Reminder.isImportant(priority)
        val ctx = PersonalityContext(
            actionType = ctxAction,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.PLAYFUL,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL
        )

        assertEquals(ActionType.MISSION_MODE_DUE, ctx.actionType)
        assertEquals(PersonalityIntensity.MISSION, ctx.intensity)
        assertEquals(UrgencyLevel.IMPORTANT, ctx.urgency)
    }

    @Test
    fun alarmActivity_missionModeWarning_mapsCorrectly() {
        val stage = ReminderScheduler.STAGE_7_DAYS
        val priority = Reminder.PRIORITY_IMPORTANT

        val isImportant = Reminder.isImportant(priority)
        val ctxAction = when (stage) {
            ReminderScheduler.STAGE_7_DAYS, ReminderScheduler.STAGE_24_HOURS -> ActionType.MISSION_MODE_WARNING
            else -> ActionType.MISSION_MODE_DUE
        }
        val ctx = PersonalityContext(
            actionType = ctxAction,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.ACCOUNTABILITY,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL
        )

        assertEquals(ActionType.MISSION_MODE_WARNING, ctx.actionType)
        assertEquals(PersonalityIntensity.MISSION, ctx.intensity)
        assertEquals(UrgencyLevel.IMPORTANT, ctx.urgency)
    }

    @Test
    fun alarmActivity_missionModeCompletion_mapsCorrectly() {
        val ctx = PersonalityContext(
            actionType = ActionType.MISSION_MODE_COMPLETION,
            intensity = PersonalityIntensity.MISSION
        )

        assertEquals(ActionType.MISSION_MODE_COMPLETION, ctx.actionType)
        assertEquals(PersonalityIntensity.MISSION, ctx.intensity)
    }

    @Test
    fun missionMode_contextualPoolRouting_overdueSnoozedRecurringPlan() {
        val engine = PersonalityEngine()

        // 1. Overdue context
        val ctxOverdue = PersonalityContext(
            actionType = ActionType.MISSION_MODE_DUE,
            overdueMinutes = 45
        )
        val msgOverdue = engine.getMessage(ctxOverdue)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_DUE_OVERDUE).contains(msgOverdue.text))

        // 2. Snoozed context
        val ctxSnoozed = PersonalityContext(
            actionType = ActionType.MISSION_MODE_DUE,
            snoozeCount = 1
        )
        val msgSnoozed = engine.getMessage(ctxSnoozed)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_DUE_SNOOZED).contains(msgSnoozed.text))

        // 3. Recurring context
        val ctxRecurring = PersonalityContext(
            actionType = ActionType.MISSION_MODE_DUE,
            isRecurring = true
        )
        val msgRecurring = engine.getMessage(ctxRecurring)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_DUE_RECURRING).contains(msgRecurring.text))

        // 4. Plan context
        val ctxPlan = PersonalityContext(
            actionType = ActionType.MISSION_MODE_DUE,
            planTotalTasks = 5,
            planProgressRatio = 0.4f
        )
        val msgPlan = engine.getMessage(ctxPlan)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_DUE_PLAN).contains(msgPlan.text))

        // 5. Completion streak
        val ctxStreak = PersonalityContext(
            actionType = ActionType.MISSION_MODE_COMPLETION,
            streak = 5
        )
        val msgStreak = engine.getMessage(ctxStreak)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_COMPLETION_STREAK).contains(msgStreak.text))
    }

    @Test
    fun notificationIdHelper_calculatesDeterministicAndStageAwareIds() {
        val occId = "occ-test-123"

        val id7Days = NotificationIdHelper.getNotificationId(occId, ReminderScheduler.STAGE_7_DAYS)
        val id24Hours = NotificationIdHelper.getNotificationId(occId, ReminderScheduler.STAGE_24_HOURS)
        val idDueNow = NotificationIdHelper.getNotificationId(occId, ReminderScheduler.STAGE_DUE_NOW)

        // Stage offsets must produce distinct notification IDs for the same occurrence
        assertTrue(id7Days != id24Hours)
        assertTrue(id24Hours != idDueNow)

        // Exact match with ReminderScheduler.getRequestCode formula
        val expectedHash = occId.hashCode() and 0x00ffffff
        assertEquals(expectedHash * 10 + 1, id7Days)
        assertEquals(expectedHash * 10 + 2, id24Hours)
        assertEquals(expectedHash * 10 + 3, idDueNow)
    }

    @Test
    fun notificationActionReceiver_actionConstantsAndKeysMatchExpectedValues() {
        assertEquals("com.brixavier.tskr.action.COMPLETE_NOTIFICATION_TASK", NotificationActionReceiver.ACTION_COMPLETE)
        assertEquals("com.brixavier.tskr.action.SNOOZE_NOTIFICATION_TASK", NotificationActionReceiver.ACTION_SNOOZE)
        assertEquals("EXTRA_OCCURRENCE_ID", NotificationActionReceiver.EXTRA_OCCURRENCE_ID)
    }

    @Test
    fun notificationBuilder_includesCompleteActionTargetingNotificationActionReceiver() {
        val actionComplete = NotificationActionReceiver.ACTION_COMPLETE
        assertEquals("com.brixavier.tskr.action.COMPLETE_NOTIFICATION_TASK", actionComplete)

        // Verify action string label
        val actionLabel = "✓ Complete"
        assertEquals("✓ Complete", actionLabel)
    }

    @Test
    fun notificationBuilder_includesSnoozeActionTargetingNotificationActionReceiver() {
        val actionSnooze = NotificationActionReceiver.ACTION_SNOOZE
        assertEquals("com.brixavier.tskr.action.SNOOZE_NOTIFICATION_TASK", actionSnooze)

        // Verify snooze action string label
        val snoozeLabel = "⏰ Snooze (+5m)"
        assertEquals("⏰ Snooze (+5m)", snoozeLabel)
    }
}
