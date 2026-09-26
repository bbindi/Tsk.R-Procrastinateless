package com.brixavier.tskr.personality

enum class ActionType {
    ONBOARDING,
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    TASK_RESTORED,
    TASK_APPROACHING,
    TASK_DUE,
    TASK_OVERDUE,
    TASK_SNOOZED,
    COMPLETION,
    EMPTY_STATE,
    MISSION_MODE_WARNING,
    MISSION_MODE_DUE,
    MISSION_MODE_COMPLETION,
    WIDGET,
    PLAN_CREATED,
    PLAN_NO_TASKS,
    PLAN_PROGRESS_MILESTONE,
    PLAN_DEADLINE,
    PLAN_COMPLETED,
    PLAN_ARCHIVED,
    PLAN_REOPENED
}

enum class PersonalityIntensity {
    FRIENDLY,
    PLAYFUL,
    ACCOUNTABILITY,
    MISSION
}

enum class UrgencyLevel {
    NORMAL,
    IMPORTANT
}

enum class PlanStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

/**
 * Lightweight, immutable context passed to the PersonalityEngine.
 * Contains no references to Android, Room, or UI layers.
 */
data class PersonalityContext(
    val actionType: ActionType,
    val intensity: PersonalityIntensity = PersonalityIntensity.ACCOUNTABILITY,
    val urgency: UrgencyLevel = UrgencyLevel.NORMAL,
    val isRecurring: Boolean = false,
    val streak: Int = 0,
    val timeOfDayHour: Int? = null,
    val overdueMinutes: Int = 0,
    val snoozeCount: Int = 0,
    val isEarlyCompletion: Boolean = false,
    val oldestOverdueMinutes: Int = 0,
    val isAllCaughtUp: Boolean = false,
    val planProgressRatio: Float = 0f,
    val planTotalTasks: Int = 0,
    val planCompletedTasks: Int = 0,
    val planDaysUntilDeadline: Int? = null,
    val planStatus: PlanStatus = PlanStatus.ACTIVE,
    val isFirstTaskCompleted: Boolean = false
)

/**
 * The generated personality result.
 * Structured so future phases can add metadata if needed without breaking existing API.
 */
data class PersonalityContent(
    val text: String
)
