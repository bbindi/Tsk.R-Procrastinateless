package com.brixavier.tskr.bloc

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brixavier.tskr.data.ReminderDao
import com.brixavier.tskr.engine.RecurrenceEngine
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import com.brixavier.tskr.model.WeeklyProgress
import com.brixavier.tskr.model.WeeklyProgressCalculator
import com.brixavier.tskr.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.brixavier.tskr.widget.TskRWidget
import androidx.glance.appwidget.updateAll
import com.brixavier.tskr.personality.ActionType
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Representing all possible states of the Reminder Screen.
 */
data class ReminderState(
    val reminders: List<Reminder> = emptyList(),
    val pendingOccurrences: List<ReminderOccurrence> = emptyList(),
    val completedOccurrences: List<ReminderOccurrence> = emptyList(),
    val uiItems: List<ReminderUiItem> = emptyList(),
    val plans: List<Plan> = emptyList(),
    val planProgress: List<PlanProgress> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val userMessageAction: ActionType? = null,
    val streak: Int = 0,
    val weeklyProgress: WeeklyProgress = WeeklyProgress(),
    val noteSections: List<PlanNoteSection> = emptyList(),
    val planSteps: List<PlanStep> = emptyList()
)

/**
 * Representing all user actions (Events) that can be dispatched to the Bloc.
 */
sealed class ReminderEvent {
    data class AddReminder(
        val title: String,
        val date: String,
        val time: String,
        val priority: String = Reminder.PRIORITY_NORMAL,
        val notes: String = "",
        val subTasksRaw: String = "",
        val motivation: String = "",
        val recurrenceType: String = Reminder.RECURRENCE_NONE,
        val recurrenceInterval: Int = 1,
        val notify7Days: Boolean = false,
        val notify24Hours: Boolean = false,
        val notifyDueNow: Boolean = true,
        val planId: String? = null,
        val recurrenceDaysOfWeek: String = "",
        val recurrenceMonthlyMode: String = Reminder.MONTHLY_MODE_DAY_OF_MONTH,
        val recurrenceYearlyMode: String = Reminder.YEARLY_MODE_SPECIFIC_DATE,
        val recurrenceOrdinal: String = "",
        val recurrenceWeekday: String = "",
        val recurrenceYearlyMonth: Int = 0
    ) : ReminderEvent()
    data class ToggleReminder(val id: String, val isActive: Boolean) : ReminderEvent()
    data class DeleteReminder(val id: String) : ReminderEvent()
    data class UpdateReminder(val reminder: Reminder) : ReminderEvent()
    data class SnoozeReminder(val occurrenceId: String) : ReminderEvent()
    data class ToggleChecklistItem(val occurrenceId: String, val stepIndex: Int, val isChecked: Boolean) : ReminderEvent()

    data class AddPlan(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val description: String = "",
        val icon: String = "",
        val deadline: String? = null,
        val category: String = "PERSONAL",
        val categorySource: String = "AUTOMATIC",
        val color: String = "SLATE",
        val colorSource: String = "CATEGORY"
    ) : ReminderEvent()
    data class UpdatePlan(val plan: Plan) : ReminderEvent()
    data class DeletePlan(val id: String) : ReminderEvent()

    data class AddNoteSection(val planId: String, val title: String, val content: String = "") : ReminderEvent()
    data class UpdateNoteSection(val section: PlanNoteSection) : ReminderEvent()
    data class DeleteNoteSection(val section: PlanNoteSection) : ReminderEvent()
    data class ReorderNoteSections(val sections: List<PlanNoteSection>) : ReminderEvent()

    data class AddPlanStep(val planId: String, val text: String) : ReminderEvent()
    data class UpdatePlanStep(val step: PlanStep) : ReminderEvent()
    data class DeletePlanStep(val step: PlanStep) : ReminderEvent()
    data class ReorderPlanSteps(val steps: List<PlanStep>) : ReminderEvent()

    object ClearUserMessage : ReminderEvent()
    object LoadReminders : ReminderEvent()
}

/**
 * Business Logic Component (BLoC) for managing Reminders and Plans.
 * Updated in V2-C C4 to support Plan ↔ Task assignment and movement transactions.
 */
class ReminderBloc(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ReminderState?>(null)
    val state: StateFlow<ReminderState?> = _state.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("tsk_r_prefs", Context.MODE_PRIVATE)

    // Synchronized set guarding against rapid duplicate completion taps for the same occurrence ID
    private val processingIds = Collections.synchronizedSet(mutableSetOf<String>())

    // Synchronized map guarding against rapid multi-tap completion cascades for the same master template ID (500ms cooldown)
    private val templateCooldownUntil = Collections.synchronizedMap(mutableMapOf<String, Long>())

    init {
        updateStreakState()

        viewModelScope.launch {
            combine(
                combine(
                    reminderDao.getAllRemindersFlow(),
                    reminderDao.getPendingOccurrencesFlow(),
                    reminderDao.getCompletedOccurrencesFlow(),
                    reminderDao.getAllPlansFlow()
                ) { r1, r2, r3, r4 -> arrayOf(r1, r2, r3, r4) },
                reminderDao.getAllPlanProgressFlow(),
                reminderDao.getAllNoteSectionsFlow(),
                reminderDao.getAllPlanStepsFlow()
            ) { tuple, progressList, sectionsList, stepsList ->
                @Suppress("UNCHECKED_CAST") val templates = tuple[0] as List<Reminder>
                @Suppress("UNCHECKED_CAST") val pendingList = tuple[1] as List<ReminderOccurrence>
                @Suppress("UNCHECKED_CAST") val completedList = tuple[2] as List<ReminderOccurrence>
                @Suppress("UNCHECKED_CAST") val plansList = tuple[3] as List<Plan>

                val items = mapToUiItems(templates, pendingList, completedList, plansList)
                val streak = sharedPrefs.getInt("streak_count", 0)

                val (monStr, sunStr) = WeeklyProgressCalculator.getWeekBounds()
                val allOccurrences = pendingList + completedList
                val weeklyProg = WeeklyProgressCalculator.calculate(allOccurrences, monStr, sunStr)

                ReminderState(
                    reminders = templates,
                    pendingOccurrences = pendingList,
                    completedOccurrences = completedList,
                    uiItems = items,
                    plans = plansList,
                    planProgress = progressList,
                    isLoading = false,
                    streak = streak,
                    weeklyProgress = weeklyProg,
                    noteSections = sectionsList,
                    planSteps = stepsList
                )
            }.collect { newState ->
                _state.update { oldState ->
                    newState.copy(
                        userMessage = oldState?.userMessage,
                        userMessageAction = oldState?.userMessageAction,
                        errorMessage = oldState?.errorMessage
                    )
                }
            }
        }
    }

    /**
     * Dispatch an event to be processed by the BLoC.
     */
    fun onEvent(event: ReminderEvent) {
        viewModelScope.launch {
            when (event) {
                is ReminderEvent.LoadReminders -> {
                    // Handled automatically by Flow combine in init
                }
                is ReminderEvent.AddReminder -> handleAddReminder(
                    event.title,
                    event.date,
                    event.time,
                    event.priority,
                    event.notes,
                    event.subTasksRaw,
                    event.motivation,
                    event.recurrenceType,
                    event.recurrenceInterval,
                    event.notify7Days,
                    event.notify24Hours,
                    event.notifyDueNow,
                    event.planId,
                    event.recurrenceDaysOfWeek,
                    event.recurrenceMonthlyMode,
                    event.recurrenceYearlyMode,
                    event.recurrenceOrdinal,
                    event.recurrenceWeekday,
                    event.recurrenceYearlyMonth
                )
                is ReminderEvent.ToggleReminder -> handleToggleReminder(event.id, event.isActive)
                is ReminderEvent.DeleteReminder -> handleDeleteReminder(event.id)
                is ReminderEvent.UpdateReminder -> handleUpdateReminder(event.reminder)
                is ReminderEvent.SnoozeReminder -> handleSnoozeReminder(event.occurrenceId)
                is ReminderEvent.ToggleChecklistItem -> handleToggleChecklistItem(event.occurrenceId, event.stepIndex, event.isChecked)

                is ReminderEvent.AddPlan -> handleAddPlan(
                    event.id, event.name, event.description, event.icon, event.deadline,
                    event.category, event.categorySource, event.color, event.colorSource
                )
                is ReminderEvent.UpdatePlan -> handleUpdatePlan(event.plan)
                is ReminderEvent.DeletePlan -> handleDeletePlan(event.id)

                is ReminderEvent.AddNoteSection -> handleAddNoteSection(event.planId, event.title, event.content)
                is ReminderEvent.UpdateNoteSection -> handleUpdateNoteSection(event.section)
                is ReminderEvent.DeleteNoteSection -> handleDeleteNoteSection(event.section)
                is ReminderEvent.ReorderNoteSections -> handleReorderNoteSections(event.sections)

                is ReminderEvent.AddPlanStep -> handleAddPlanStep(event.planId, event.text)
                is ReminderEvent.UpdatePlanStep -> handleUpdatePlanStep(event.step)
                is ReminderEvent.DeletePlanStep -> handleDeletePlanStep(event.step)
                is ReminderEvent.ReorderPlanSteps -> handleReorderPlanSteps(event.steps)

                is ReminderEvent.ClearUserMessage -> {
                    _state.update { it?.copy(userMessage = null, userMessageAction = null, errorMessage = null) }
                }
            }
        }
    }

    private suspend fun handleAddReminder(
        title: String,
        date: String,
        time: String,
        priority: String = Reminder.PRIORITY_NORMAL,
        notes: String = "",
        subTasksRaw: String = "",
        motivation: String = "",
        recurrenceType: String = Reminder.RECURRENCE_NONE,
        recurrenceInterval: Int = 1,
        notify7Days: Boolean = false,
        notify24Hours: Boolean = false,
        notifyDueNow: Boolean = true,
        planId: String? = null,
        recurrenceDaysOfWeek: String = "",
        recurrenceMonthlyMode: String = Reminder.MONTHLY_MODE_DAY_OF_MONTH,
        recurrenceYearlyMode: String = Reminder.YEARLY_MODE_SPECIFIC_DATE,
        recurrenceOrdinal: String = "",
        recurrenceWeekday: String = "",
        recurrenceYearlyMonth: Int = 0
    ) {
        if (title.isBlank()) {
            _state.update { it?.copy(errorMessage = "Task name cannot be empty") }
            return
        }

        val safeInterval = if (recurrenceInterval < 1) 1 else recurrenceInterval
        val templateId = UUID.randomUUID().toString()

        val newReminder = Reminder(
            id = templateId,
            title = title,
            date = date,
            time = time,
            isActive = true,
            priority = priority,
            notes = notes,
            subTasksRaw = subTasksRaw,
            motivation = motivation,
            recurrenceType = recurrenceType,
            recurrenceInterval = safeInterval,
            notify7Days = notify7Days,
            notify24Hours = notify24Hours,
            notifyDueNow = notifyDueNow,
            planId = planId,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek,
            recurrenceMonthlyMode = recurrenceMonthlyMode,
            recurrenceYearlyMode = recurrenceYearlyMode,
            recurrenceOrdinal = recurrenceOrdinal,
            recurrenceWeekday = recurrenceWeekday,
            recurrenceYearlyMonth = recurrenceYearlyMonth
        )

        val initialOccurrence = ReminderOccurrence(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            title = title,
            priority = priority,
            date = date,
            time = time,
            status = ReminderOccurrence.STATUS_PENDING,
            subTasksRaw = resetChecklistFromTemplate(subTasksRaw),
            notify7Days = notify7Days,
            notify24Hours = notify24Hours,
            notifyDueNow = notifyDueNow,
            planId = planId
        )

        withContext(Dispatchers.IO) {
            reminderDao.insertReminder(newReminder)
            reminderDao.insertOccurrence(initialOccurrence)

            reminderScheduler.scheduleOccurrenceAlarms(initialOccurrence)
            updateWidget()
        }

        _state.update { it?.copy(userMessage = "Task scheduled successfully.", userMessageAction = ActionType.TASK_CREATED) }
    }

    private suspend fun handleToggleReminder(id: String, isActive: Boolean) {
        if (!processingIds.add(id)) {
            // Guard against rapid duplicate tap for the exact same occurrence ID
            return
        }

        try {
            withContext(Dispatchers.IO) {
                if (!isActive) {
                    // Resolve target occurrence and its templateId to enforce template-scoped cooldown
                    val targetOcc = reminderDao.getOccurrenceById(id)
                        ?: reminderDao.getOccurrencesForTemplate(id).firstOrNull { it.isPending() }
                    val templateId = targetOcc?.templateId ?: targetOcc?.id ?: id

                    val now = System.currentTimeMillis()
                    val cooldownUntil = templateCooldownUntil[templateId] ?: 0L
                    if (now < cooldownUntil) {
                        // Inside 500ms template completion cooldown; safely reject rapid tap
                        return@withContext
                    }
                    templateCooldownUntil[templateId] = now + 500L

                    // Complete task: run atomic transaction
                    val nextOccurrence = reminderDao.completeOccurrenceTransaction(id)

                    reminderScheduler.cancelOccurrenceAlarms(id)
                    cancelNotification(id)

                    if (nextOccurrence != null) {
                        reminderScheduler.scheduleOccurrenceAlarms(nextOccurrence)
                        _state.update {
                            it?.copy(
                                userMessage = "Task completed — next occurrence: ${nextOccurrence.date} at ${nextOccurrence.time}.",
                                userMessageAction = ActionType.COMPLETION
                            )
                        }
                    } else {
                        _state.update {
                            it?.copy(
                                userMessage = "Task marked as completed.",
                                userMessageAction = ActionType.COMPLETION
                            )
                        }
                    }

                    checkAndIncrementStreak()
                } else {
                    // Restore task: run atomic restore transaction
                    val restoredOcc = reminderDao.restoreOccurrenceTransaction(id)
                    if (restoredOcc != null) {
                        reminderScheduler.scheduleOccurrenceAlarms(restoredOcc)
                        _state.update {
                            it?.copy(
                                userMessage = "Task restored.",
                                userMessageAction = ActionType.TASK_RESTORED
                            )
                        }
                    } else {
                        _state.update {
                            it?.copy(
                                userMessage = "An active task for this recurring schedule is already pending.",
                                userMessageAction = null
                            )
                        }
                    }
                }
                updateWidget()
            }
        } finally {
            processingIds.remove(id)
        }
    }

    private suspend fun handleSnoozeReminder(occurrenceId: String) {
        withContext(Dispatchers.IO) {
            val occurrence = reminderDao.getOccurrenceById(occurrenceId) ?: return@withContext
            if (!occurrence.isPending()) return@withContext

            val snoozeTime = LocalDateTime.now().plusMinutes(5)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            val snoozeEpochMs = snoozeTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val snoozedOccurrence = occurrence.copy(
                date = snoozeTime.format(dateFormatter),
                time = snoozeTime.format(timeFormatter),
                snoozedUntilMs = snoozeEpochMs,
                snoozeCount = occurrence.snoozeCount + 1
            )

            reminderDao.updateOccurrence(snoozedOccurrence)
            reminderScheduler.scheduleOccurrenceAlarms(snoozedOccurrence)

            _state.update {
                it?.copy(
                    userMessage = "Snoozed for 5 minutes.",
                    userMessageAction = ActionType.TASK_SNOOZED
                )
            }
            updateWidget()
        }
    }

    private suspend fun handleToggleChecklistItem(occurrenceId: String, stepIndex: Int, isChecked: Boolean) {
        withContext(Dispatchers.IO) {
            val occurrence = reminderDao.getOccurrenceById(occurrenceId) ?: return@withContext
            val steps = occurrence.subTasksRaw.split("|").toMutableList()
            if (stepIndex in steps.indices) {
                val trimmed = steps[stepIndex].trim()
                val text = when {
                    trimmed.startsWith("[x] ") -> trimmed.substring(4)
                    trimmed.startsWith("[ ] ") -> trimmed.substring(4)
                    else -> trimmed
                }
                steps[stepIndex] = if (isChecked) "[x] $text" else "[ ] $text"
                val updatedSubTasks = steps.joinToString("|")
                val updatedOcc = occurrence.copy(subTasksRaw = updatedSubTasks)
                reminderDao.updateOccurrence(updatedOcc)
            }
        }
    }

    private fun updateStreakState() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDate = sharedPrefs.getString("last_streak_date", "")
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }

            val validStreak = if (lastDate == today || lastDate == yesterday) {
                sharedPrefs.getInt("streak_count", 0)
            } else {
                0
            }

            _state.update { it?.copy(streak = validStreak) }
        }
    }

    private suspend fun checkAndIncrementStreak() {
        withContext(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDate = sharedPrefs.getString("last_streak_date", "")
            var currentStreak = sharedPrefs.getInt("streak_count", 0)

            if (lastDate != today) {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }

                if (lastDate == yesterday) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }

                sharedPrefs.edit()
                    .putInt("streak_count", currentStreak)
                    .putString("last_streak_date", today)
                    .apply()
                
                _state.update { it?.copy(streak = currentStreak) }
            }
        }
    }

    private suspend fun handleDeleteReminder(id: String) {
        withContext(Dispatchers.IO) {
            reminderDao.deleteTemplateTransaction(id)
            reminderScheduler.cancelOccurrenceAlarms(id)
            cancelNotification(id)
            updateWidget()
        }
        _state.update { it?.copy(userMessage = "Task deleted.", userMessageAction = ActionType.TASK_DELETED) }
    }

    private fun cancelNotification(id: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val hash = id.hashCode() and 0x00ffffff
        
        notificationManager.cancel(hash * 10 + 1) // STAGE_7_DAYS
        notificationManager.cancel(hash * 10 + 2) // STAGE_24_HOURS
        notificationManager.cancel(hash * 10 + 3) // STAGE_DUE_NOW
        notificationManager.cancel(hash * 10 + 9) // Default/Fallback
    }

    private suspend fun handleUpdateReminder(reminder: Reminder) {
        if (reminder.title.isBlank()) {
            _state.update { it?.copy(errorMessage = "Task name cannot be empty") }
            return
        }

        val updatedPending = withContext(Dispatchers.IO) {
            // Atomic Room Transaction
            reminderDao.updateReminderTransaction(reminder)
        }

        // Post-Transaction Side Effects (Alarms, Widget, Notifications)
        withContext(Dispatchers.IO) {
            if (updatedPending != null) {
                reminderScheduler.cancelOccurrenceAlarms(updatedPending.id)
                cancelNotification(updatedPending.id)
                if (reminder.isActive) {
                    reminderScheduler.scheduleOccurrenceAlarms(updatedPending)
                }
            }
            updateWidget()
        }

        _state.update { it?.copy(userMessage = "Task updated.", userMessageAction = ActionType.TASK_UPDATED) }
    }

    private suspend fun handleAddPlan(
        id: String,
        name: String,
        description: String,
        icon: String,
        deadline: String?,
        category: String = "PERSONAL",
        categorySource: String = "AUTOMATIC",
        color: String = "SLATE",
        colorSource: String = "CATEGORY"
    ) {
        if (name.isBlank()) {
            _state.update { it?.copy(errorMessage = "Plan name cannot be empty") }
            return
        }

        val newPlan = Plan(
            id = id,
            name = name,
            description = description,
            icon = icon,
            deadline = deadline,
            category = category,
            categorySource = categorySource,
            color = color,
            colorSource = colorSource
        )

        withContext(Dispatchers.IO) {
            reminderDao.insertPlan(newPlan)
        }

        _state.update { it?.copy(userMessage = "Plan created successfully.") }
    }

    private suspend fun handleUpdatePlan(plan: Plan) {
        if (plan.name.isBlank()) {
            _state.update { it?.copy(errorMessage = "Plan name cannot be empty") }
            return
        }

        withContext(Dispatchers.IO) {
            reminderDao.updatePlan(plan)
        }

        _state.update { it?.copy(userMessage = "Plan updated.") }
    }

    private suspend fun handleDeletePlan(id: String) {
        withContext(Dispatchers.IO) {
            reminderDao.deletePlanTransaction(id)
        }
        _state.update { it?.copy(userMessage = "Plan deleted.") }
    }

    private suspend fun handleAddNoteSection(planId: String, title: String, content: String) {
        withContext(Dispatchers.IO) {
            val existing = reminderDao.getNoteSectionsForPlan(planId)
            val nextSortOrder = if (existing.isNotEmpty()) existing.maxOf { it.sortOrder } + 1 else 0
            val section = PlanNoteSection(
                planId = planId,
                title = title,
                content = content,
                sortOrder = nextSortOrder
            )
            reminderDao.insertNoteSection(section)
        }
    }

    private suspend fun handleUpdateNoteSection(section: PlanNoteSection) {
        withContext(Dispatchers.IO) {
            val updated = section.copy(updatedAtMs = System.currentTimeMillis())
            reminderDao.updateNoteSection(updated)
        }
    }

    private suspend fun handleDeleteNoteSection(section: PlanNoteSection) {
        withContext(Dispatchers.IO) {
            reminderDao.deleteNoteSection(section)
        }
    }

    private suspend fun handleReorderNoteSections(sections: List<PlanNoteSection>) {
        withContext(Dispatchers.IO) {
            sections.forEachIndexed { index, sec ->
                reminderDao.updateNoteSection(sec.copy(sortOrder = index, updatedAtMs = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun handleAddPlanStep(planId: String, text: String) {
        withContext(Dispatchers.IO) {
            val existing = reminderDao.getStepsForPlan(planId)
            val nextSortOrder = if (existing.isNotEmpty()) existing.maxOf { it.sortOrder } + 1 else 0
            val step = PlanStep(
                planId = planId,
                text = text,
                isCompleted = false,
                sortOrder = nextSortOrder
            )
            reminderDao.insertStep(step)
        }
    }

    private suspend fun handleUpdatePlanStep(step: PlanStep) {
        withContext(Dispatchers.IO) {
            val updated = step.copy(updatedAtMs = System.currentTimeMillis())
            reminderDao.updateStep(updated)
        }
    }

    private suspend fun handleDeletePlanStep(step: PlanStep) {
        withContext(Dispatchers.IO) {
            reminderDao.deleteStep(step)
        }
    }

    private suspend fun handleReorderPlanSteps(steps: List<PlanStep>) {
        withContext(Dispatchers.IO) {
            steps.forEachIndexed { index, st ->
                reminderDao.updateStep(st.copy(sortOrder = index, updatedAtMs = System.currentTimeMillis()))
            }
        }
    }

    private fun updateWidget() {
        viewModelScope.launch(Dispatchers.IO) {
            TskRWidget().updateAll(context)
        }
    }
}

/**
 * Maps Room templates and occurrences into unified presentation UI items with plan association.
 */
private fun mapToUiItems(
    templates: List<Reminder>,
    pendingOccurrences: List<ReminderOccurrence>,
    completedOccurrences: List<ReminderOccurrence>,
    plans: List<Plan>
): List<ReminderUiItem> {
    val templateMap = templates.associateBy { it.id }
    val planMap = plans.associateBy { it.id }

    fun buildItem(occ: ReminderOccurrence): ReminderUiItem {
        val tmpl = occ.templateId?.let { templateMap[it] }
        val plan = occ.planId?.let { planMap[it] } ?: tmpl?.planId?.let { planMap[it] }
        return ReminderUiItem(
            occurrenceId = occ.id,
            templateId = occ.templateId,
            title = occ.title,
            date = occ.date,
            time = occ.time,
            priority = occ.priority,
            notes = tmpl?.notes ?: "",
            subTasksRaw = occ.subTasksRaw,
            motivation = tmpl?.motivation ?: "",
            status = occ.status,
            completedAtMs = occ.completedAtMs,
            recurrenceType = tmpl?.recurrenceType ?: Reminder.RECURRENCE_NONE,
            recurrenceInterval = tmpl?.recurrenceInterval ?: 1,
            notify7Days = occ.notify7Days,
            notify24Hours = occ.notify24Hours,
            notifyDueNow = occ.notifyDueNow,
            planId = plan?.id,
            planName = plan?.name,
            planIcon = plan?.icon,
            snoozeCount = occ.snoozeCount
        )
    }

    val pendingUiItems = pendingOccurrences.map { buildItem(it) }
    val completedUiItems = completedOccurrences.map { buildItem(it) }

    return pendingUiItems + completedUiItems
}

private fun resetChecklistFromTemplate(templateSubTasksRaw: String): String {
    if (templateSubTasksRaw.isBlank()) return ""
    return templateSubTasksRaw.split("|")
        .filter { it.isNotBlank() }
        .joinToString("|") { rawItem ->
            val trimmed = rawItem.trim()
            val cleanText = when {
                trimmed.startsWith("[x] ") -> trimmed.substring(4)
                trimmed.startsWith("[ ] ") -> trimmed.substring(4)
                else -> trimmed
            }
            "[ ] $cleanText"
        }
}
