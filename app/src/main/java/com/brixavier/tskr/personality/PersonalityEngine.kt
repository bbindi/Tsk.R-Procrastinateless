package com.brixavier.tskr.personality

interface MessageRotationStore {
    fun getIndex(poolId: String): Int
    fun setIndex(poolId: String, index: Int)
    fun getOrder(poolId: String): List<Int>?
    fun setOrder(poolId: String, order: List<Int>)
}

interface PersonalityRandomizer {
    fun <T> shuffle(list: List<T>): List<T>
}

class DefaultPersonalityRandomizer : PersonalityRandomizer {
    override fun <T> shuffle(list: List<T>): List<T> = list.shuffled()
}

/**
 * Minimal in-memory storage abstraction.
 * Production phases may provide a SharedPreferences or DataStore backed implementation.
 */
class InMemoryRotationStore : MessageRotationStore {
    private val indices = mutableMapOf<String, Int>()
    private val orders = mutableMapOf<String, List<Int>>()
    
    override fun getIndex(poolId: String): Int = indices[poolId] ?: 0
    override fun setIndex(poolId: String, index: Int) { indices[poolId] = index }
    override fun getOrder(poolId: String): List<Int>? = orders[poolId]
    override fun setOrder(poolId: String, order: List<Int>) { orders[poolId] = order }
}

class PersonalityEngine(
    private val store: MessageRotationStore = InMemoryRotationStore(),
    private val randomizer: PersonalityRandomizer = DefaultPersonalityRandomizer()
) {
    fun getMessage(context: PersonalityContext): PersonalityContent {
        return try {
            val poolId = selectPool(context)
            val messages = MessagePools.getPool(poolId)
            
            if (messages.isEmpty()) {
                return PersonalityContent(MessagePools.FALLBACK_MESSAGE)
            }

            var order = store.getOrder(poolId)
            if (order == null || order.size != messages.size) {
                order = randomizer.shuffle(messages.indices.toList())
                store.setOrder(poolId, order)
                store.setIndex(poolId, 0)
            }

            var index = store.getIndex(poolId)
            if (index >= order.size) {
                order = randomizer.shuffle(messages.indices.toList())
                store.setOrder(poolId, order)
                index = 0
            }

            val messageIndex = order[index]
            store.setIndex(poolId, index + 1)

            if (messageIndex in messages.indices) {
                PersonalityContent(messages[messageIndex])
            } else {
                PersonalityContent(MessagePools.FALLBACK_MESSAGE)
            }
        } catch (e: Exception) {
            PersonalityContent(MessagePools.FALLBACK_MESSAGE)
        }
    }

    private fun selectPool(context: PersonalityContext): String {
        return when (context.actionType) {
            ActionType.ONBOARDING -> MessagePools.ONBOARDING
            ActionType.TASK_CREATED -> MessagePools.TASK_CREATED
            ActionType.TASK_UPDATED -> MessagePools.TASK_UPDATED
            ActionType.TASK_DELETED -> MessagePools.TASK_DELETED
            ActionType.TASK_RESTORED -> MessagePools.TASK_RESTORED
            ActionType.TASK_APPROACHING -> MessagePools.TASK_APPROACHING
            ActionType.TASK_DUE -> if (context.urgency == UrgencyLevel.IMPORTANT) MessagePools.TASK_DUE_IMPORTANT else MessagePools.TASK_DUE_NORMAL
            ActionType.TASK_OVERDUE -> if (context.overdueMinutes > 60) MessagePools.TASK_OVERDUE_LATE else MessagePools.TASK_OVERDUE_EARLY
            ActionType.TASK_SNOOZED -> MessagePools.TASK_SNOOZED
            ActionType.COMPLETION -> {
                if (context.streak >= 3) MessagePools.COMPLETION_STREAK
                else if (context.isRecurring) MessagePools.COMPLETION_RECURRING
                else MessagePools.COMPLETION_NORMAL
            }
            ActionType.EMPTY_STATE -> {
                if (context.isAllCaughtUp) MessagePools.EMPTY_STATE_ALL_CAUGHT_UP
                else MessagePools.EMPTY_STATE_NO_TASKS
            }
            ActionType.MISSION_MODE_WARNING -> MessagePools.MISSION_MODE_WARNING
            ActionType.MISSION_MODE_DUE -> MessagePools.MISSION_MODE_DUE
            ActionType.MISSION_MODE_COMPLETION -> MessagePools.MISSION_MODE_COMPLETION
            ActionType.WIDGET -> MessagePools.WIDGET
            ActionType.PLAN_CREATED -> MessagePools.PLAN_CREATED
            ActionType.PLAN_NO_TASKS -> MessagePools.PLAN_NO_TASKS
            ActionType.PLAN_PROGRESS_MILESTONE -> {
                when {
                    context.planProgressRatio >= 1.0f -> MessagePools.PLAN_PROGRESS_COMPLETE
                    context.planProgressRatio >= 0.5f -> MessagePools.PLAN_PROGRESS_MID
                    else -> MessagePools.PLAN_PROGRESS_START
                }
            }
            ActionType.PLAN_DEADLINE -> {
                val days = context.planDaysUntilDeadline ?: 999
                when {
                    days < 0 -> MessagePools.PLAN_DEADLINE_PASSED
                    days == 0 -> MessagePools.PLAN_DEADLINE_TODAY
                    else -> MessagePools.PLAN_DEADLINE_APPROACHING
                }
            }
            ActionType.PLAN_COMPLETED -> MessagePools.PLAN_COMPLETED
            ActionType.PLAN_ARCHIVED -> MessagePools.PLAN_ARCHIVED
            ActionType.PLAN_REOPENED -> MessagePools.PLAN_REOPENED
        }
    }
}
