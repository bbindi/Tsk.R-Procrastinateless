package com.brixavier.tskr.personality

import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.scheduler.ReminderScheduler
import org.junit.Assert.assertEquals
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
}
