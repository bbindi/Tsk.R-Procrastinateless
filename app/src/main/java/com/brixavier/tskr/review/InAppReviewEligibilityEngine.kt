package com.brixavier.tskr.review

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Pure, testable eligibility engine governing Google Play In-App Review prompt eligibility.
 */
class InAppReviewEligibilityEngine(
    private val prefs: SharedPreferences
) {

    companion object {
        private const val TAG = "ReviewEligibilityEngine"

        const val KEY_FIRST_SESSION_MS = "review_first_session_ms"
        const val KEY_SESSION_COUNT = "review_session_count"
        const val KEY_COMPLETED_TASK_COUNT = "review_completed_task_count"
        const val KEY_LAST_PROMPT_MS = "review_last_prompt_ms"
        const val KEY_PROMPT_DISMISSED = "review_prompt_dismissed"

        const val MIN_SESSIONS = 5
        const val MIN_COMPLETED_TASKS = 5
        const val MIN_ELAPSED_DAYS_MS = 3L * 24 * 60 * 60 * 1000 // 3 days
        const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000        // 30 days

        fun create(context: Context): InAppReviewEligibilityEngine {
            val sharedPrefs = context.getSharedPreferences("tsk_r_prefs", Context.MODE_PRIVATE)
            return InAppReviewEligibilityEngine(sharedPrefs)
        }
    }

    private fun logDebug(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {}
    }

    fun recordSessionLaunch(currentTimeMs: Long = System.currentTimeMillis()) {
        val firstSessionMs = prefs.getLong(KEY_FIRST_SESSION_MS, 0L)
        val currentSessionCount = prefs.getInt(KEY_SESSION_COUNT, 0)

        val editor = prefs.edit()
        if (firstSessionMs == 0L) {
            editor.putLong(KEY_FIRST_SESSION_MS, currentTimeMs)
        }
        editor.putInt(KEY_SESSION_COUNT, currentSessionCount + 1)
        editor.apply()

        logDebug("Recorded session launch #${currentSessionCount + 1}")
    }

    fun recordTaskCompletion() {
        val currentCompletedCount = prefs.getInt(KEY_COMPLETED_TASK_COUNT, 0)
        prefs.edit().putInt(KEY_COMPLETED_TASK_COUNT, currentCompletedCount + 1).apply()
        logDebug("Recorded task completion #${currentCompletedCount + 1}")
    }

    fun recordPromptAttempt(currentTimeMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_PROMPT_MS, currentTimeMs).apply()
        logDebug("Recorded review prompt attempt at timestamp: $currentTimeMs")
    }

    fun recordPromptDismissed() {
        prefs.edit().putBoolean(KEY_PROMPT_DISMISSED, true).apply()
        logDebug("Recorded user review prompt dismissal.")
    }

    fun isEligibleForReviewPrompt(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        val firstSessionMs = prefs.getLong(KEY_FIRST_SESSION_MS, 0L)
        val sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0)
        val completedTaskCount = prefs.getInt(KEY_COMPLETED_TASK_COUNT, 0)
        val lastPromptMs = prefs.getLong(KEY_LAST_PROMPT_MS, 0L)
        val isDismissed = prefs.getBoolean(KEY_PROMPT_DISMISSED, false)

        // Rule 1: Never prompt on first launch or if dismissed
        if (sessionCount <= 1 || isDismissed) return false

        // Rule 2: Require at least 5 sessions
        if (sessionCount < MIN_SESSIONS) return false

        // Rule 3: Require at least 5 completed tasks
        if (completedTaskCount < MIN_COMPLETED_TASKS) return false

        // Rule 4: Require sessions spanning multiple days (at least 3 days since first session)
        if (firstSessionMs == 0L || (currentTimeMs - firstSessionMs) < MIN_ELAPSED_DAYS_MS) return false

        // Rule 5: Require 30-day cooldown between prompt attempts
        if (lastPromptMs > 0L && (currentTimeMs - lastPromptMs) < COOLDOWN_MS) return false

        return true
    }
}
