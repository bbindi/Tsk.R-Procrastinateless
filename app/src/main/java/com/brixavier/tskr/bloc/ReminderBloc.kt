package com.brixavier.tskr.bloc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brixavier.tskr.data.ReminderDao
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.brixavier.tskr.widget.TskRWidget
import androidx.glance.appwidget.updateAll

/**
 * Representing all possible states of the Reminder Screen.
 */
data class ReminderState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val streak: Int = 0
)

/**
 * Representing all user actions (Events) that can be dispatched to the Bloc.
 */
sealed class ReminderEvent {
    data class AddReminder(
        val title: String,
        val date: String,
        val time: String,
        val priority: String = "CASUAL",
        val notes: String = "",
        val subTasksRaw: String = "",
        val motivation: String = ""
    ) : ReminderEvent()
    data class ToggleReminder(val id: String, val isActive: Boolean) : ReminderEvent()
    data class DeleteReminder(val id: String) : ReminderEvent()
    data class UpdateReminder(val reminder: Reminder) : ReminderEvent()
    object LoadReminders : ReminderEvent()
}

/**
 * Business Logic Component (BLoC) for managing Reminders.
 * Now integrated with Room Database for persistence and AlarmManager for trigger timing.
 */
class ReminderBloc(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow<ReminderState?>(null)
    val state: StateFlow<ReminderState?> = _state.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("tsk_r_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Initialize streak from preferences
        updateStreakState()

        // Collect Flow from Room database to keep state synchronized automatically
        viewModelScope.launch {
            reminderDao.getAllRemindersFlow().collect { remindersList ->
                val streak = sharedPrefs.getInt("streak_count", 0)
                _state.update { 
                    ReminderState(
                        reminders = remindersList,
                        isLoading = false,
                        streak = streak
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
                    // Handled automatically by the Flow collection in init
                }
                is ReminderEvent.AddReminder -> handleAddReminder(
                    event.title,
                    event.date,
                    event.time,
                    event.priority,
                    event.notes,
                    event.subTasksRaw,
                    event.motivation
                )
                is ReminderEvent.ToggleReminder -> handleToggleReminder(event.id, event.isActive)
                is ReminderEvent.DeleteReminder -> handleDeleteReminder(event.id)
                is ReminderEvent.UpdateReminder -> handleUpdateReminder(event.reminder)
            }
        }
    }

    private suspend fun handleAddReminder(
        title: String,
        date: String,
        time: String,
        priority: String = "CASUAL",
        notes: String = "",
        subTasksRaw: String = "",
        motivation: String = ""
    ) {
        if (title.isBlank()) {
            _state.update { it?.copy(errorMessage = "Task name cannot be empty") }
            return
        }

        val newReminder = Reminder(
            title = title,
            date = date,
            time = time,
            isActive = true,
            priority = priority,
            notes = notes,
            subTasksRaw = subTasksRaw,
            motivation = motivation
        )

        withContext(Dispatchers.IO) {
            // 1. Persist in SQLite Room Database
            reminderDao.insertReminder(newReminder)

            // 2. Schedule alarms using AlarmManager (7 days and 24 hours warnings)
            reminderScheduler.scheduleAlarms(newReminder)
            
            updateWidget()
        }
    }

    private suspend fun handleToggleReminder(id: String, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            val reminder = reminderDao.getReminderById(id) ?: return@withContext
            val updatedReminder = reminder.copy(isActive = isActive)

            // 1. Update database
            reminderDao.updateReminder(updatedReminder)

            // 2. Schedule or Cancel alarms based on new active state
            if (isActive) {
                reminderScheduler.scheduleAlarms(updatedReminder)
            } else {
                reminderScheduler.cancelAlarms(updatedReminder)
                // If task was completed (active -> inactive), check streak
                checkAndIncrementStreak()
            }
            
            updateWidget()
        }
    }

    private fun updateStreakState() {
        viewModelScope.launch(Dispatchers.IO) {
            val streak = sharedPrefs.getInt("streak_count", 0)
            _state.update { it?.copy(streak = streak) }
        }
    }

    private suspend fun checkAndIncrementStreak() {
        withContext(Dispatchers.IO) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val lastDate = sharedPrefs.getString("last_streak_date", "")
            var currentStreak = sharedPrefs.getInt("streak_count", 0)

            if (lastDate != today) {
                val yesterday = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -1)
                }.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(it.time) }

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
            val reminder = reminderDao.getReminderById(id) ?: return@withContext

            // 1. Delete from database
            reminderDao.deleteReminder(reminder)

            // 2. Cancel scheduled AlarmManager alarms
            reminderScheduler.cancelAlarms(reminder)
            
            updateWidget()
        }
    }

    private suspend fun handleUpdateReminder(reminder: Reminder) {
        if (reminder.title.isBlank()) {
            _state.update { it?.copy(errorMessage = "Task name cannot be empty") }
            return
        }

        withContext(Dispatchers.IO) {
            // 1. Update Room Database using the annotated `update` method
            reminderDao.update(reminder)

            // 2. Reschedule background alarms with new times
            reminderScheduler.cancelAlarms(reminder)
            if (reminder.isActive) {
                reminderScheduler.scheduleAlarms(reminder)
            }
            
            updateWidget()
        }
    }

    private fun updateWidget() {
        viewModelScope.launch(Dispatchers.IO) {
            TskRWidget().updateAll(context)
        }
    }
}
