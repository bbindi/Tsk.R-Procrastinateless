package com.brixavier.tskr.personality

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Pure, stateless domain helper that transforms raw occurrence, reminder, and plan state
 * into structured situational facts for PersonalityContext.
 *
 * Contains ZERO Room queries, network calls, or UI dependencies.
 */
object BehavioralContextResolver {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    /**
     * Determines whether an occurrence was completed BEFORE its scheduled due timestamp.
     * Evaluates true iff completedAtMs is present, completedAtMs < scheduledEpochMs, and snoozedUntilMs is null.
     */
    fun isEarlyCompletion(occurrence: ReminderOccurrence): Boolean {
        val completedAt = occurrence.completedAtMs ?: return false
        if (occurrence.snoozedUntilMs != null) return false

        val scheduledEpochMs = parseScheduledEpochMs(occurrence.date, occurrence.time) ?: return false
        return completedAt < scheduledEpochMs
    }

    /**
     * Calculates the overdue duration in minutes relative to a given local timestamp string ("yyyy-MM-dd HH:mm").
     */
    fun calculateOverdueMinutes(
        dateStr: String,
        timeStr: String,
        nowStr: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    ): Int {
        try {
            val scheduled = LocalDateTime.parse("$dateStr $timeStr", formatter)
            val now = LocalDateTime.parse(nowStr, formatter)
            if (now.isAfter(scheduled)) {
                return Duration.between(scheduled, now).toMinutes().toInt()
            }
        } catch (_: Exception) {}
        return 0
    }

    /**
     * Helper to convert date and time strings to epoch milliseconds.
     */
    fun parseScheduledEpochMs(dateStr: String, timeStr: String): Long? {
        return try {
            val dt = LocalDateTime.parse("$dateStr $timeStr", formatter)
            dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Constructs a complete, immutable PersonalityContext for a given occurrence, parent reminder, and plan.
     */
    fun resolveContext(
        actionType: ActionType,
        occurrence: ReminderOccurrence?,
        reminder: Reminder?,
        plan: Plan? = null,
        streak: Int = 0,
        nowHour: Int = LocalTime.now().hour,
        planProgressRatio: Float = 0f,
        planTotalTasks: Int = 0,
        planCompletedTasks: Int = 0,
        oldestOverdueMinutes: Int = 0
    ): PersonalityContext {
        val isImportant = (occurrence?.priority == Reminder.PRIORITY_IMPORTANT) ||
                          (reminder?.priority == Reminder.PRIORITY_IMPORTANT)
        val isRecurring = (occurrence?.templateId != null) || (reminder?.isRecurring() == true)
        val snoozeCount = occurrence?.snoozeCount ?: 0

        var overdueMinutes = 0
        var earlyCompletion = false

        if (occurrence != null) {
            overdueMinutes = calculateOverdueMinutes(occurrence.date, occurrence.time)
            earlyCompletion = isEarlyCompletion(occurrence)
        } else if (reminder != null) {
            overdueMinutes = calculateOverdueMinutes(reminder.date, reminder.time)
        }

        return PersonalityContext(
            actionType = actionType,
            intensity = if (isImportant) PersonalityIntensity.MISSION else PersonalityIntensity.ACCOUNTABILITY,
            urgency = if (isImportant) UrgencyLevel.IMPORTANT else UrgencyLevel.NORMAL,
            isRecurring = isRecurring,
            streak = streak,
            timeOfDayHour = nowHour,
            overdueMinutes = overdueMinutes,
            snoozeCount = snoozeCount,
            isEarlyCompletion = earlyCompletion,
            oldestOverdueMinutes = oldestOverdueMinutes,
            planProgressRatio = planProgressRatio,
            planTotalTasks = planTotalTasks,
            planCompletedTasks = planCompletedTasks
        )
    }
}
