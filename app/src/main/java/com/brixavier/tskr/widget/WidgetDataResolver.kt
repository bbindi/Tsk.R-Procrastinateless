package com.brixavier.tskr.widget

import com.brixavier.tskr.data.ReminderDatabase
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.BehavioralContextResolver
import com.brixavier.tskr.personality.MessagePools
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityEngine
import com.brixavier.tskr.personality.PersonalityIntensity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round

/**
 * Presentation model for task items displayed in the Glance widget.
 * Carries occurrenceId for occurrence-level action targeting.
 */
data class WidgetTaskItem(
    val reminderId: String,
    val occurrenceId: String,
    val title: String,
    val date: String,
    val time: String,
    val priority: String,
    val isRecurring: Boolean,
    val planId: String? = null,
    val planName: String? = null,
    val planIcon: String? = null
) {
    fun isImportant(): Boolean = Reminder.isImportant(priority)

    fun getPriorityEmoji(): String = if (isImportant()) "🔥" else "🙂"
}

/**
 * Representation of the Widget UI state for rendering.
 */
data class WidgetUiState(
    val pendingTasks: List<WidgetTaskItem> = emptyList(),
    val overdueCount: Int = 0,
    val totalPendingCount: Int = 0,
    val personalityQuote: String = "Stay focused. Get your tasks done today.",
    val activePlanName: String? = null,
    val activePlanIcon: String? = null,
    val activePlanTotalTasks: Int = 0,
    val activePlanCompletedTasks: Int = 0
) {
    fun hasActivePlan(): Boolean = !activePlanName.isNullOrBlank() && activePlanTotalTasks > 0

    fun getPlanProgressPercent(): Int {
        if (activePlanTotalTasks <= 0) return 0
        return round((activePlanCompletedTasks.toFloat() / activePlanTotalTasks.toFloat()) * 100).toInt()
    }
}

/**
 * Read-only resolver that constructs occurrence-aware widget presentation state from Room.
 */
object WidgetDataResolver {

    private val completingOccurrenceIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun markCompleting(occurrenceId: String): Boolean {
        return completingOccurrenceIds.add(occurrenceId)
    }

    fun clearCompleting(occurrenceId: String) {
        completingOccurrenceIds.remove(occurrenceId)
    }

    fun isCompleting(occurrenceId: String): Boolean {
        return completingOccurrenceIds.contains(occurrenceId)
    }

    fun clearAllCompleting() {
        completingOccurrenceIds.clear()
    }

    suspend fun resolveWidgetState(
        database: ReminderDatabase,
        limit: Int = 3,
        nowStr: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    ): WidgetUiState {
        val pendingOccurrences = database.reminderDao().getPendingOccurrences()
        val allReminders = database.reminderDao().getUpcomingReminders()
        val templateMap = allReminders.associateBy { it.id }

        val items = mapPendingOccurrencesToWidgetItems(pendingOccurrences, templateMap, emptyMap(), nowStr)

        val totalPending = items.size
        val overdueItems = items.filter { "${it.date} ${it.time}" < nowStr }
        val overdueCount = overdueItems.size
        val oldestOverdueMinutes = overdueItems.maxOfOrNull { item ->
            BehavioralContextResolver.calculateOverdueMinutes(item.date, item.time, nowStr)
        } ?: 0
        val hasImportant = items.any { it.isImportant() }

        val limitedItems = items.take(limit)

        // Construct PersonalityContext using ActionType.WIDGET
        val intensity = when {
            overdueCount > 0 || hasImportant -> PersonalityIntensity.ACCOUNTABILITY
            items.isEmpty() -> PersonalityIntensity.FRIENDLY
            else -> PersonalityIntensity.PLAYFUL
        }

        val personalityContext = PersonalityContext(
            actionType = ActionType.WIDGET,
            intensity = intensity,
            overdueMinutes = oldestOverdueMinutes,
            oldestOverdueMinutes = oldestOverdueMinutes,
            isAllCaughtUp = items.isEmpty()
        )

        val nowDateStr = nowStr.substringBefore(" ")
        val quote = getStableWidgetPersonalityQuote(personalityContext, nowDateStr)

        // Resolve active Plan context if a top task is assigned to a plan
        var activePlanName: String? = null
        var activePlanIcon: String? = null
        var activePlanTotal = 0
        var activePlanCompleted = 0

        val topPlanId = items.firstOrNull { it.planId != null }?.planId
        if (topPlanId != null) {
            val plan = database.reminderDao().getPlanById(topPlanId)
            if (plan != null) {
                val progress = database.reminderDao().getPlanProgress(plan.id)
                activePlanName = plan.name
                activePlanIcon = plan.icon
                activePlanTotal = progress.totalTasks
                activePlanCompleted = progress.completedTasks
            }
        }

        return WidgetUiState(
            pendingTasks = limitedItems,
            overdueCount = overdueCount,
            totalPendingCount = totalPending,
            personalityQuote = quote,
            activePlanName = activePlanName,
            activePlanIcon = activePlanIcon,
            activePlanTotalTasks = activePlanTotal,
            activePlanCompletedTasks = activePlanCompleted
        )
    }

    /**
     * Constructs a deterministic, stable personality quote for the widget to prevent flickering across recompositions.
     */
    fun getStableWidgetPersonalityQuote(
        context: PersonalityContext,
        dateSeed: String
    ): String {
        val poolId = when {
            context.isAllCaughtUp -> MessagePools.EMPTY_STATE_ALL_CAUGHT_UP
            context.oldestOverdueMinutes > 60 || context.overdueMinutes > 60 -> MessagePools.WIDGET_OVERDUE_LATE
            context.oldestOverdueMinutes > 0 || context.overdueMinutes > 0 -> MessagePools.WIDGET_OVERDUE
            context.intensity == PersonalityIntensity.ACCOUNTABILITY -> MessagePools.TASK_DUE_IMPORTANT
            else -> MessagePools.WIDGET
        }
        val messages = MessagePools.getPool(poolId)
        if (messages.isEmpty()) return MessagePools.FALLBACK_MESSAGE

        val seed = "$dateSeed-${context.intensity.name}-${context.isAllCaughtUp}-${context.oldestOverdueMinutes > 0}"
        val hash = Math.abs(seed.hashCode())
        val index = hash % messages.size
        return messages[index]
    }

    /**
     * Maps pending occurrence records to WidgetTaskItems.
     * Uses pendingOccurrence.date & pendingOccurrence.time for current scheduling.
     */
    fun mapPendingOccurrencesToWidgetItems(
        pendingOccurrences: List<ReminderOccurrence>,
        templateMap: Map<String, Reminder>,
        planMap: Map<String, Plan> = emptyMap(),
        nowStr: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    ): List<WidgetTaskItem> {
        val widgetItems = pendingOccurrences.mapNotNull { occ ->
            if (isCompleting(occ.id)) return@mapNotNull null

            val tmpl = occ.templateId?.let { templateMap[it] }
            if (tmpl == null || !tmpl.isActive) return@mapNotNull null

            val plan = occ.planId?.let { planMap[it] } ?: tmpl.planId?.let { planMap[it] }

            WidgetTaskItem(
                reminderId = tmpl.id,
                occurrenceId = occ.id,
                title = occ.title,
                date = occ.date, // AUTHORITATIVE CURRENT PENDING OCCURRENCE DATE
                time = occ.time, // AUTHORITATIVE CURRENT PENDING OCCURRENCE TIME
                priority = occ.priority,
                isRecurring = tmpl.isRecurring(),
                planId = plan?.id,
                planName = plan?.name,
                planIcon = plan?.icon
            )
        }

        // Sort chronologically by occurrence date & time, placing overdue tasks first
        return widgetItems.sortedWith(
            compareBy<WidgetTaskItem> { item ->
                val isOverdue = "${item.date} ${item.time}" < nowStr
                if (isOverdue) 0 else 1
            }.thenBy { item ->
                if (item.isImportant()) 0 else 1
            }.thenBy { item ->
                "${item.date} ${item.time}"
            }
        )
    }
}
