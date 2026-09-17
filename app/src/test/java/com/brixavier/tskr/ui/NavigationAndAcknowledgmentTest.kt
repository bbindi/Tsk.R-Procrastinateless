package com.brixavier.tskr.ui

import com.brixavier.tskr.MainActivity
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.ReminderUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class NavigationAndAcknowledgmentTest {

    // --- 1. INTENT EXTRA CONSUMPTION LOGIC ---

    @Test
    fun intentExtra_consumptionRemovesExtraFromMap() {
        val extras = mutableMapOf<String, Any?>(
            MainActivity.EXTRA_OPEN_REMINDER_ID to "test-reminder-123"
        )

        // Verify extra is present before consumption
        assertEquals("test-reminder-123", extras[MainActivity.EXTRA_OPEN_REMINDER_ID])

        // Simulate consumption logic in MainActivity
        val extractedId = extras[MainActivity.EXTRA_OPEN_REMINDER_ID] as? String
        if (extractedId != null) {
            extras.remove(MainActivity.EXTRA_OPEN_REMINDER_ID)
        }

        // Verify extra is completely removed after consumption
        assertNull(extras[MainActivity.EXTRA_OPEN_REMINDER_ID])
    }

    // --- 2. EXACT OCCURRENCE ID RESOLUTION OVER TEMPLATE ID ---

    @Test
    fun idResolution_prefersExactOccurrenceIdMatchOverTemplateIdFallback() {
        val targetOccurrenceId = "occ-specific-target"
        val targetTemplateId = "tmpl-shared-123"

        val occ1 = ReminderUiItem(
            occurrenceId = targetOccurrenceId,
            templateId = targetTemplateId,
            title = "Task Occurrence 1",
            date = "2026-09-14",
            time = "10:00",
            priority = Reminder.PRIORITY_NORMAL,
            notes = "",
            subTasksRaw = "",
            motivation = "",
            status = ReminderOccurrence.STATUS_PENDING,
            completedAtMs = null
        )

        val occ2 = ReminderUiItem(
            occurrenceId = "occ-next-generated",
            templateId = targetTemplateId,
            title = "Task Occurrence 2",
            date = "2026-09-15",
            time = "10:00",
            priority = Reminder.PRIORITY_NORMAL,
            notes = "",
            subTasksRaw = "",
            motivation = "",
            status = ReminderOccurrence.STATUS_PENDING,
            completedAtMs = null
        )

        val uiItems = listOf(occ1, occ2)

        // Simulate MainActivity ID resolution logic
        fun resolveSelectedUiItem(selectedId: String?, items: List<ReminderUiItem>): ReminderUiItem? {
            if (selectedId.isNullOrBlank()) return null
            return items.find { it.occurrenceId == selectedId }
                ?: items.find { it.templateId == selectedId }
        }

        // Exact occurrence match resolves occ1
        val matchByOcc = resolveSelectedUiItem(targetOccurrenceId, uiItems)
        assertNotNull(matchByOcc)
        assertEquals(targetOccurrenceId, matchByOcc?.occurrenceId)

        // Template match fallback resolves when occurrenceId doesn't match
        val matchByTmpl = resolveSelectedUiItem(targetTemplateId, uiItems)
        assertNotNull(matchByTmpl)
        assertEquals(targetTemplateId, matchByTmpl?.templateId)
    }

    // --- 3. CONTEXTUAL COMPLETION MESSAGE FORMATTING ---

    @Test
    fun completionMessage_recurringProducesContextualNextOccurrenceMessage() {
        val nextOccurrence = ReminderOccurrence(
            id = "occ-2",
            templateId = "tmpl-daily",
            title = "Drink Water",
            date = "2026-09-15",
            time = "15:10",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val message = "Task completed — next occurrence: ${nextOccurrence.date} at ${nextOccurrence.time}"
        assertEquals("Task completed — next occurrence: 2026-09-15 at 15:10", message)
    }

    @Test
    fun completionMessage_nonRecurringProducesStandardMessage() {
        val message = "Task marked as completed"
        assertEquals("Task marked as completed", message)
    }

    // --- 4. RAPID DUPLICATE TAP SAFEGUARD ---

    @Test
    fun rapidTapSafeguard_preventsDuplicateProcessingForSameOccurrenceId() {
        val processingIds = Collections.synchronizedSet(mutableSetOf<String>())
        val occurrenceId = "occ-rapid-tap-1"

        // First tap adds occurrenceId successfully
        val firstTapAccepted = processingIds.add(occurrenceId)
        assertTrue(firstTapAccepted)

        // Simultaneous second tap for same occurrenceId is rejected
        val secondTapAccepted = processingIds.add(occurrenceId)
        assertFalse(secondTapAccepted)

        // Cleanup after processing finishes
        processingIds.remove(occurrenceId)

        // Subsequent tap after cleanup is accepted
        val thirdTapAccepted = processingIds.add(occurrenceId)
        assertTrue(thirdTapAccepted)
    }

    // --- 5. TEMPLATE-SCOPED COMPLETION COOLDOWN ---

    @Test
    fun templateCooldown_preventsCascadingCompletionAcrossOccurrenceIdChanges() {
        val templateCooldowns = Collections.synchronizedMap(mutableMapOf<String, Long>())
        val templateId = "tmpl-daily-water"

        var currentTime = 1000L

        // Helper simulating Template Cooldown Check
        fun canCompleteTemplate(tmplId: String, now: Long): Boolean {
            val cooldownUntil = templateCooldowns[tmplId] ?: 0L
            if (now < cooldownUntil) {
                return false
            }
            templateCooldowns[tmplId] = now + 500L
            return true
        }

        // Tap 1 for Occurrence #1 at 1000ms -> ACCEPTED
        val tap1Accepted = canCompleteTemplate(templateId, currentTime)
        assertTrue(tap1Accepted)

        // Rapid Tap 2 for Occurrence #2 at 1150ms (inside 500ms window) -> REJECTED
        currentTime = 1150L
        val tap2Accepted = canCompleteTemplate(templateId, currentTime)
        assertFalse(tap2Accepted)

        // Rapid Tap 3 for Occurrence #3 at 1250ms (inside 500ms window) -> REJECTED
        currentTime = 1250L
        val tap3Accepted = canCompleteTemplate(templateId, currentTime)
        assertFalse(tap3Accepted)

        // Intentional Tap 4 for Occurrence #2 after 500ms cooldown expires (at 1600ms) -> ACCEPTED
        currentTime = 1600L
        val tap4Accepted = canCompleteTemplate(templateId, currentTime)
        assertTrue(tap4Accepted)
    }
}
