package com.brixavier.tskr.personality

import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonalityEngineTest {

    private lateinit var engine: PersonalityEngine
    private lateinit var store: InMemoryRotationStore
    private lateinit var randomizer: TestRandomizer

    class TestRandomizer : PersonalityRandomizer {
        // Reverses the list instead of randomizing, making it perfectly predictable for tests
        override fun <T> shuffle(list: List<T>): List<T> = list.reversed()
    }

    @Before
    fun setup() {
        store = InMemoryRotationStore()
        randomizer = TestRandomizer()
        engine = PersonalityEngine(store, randomizer)
    }

    @Test
    fun actionType_routesToCorrectPools() {
        val ctxOnboarding = PersonalityContext(actionType = ActionType.ONBOARDING)
        val msgOnboarding = engine.getMessage(ctxOnboarding)
        assertTrue(MessagePools.getPool(MessagePools.ONBOARDING).contains(msgOnboarding.text))

        val ctxCreated = PersonalityContext(actionType = ActionType.TASK_CREATED)
        val msgCreated = engine.getMessage(ctxCreated)
        assertTrue(MessagePools.getPool(MessagePools.TASK_CREATED).contains(msgCreated.text))
    }

    @Test
    fun urgency_modifiesDuePoolSelection() {
        val ctxNormal = PersonalityContext(actionType = ActionType.TASK_DUE, urgency = UrgencyLevel.NORMAL)
        val msgNormal = engine.getMessage(ctxNormal)
        assertTrue(MessagePools.getPool(MessagePools.TASK_DUE_NORMAL).contains(msgNormal.text))

        val ctxImportant = PersonalityContext(actionType = ActionType.TASK_DUE, urgency = UrgencyLevel.IMPORTANT)
        val msgImportant = engine.getMessage(ctxImportant)
        assertTrue(MessagePools.getPool(MessagePools.TASK_DUE_IMPORTANT).contains(msgImportant.text))
    }

    @Test
    fun completionContext_modifiesCompletionPoolSelection() {
        val ctxNormal = PersonalityContext(actionType = ActionType.COMPLETION)
        val msgNormal = engine.getMessage(ctxNormal)
        assertTrue(MessagePools.getPool(MessagePools.COMPLETION_NORMAL).contains(msgNormal.text))

        val ctxRecurring = PersonalityContext(actionType = ActionType.COMPLETION, isRecurring = true)
        val msgRecurring = engine.getMessage(ctxRecurring)
        assertTrue(MessagePools.getPool(MessagePools.COMPLETION_RECURRING).contains(msgRecurring.text))

        val ctxStreak = PersonalityContext(actionType = ActionType.COMPLETION, streak = 5)
        val msgStreak = engine.getMessage(ctxStreak)
        assertTrue(MessagePools.getPool(MessagePools.COMPLETION_STREAK).contains(msgStreak.text))
        
        // Streak overrides recurring
        val ctxStreakRec = PersonalityContext(actionType = ActionType.COMPLETION, isRecurring = true, streak = 4)
        val msgStreakRec = engine.getMessage(ctxStreakRec)
        assertTrue(MessagePools.getPool(MessagePools.COMPLETION_STREAK).contains(msgStreakRec.text))
    }

    @Test
    fun overdueContext_modifiesOverduePoolSelection() {
        val ctxEarly = PersonalityContext(actionType = ActionType.TASK_OVERDUE, overdueMinutes = 30)
        val msgEarly = engine.getMessage(ctxEarly)
        assertTrue(MessagePools.getPool(MessagePools.TASK_OVERDUE_EARLY).contains(msgEarly.text))

        val ctxLate = PersonalityContext(actionType = ActionType.TASK_OVERDUE, overdueMinutes = 120)
        val msgLate = engine.getMessage(ctxLate)
        assertTrue(MessagePools.getPool(MessagePools.TASK_OVERDUE_LATE).contains(msgLate.text))
    }

    @Test
    fun missionMode_routesToMissionPools() {
        val ctxWarning = PersonalityContext(actionType = ActionType.MISSION_MODE_WARNING)
        val msgWarning = engine.getMessage(ctxWarning)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_WARNING).contains(msgWarning.text))

        val ctxDue = PersonalityContext(actionType = ActionType.MISSION_MODE_DUE)
        val msgDue = engine.getMessage(ctxDue)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_DUE).contains(msgDue.text))

        val ctxComplete = PersonalityContext(actionType = ActionType.MISSION_MODE_COMPLETION)
        val msgComplete = engine.getMessage(ctxComplete)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_COMPLETION).contains(msgComplete.text))
    }

    @Test
    fun emptyState_routesToCorrectPools() {
        val ctxNoTasks = PersonalityContext(actionType = ActionType.EMPTY_STATE, isAllCaughtUp = false)
        val msgNoTasks = engine.getMessage(ctxNoTasks)
        assertTrue(MessagePools.getPool(MessagePools.EMPTY_STATE_NO_TASKS).contains(msgNoTasks.text))

        val ctxAllCaughtUp = PersonalityContext(actionType = ActionType.EMPTY_STATE, isAllCaughtUp = true)
        val msgAllCaughtUp = engine.getMessage(ctxAllCaughtUp)
        assertTrue(MessagePools.getPool(MessagePools.EMPTY_STATE_ALL_CAUGHT_UP).contains(msgAllCaughtUp.text))
    }

    @Test
    fun rotation_doesNotImmediatelyRepeat() {
        val ctx = PersonalityContext(actionType = ActionType.TASK_CREATED)
        val poolSize = MessagePools.getPool(MessagePools.TASK_CREATED).size
        
        val seenMessages = mutableSetOf<String>()
        for (i in 0 until poolSize) {
            val msg = engine.getMessage(ctx).text
            assertTrue("Message '\$msg' repeated before exhausting pool", seenMessages.add(msg))
        }

        assertEquals("Did not see all distinct messages", poolSize, seenMessages.size)

        // Next one should reshuffle and start over, so it should be one of the original messages
        val nextMsg = engine.getMessage(ctx).text
        assertTrue(seenMessages.contains(nextMsg))
    }

    @Test
    fun failSafe_returnsFallbackMessageWhenExceptionOccurs() {
        // Force an exception by breaking the store
        val brokenStore = object : MessageRotationStore {
            override fun getIndex(poolId: String): Int = throw RuntimeException("Simulated failure")
            override fun setIndex(poolId: String, index: Int) {}
            override fun getOrder(poolId: String): List<Int>? = null
            override fun setOrder(poolId: String, order: List<Int>) {}
        }
        val safeEngine = PersonalityEngine(brokenStore, randomizer)
        
        val msg = safeEngine.getMessage(PersonalityContext(actionType = ActionType.TASK_CREATED))
        assertEquals(MessagePools.FALLBACK_MESSAGE, msg.text)
    }

    @Test
    fun allMessagePools_areNonEmptyAndContainValidText() {
        val poolIds = listOf(
            MessagePools.ONBOARDING, MessagePools.TASK_CREATED, MessagePools.TASK_UPDATED,
            MessagePools.TASK_DELETED, MessagePools.TASK_RESTORED, MessagePools.TASK_APPROACHING,
            MessagePools.TASK_DUE_NORMAL, MessagePools.TASK_DUE_IMPORTANT, MessagePools.TASK_OVERDUE_EARLY,
            MessagePools.TASK_OVERDUE_LATE, MessagePools.TASK_SNOOZED, MessagePools.COMPLETION_NORMAL,
            MessagePools.COMPLETION_RECURRING, MessagePools.COMPLETION_STREAK, MessagePools.EMPTY_STATE_NO_TASKS,
            MessagePools.EMPTY_STATE_ALL_CAUGHT_UP, MessagePools.MISSION_MODE_WARNING, MessagePools.MISSION_MODE_WARNING_PLAN,
            MessagePools.MISSION_MODE_DUE, MessagePools.MISSION_MODE_DUE_OVERDUE, MessagePools.MISSION_MODE_DUE_SNOOZED,
            MessagePools.MISSION_MODE_DUE_RECURRING, MessagePools.MISSION_MODE_DUE_PLAN, MessagePools.MISSION_MODE_COMPLETION,
            MessagePools.MISSION_MODE_COMPLETION_STREAK, MessagePools.MISSION_MODE_COMPLETION_RECURRING, MessagePools.WIDGET,
            MessagePools.PLAN_CREATED, MessagePools.PLAN_NO_TASKS, MessagePools.PLAN_PROGRESS_START,
            MessagePools.PLAN_PROGRESS_MID, MessagePools.PLAN_PROGRESS_COMPLETE, MessagePools.PLAN_DEADLINE_APPROACHING,
            MessagePools.PLAN_DEADLINE_TODAY, MessagePools.PLAN_DEADLINE_PASSED, MessagePools.PLAN_COMPLETED,
            MessagePools.PLAN_ARCHIVED, MessagePools.PLAN_REOPENED
        )

        assertEquals(38, poolIds.size)

        for (poolId in poolIds) {
            val pool = MessagePools.getPool(poolId)
            assertTrue("Pool '$poolId' must contain at least 3 messages", pool.size >= 3)
            for (msg in pool) {
                assertTrue("Message in '$poolId' must not be blank", msg.isNotBlank())
            }
        }
    }

    @Test
    fun behavioralContextResolver_derivesEarlyCompletionCorrectly() {
        val futureDate = "2026-10-15"
        val futureTime = "14:00"

        val earlyOcc = ReminderOccurrence(
            id = "occ-early",
            title = "Submit Paper",
            date = futureDate,
            time = futureTime,
            status = ReminderOccurrence.STATUS_COMPLETED,
            completedAtMs = System.currentTimeMillis() // Completed now, long before 2026-10-15!
        )

        assertTrue(BehavioralContextResolver.isEarlyCompletion(earlyOcc))

        // Snoozed occurrence suppresses early completion
        val snoozedOcc = earlyOcc.copy(snoozedUntilMs = System.currentTimeMillis() + 300000)
        Assert.assertFalse(BehavioralContextResolver.isEarlyCompletion(snoozedOcc))
    }

    @Test
    fun behavioralContextResolver_calculatesOverdueMinutesCorrectly() {
        val overdueMinutes = BehavioralContextResolver.calculateOverdueMinutes(
            dateStr = "2026-09-25",
            timeStr = "10:00",
            nowStr = "2026-09-25 11:30"
        )
        assertEquals(90, overdueMinutes)
    }

    @Test
    fun behavioralContextResolver_resolvesContextCorrectly() {
        val occ = ReminderOccurrence(
            id = "occ-1",
            title = "Test Task",
            date = "2026-09-25",
            time = "10:00",
            snoozeCount = 2
        )

        val ctx = BehavioralContextResolver.resolveContext(
            actionType = ActionType.TASK_DUE,
            occurrence = occ,
            reminder = null,
            streak = 4
        )

        assertEquals(2, ctx.snoozeCount)
        assertEquals(4, ctx.streak)
        assertEquals(ActionType.TASK_DUE, ctx.actionType)
    }

    @Test
    fun taskCreated_lateNightHour_routesToLateNightPool() {
        val lateNightCtx = PersonalityContext(
            actionType = ActionType.TASK_CREATED,
            timeOfDayHour = 2 // 2 AM
        )
        val lateNightMsg = engine.getMessage(lateNightCtx)
        assertTrue(MessagePools.getPool(MessagePools.TASK_CREATED_LATE_NIGHT).contains(lateNightMsg.text))

        val daytimeCtx = PersonalityContext(
            actionType = ActionType.TASK_CREATED,
            timeOfDayHour = 14 // 2 PM
        )
        val daytimeMsg = engine.getMessage(daytimeCtx)
        assertTrue(MessagePools.getPool(MessagePools.TASK_CREATED).contains(daytimeMsg.text))
    }

    @Test
    fun completion_earlyCompletion_takesPrecedenceAndRoutesToEarlyPool() {
        val earlyCtx = PersonalityContext(
            actionType = ActionType.COMPLETION,
            isEarlyCompletion = true,
            streak = 5,
            isRecurring = true
        )
        val earlyMsg = engine.getMessage(earlyCtx)
        assertTrue(MessagePools.getPool(MessagePools.COMPLETION_EARLY).contains(earlyMsg.text))

        val missionEarlyCtx = PersonalityContext(
            actionType = ActionType.MISSION_MODE_COMPLETION,
            isEarlyCompletion = true,
            streak = 5
        )
        val missionEarlyMsg = engine.getMessage(missionEarlyCtx)
        assertTrue(MessagePools.getPool(MessagePools.MISSION_MODE_COMPLETION_EARLY).contains(missionEarlyMsg.text))
    }

    @Test
    fun taskSnoozed_repeatedSnoozeCount_routesToRepeatedSnoozePools() {
        val normalSnoozeCtx = PersonalityContext(
            actionType = ActionType.TASK_SNOOZED,
            snoozeCount = 1
        )
        val normalMsg = engine.getMessage(normalSnoozeCtx)
        assertTrue(MessagePools.getPool(MessagePools.TASK_SNOOZED).contains(normalMsg.text))

        val repeatedSnoozeCtx = PersonalityContext(
            actionType = ActionType.TASK_SNOOZED,
            snoozeCount = 2
        )
        val repeatedMsg = engine.getMessage(repeatedSnoozeCtx)
        assertTrue(MessagePools.getPool(MessagePools.TASK_SNOOZED_REPEATED).contains(repeatedMsg.text))

        val highSnoozeCtx = PersonalityContext(
            actionType = ActionType.TASK_SNOOZED,
            snoozeCount = 3
        )
        val highMsg = engine.getMessage(highSnoozeCtx)
        assertTrue(MessagePools.getPool(MessagePools.TASK_SNOOZED_REPEATED_HIGH).contains(highMsg.text))
    }
}
