package com.brixavier.tskr.review

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests covering [InAppReviewEligibilityEngine] rules and state persistence.
 */
class InAppReviewEligibilityTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()

        override fun getAll(): Map<String, *> = values
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = @Suppress("UNCHECKED_CAST") (values[key] as? Set<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val values: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) changes[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
                if (key != null && values != null) changes[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) changes.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) values.clear()
                values.putAll(changes)
            }
        }
    }

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var engine: InAppReviewEligibilityEngine

    private val baseTimeMs = 1_000_000_000_000L // Baseline evaluation timestamp
    private val oneDayMs = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        engine = InAppReviewEligibilityEngine(prefs)
    }

    @Test
    fun test1_firstLaunch_isNotEligible() {
        engine.recordSessionLaunch(baseTimeMs)
        assertFalse(engine.isEligibleForReviewPrompt(baseTimeMs))
    }

    @Test
    fun test2_fewerThanFiveSessions_isNotEligible() {
        val installTime = baseTimeMs
        // Record 4 sessions across 4 days
        for (i in 0 until 4) {
            engine.recordSessionLaunch(installTime + (i * oneDayMs))
        }
        // Record 5 completed tasks
        repeat(5) { engine.recordTaskCompletion() }

        val evalTime = installTime + (4 * oneDayMs)
        assertFalse(engine.isEligibleForReviewPrompt(evalTime))
    }

    @Test
    fun test3_sessionsSpanLessThanThreeDays_isNotEligible() {
        val installTime = baseTimeMs
        // Record 5 sessions on the SAME day
        repeat(5) { engine.recordSessionLaunch(installTime + 1000) }
        repeat(5) { engine.recordTaskCompletion() }

        // Evaluated 1 day after install (< 3 days elapsed)
        val evalTime = installTime + (1 * oneDayMs)
        assertFalse(engine.isEligibleForReviewPrompt(evalTime))
    }

    @Test
    fun test4_fewerThanFiveCompletedTasks_isNotEligible() {
        val installTime = baseTimeMs
        // Record 5 sessions across 5 days
        for (i in 0 until 5) {
            engine.recordSessionLaunch(installTime + (i * oneDayMs))
        }
        // Record only 4 completed tasks (< 5)
        repeat(4) { engine.recordTaskCompletion() }

        val evalTime = installTime + (5 * oneDayMs)
        assertFalse(engine.isEligibleForReviewPrompt(evalTime))
    }

    @Test
    fun test5_allEligibilityThresholdsMet_isEligible() {
        val installTime = baseTimeMs
        // Record 5 sessions across 5 days
        for (i in 0 until 5) {
            engine.recordSessionLaunch(installTime + (i * oneDayMs))
        }
        // Record 5 completed tasks
        repeat(5) { engine.recordTaskCompletion() }

        val evalTime = installTime + (5 * oneDayMs)
        assertTrue(engine.isEligibleForReviewPrompt(evalTime))
    }

    @Test
    fun test6_cooldownPeriod_blocksPromptWithinThirtyDays() {
        val installTime = baseTimeMs
        for (i in 0 until 5) {
            engine.recordSessionLaunch(installTime + (i * oneDayMs))
        }
        repeat(5) { engine.recordTaskCompletion() }

        val evalTime = installTime + (5 * oneDayMs)
        assertTrue(engine.isEligibleForReviewPrompt(evalTime))

        // Record prompt attempt at evalTime
        engine.recordPromptAttempt(evalTime)

        // Checked 10 days later (within 30-day cooldown) -> false
        assertFalse(engine.isEligibleForReviewPrompt(evalTime + (10 * oneDayMs)))

        // Checked 31 days later (after 30-day cooldown) -> true
        assertTrue(engine.isEligibleForReviewPrompt(evalTime + (31 * oneDayMs)))
    }

    @Test
    fun test7_dismissedPrompt_permanentlyPreventsReviewPrompt() {
        val installTime = baseTimeMs
        for (i in 0 until 5) {
            engine.recordSessionLaunch(installTime + (i * oneDayMs))
        }
        repeat(5) { engine.recordTaskCompletion() }

        val evalTime = installTime + (5 * oneDayMs)
        assertTrue(engine.isEligibleForReviewPrompt(evalTime))

        // User dismisses prompt
        engine.recordPromptDismissed()

        // Permanently ineligible
        assertFalse(engine.isEligibleForReviewPrompt(evalTime + (60 * oneDayMs)))
    }

    @Test
    fun test8_sessionAndTaskRecordPersistence() {
        engine.recordSessionLaunch(baseTimeMs)
        engine.recordTaskCompletion()
        engine.recordTaskCompletion()

        assertEquals(1, prefs.getInt(InAppReviewEligibilityEngine.KEY_SESSION_COUNT, 0))
        assertEquals(2, prefs.getInt(InAppReviewEligibilityEngine.KEY_COMPLETED_TASK_COUNT, 0))
        assertEquals(baseTimeMs, prefs.getLong(InAppReviewEligibilityEngine.KEY_FIRST_SESSION_MS, 0L))
    }

    @Test
    fun test9_configurationChange_doesNotIncrementSessionCount() {
        engine.recordSessionLaunch(baseTimeMs)
        assertEquals(1, prefs.getInt(InAppReviewEligibilityEngine.KEY_SESSION_COUNT, 0))

        fun onActivityRecreated(savedInstanceStateNotNull: Boolean) {
            if (!savedInstanceStateNotNull) {
                engine.recordSessionLaunch(baseTimeMs + 1000)
            }
        }

        // Recreated due to rotation (savedInstanceState != null) -> session count remains 1
        onActivityRecreated(savedInstanceStateNotNull = true)
        assertEquals(1, prefs.getInt(InAppReviewEligibilityEngine.KEY_SESSION_COUNT, 0))

        // Fresh session launch (savedInstanceState == null) -> session count increments to 2
        onActivityRecreated(savedInstanceStateNotNull = false)
        assertEquals(2, prefs.getInt(InAppReviewEligibilityEngine.KEY_SESSION_COUNT, 0))
    }
}
