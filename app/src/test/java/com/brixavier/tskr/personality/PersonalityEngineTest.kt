package com.brixavier.tskr.personality

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
}
