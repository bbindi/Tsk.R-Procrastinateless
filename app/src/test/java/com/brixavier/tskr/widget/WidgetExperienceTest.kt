package com.brixavier.tskr.widget

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.personality.ActionType
import com.brixavier.tskr.personality.PersonalityContext
import com.brixavier.tskr.personality.PersonalityIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetExperienceTest {

    private val nowStr = "2026-09-25 12:00"

    private val planWork = Plan(id = "plan-work", name = "Work Tasks", icon = "💼")

    private val oneTimeTask = Reminder(
        id = "r1",
        title = "Submit Taxes",
        date = "2026-09-26",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        planId = planWork.id
    )

    private val oneTimePendingOccurrence = ReminderOccurrence(
        id = "occ-r1-1",
        templateId = "r1",
        title = "Submit Taxes",
        date = "2026-09-26",
        time = "10:00",
        priority = Reminder.PRIORITY_IMPORTANT,
        status = ReminderOccurrence.STATUS_PENDING,
        planId = planWork.id
    )

    private val recurringTaskMaster = Reminder(
        id = "r2",
        title = "Daily Exercise",
        date = "2026-09-20", // Original creation date (past)
        time = "08:00",
        priority = Reminder.PRIORITY_NORMAL,
        recurrenceType = Reminder.RECURRENCE_DAILY,
        recurrenceInterval = 1
    )

    private val recurringCurrentPendingOccurrence = ReminderOccurrence(
        id = "occ-r2-2",
        templateId = "r2",
        title = "Daily Exercise",
        date = "2026-09-26", // Current pending occurrence scheduled date (tomorrow)
        time = "08:00",
        priority = Reminder.PRIORITY_NORMAL,
        status = ReminderOccurrence.STATUS_PENDING
    )

    @Test
    fun testOneTimeTask_mapsCorrectlyToWidgetTaskItem() {
        val templateMap = mapOf(oneTimeTask.id to oneTimeTask)
        val planMap = mapOf(planWork.id to planWork)
        val pendingOccs = listOf(oneTimePendingOccurrence)

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = planMap,
            nowStr = nowStr
        )

        assertEquals(1, widgetItems.size)
        val item = widgetItems.first()
        assertEquals("r1", item.reminderId)
        assertEquals("occ-r1-1", item.occurrenceId)
        assertEquals("Submit Taxes", item.title)
        assertEquals("2026-09-26", item.date)
        assertEquals("10:00", item.time)
        assertTrue(item.isImportant())
        assertFalse(item.isRecurring)
        assertEquals("Work Tasks", item.planName)
    }

    @Test
    fun testRecurringTask_usesPendingOccurrenceDateNotTemplateDate() {
        val templateMap = mapOf(recurringTaskMaster.id to recurringTaskMaster)
        val pendingOccs = listOf(recurringCurrentPendingOccurrence)

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        assertEquals(1, widgetItems.size)
        val item = widgetItems.first()
        assertEquals("r2", item.reminderId)
        assertEquals("occ-r2-2", item.occurrenceId)
        // Master template date was 2026-09-20, but mapped widget date is 2026-09-26 (from current pending occurrence)!
        assertEquals("2026-09-26", item.date)
        assertEquals("08:00", item.time)
        assertTrue(item.isRecurring)
    }

    @Test
    fun testCompletedOccurrences_areExcludedFromActiveWidgetItems() {
        val templateMap = mapOf(recurringTaskMaster.id to recurringTaskMaster)
        val pendingOccs = listOf(recurringCurrentPendingOccurrence) // only pending occ passed

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        assertEquals(1, widgetItems.size)
        assertFalse(widgetItems.any { it.occurrenceId == "occ-completed-old" })
    }

    @Test
    fun testOccurrenceIdAndTemplateId_areBothPreservedInWidgetTaskItem() {
        val templateMap = mapOf(recurringTaskMaster.id to recurringTaskMaster)
        val pendingOccs = listOf(recurringCurrentPendingOccurrence)

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        val item = widgetItems.first()
        assertEquals("r2", item.reminderId)
        assertEquals("occ-r2-2", item.occurrenceId)
    }

    @Test
    fun testSorting_overdueFirst_thenImportant_thenNormal() {
        val overdueOcc = ReminderOccurrence(
            id = "occ-overdue",
            templateId = "r3",
            title = "Overdue Bill",
            date = "2026-09-24", // Past date (yesterday relative to 2026-09-25 12:00)
            time = "10:00",
            priority = Reminder.PRIORITY_NORMAL,
            status = ReminderOccurrence.STATUS_PENDING
        )
        val overdueMaster = Reminder(id = "r3", title = "Overdue Bill", date = "2026-09-24", time = "10:00")

        val templateMap = mapOf(
            oneTimeTask.id to oneTimeTask,
            recurringTaskMaster.id to recurringTaskMaster,
            overdueMaster.id to overdueMaster
        )
        val pendingOccs = listOf(oneTimePendingOccurrence, recurringCurrentPendingOccurrence, overdueOcc)

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        assertEquals(3, widgetItems.size)
        // Overdue item must sort first
        assertEquals("occ-overdue", widgetItems[0].occurrenceId)
        // Then Important item
        assertEquals("occ-r1-1", widgetItems[1].occurrenceId)
        // Then Normal item
        assertEquals("occ-r2-2", widgetItems[2].occurrenceId)
    }

    @Test
    fun testEmptyState_returnedWhenNoPendingOccurrences() {
        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = emptyList(),
            templateMap = emptyMap(),
            planMap = emptyMap(),
            nowStr = nowStr
        )

        assertTrue(widgetItems.isEmpty())
    }

    @Test
    fun testWidgetUiState_countsOverdueTasksAndPreservedPendingTotal() {
        val overdueOcc = ReminderOccurrence(
            id = "occ-overdue",
            templateId = "r3",
            title = "Overdue Bill",
            date = "2026-09-24", // Past date (yesterday relative to 2026-09-25 12:00)
            time = "10:00",
            priority = Reminder.PRIORITY_NORMAL,
            status = ReminderOccurrence.STATUS_PENDING
        )
        val overdueMaster = Reminder(id = "r3", title = "Overdue Bill", date = "2026-09-24", time = "10:00")

        val templateMap = mapOf(
            oneTimeTask.id to oneTimeTask,
            recurringTaskMaster.id to recurringTaskMaster,
            overdueMaster.id to overdueMaster
        )
        val pendingOccs = listOf(oneTimePendingOccurrence, recurringCurrentPendingOccurrence, overdueOcc)

        val widgetItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = pendingOccs,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        val overdueCount = widgetItems.count { "${it.date} ${it.time}" < nowStr }
        assertEquals(1, overdueCount)
        assertEquals(3, widgetItems.size)
    }

    @Test
    fun testLargeWidgetLimit_takesUpTo5Items() {
        val tasks = (1..10).map { i ->
            ReminderOccurrence(
                id = "occ-$i",
                templateId = "r-$i",
                title = "Task $i",
                date = "2026-09-26",
                time = "10:00",
                status = ReminderOccurrence.STATUS_PENDING
            )
        }
        val templateMap = (1..10).associate { i ->
            "r-$i" to Reminder(id = "r-$i", title = "Task $i", date = "2026-09-26", time = "10:00")
        }

        val mappedItems = WidgetDataResolver.mapPendingOccurrencesToWidgetItems(
            pendingOccurrences = tasks,
            templateMap = templateMap,
            planMap = emptyMap(),
            nowStr = nowStr
        )

        val largeWidgetList = mappedItems.take(5)
        assertEquals(5, largeWidgetList.size)
        assertEquals(10, mappedItems.size)
    }

    @Test
    fun testSnoozeOccurrence_5MinutesCalculation_doesNotMutateTemplateRecurrence() {
        val baseOccurrence = ReminderOccurrence(
            id = "occ-snooze-test",
            templateId = "r2",
            title = "Daily Exercise",
            date = "2026-09-25",
            time = "10:00",
            status = ReminderOccurrence.STATUS_PENDING
        )

        val snoozeTime = LocalDateTime.of(2026, 9, 25, 10, 0).plusMinutes(5)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

        val snoozedOccurrence = baseOccurrence.copy(
            date = snoozeTime.format(dateFormatter),
            time = snoozeTime.format(timeFormatter),
            snoozedUntilMs = 123456789L
        )

        assertEquals("2026-09-25", snoozedOccurrence.date)
        assertEquals("10:05", snoozedOccurrence.time)
        assertEquals(123456789L, snoozedOccurrence.snoozedUntilMs)

        // Master template recurrence rules remain 100% unchanged
        assertEquals(Reminder.RECURRENCE_DAILY, recurringTaskMaster.recurrenceType)
        assertEquals(1, recurringTaskMaster.recurrenceInterval)
    }

    @Test
    fun testPersonalityQuote_isDeterministicAndStableForSameDateSeed() {
        val personalityContext = PersonalityContext(
            actionType = ActionType.WIDGET,
            intensity = PersonalityIntensity.ACCOUNTABILITY
        )

        val quote1 = WidgetDataResolver.getStableWidgetPersonalityQuote(personalityContext, "2026-09-25")
        val quote2 = WidgetDataResolver.getStableWidgetPersonalityQuote(personalityContext, "2026-09-25")

        assertEquals(quote1, quote2)
        assertTrue(quote1.isNotBlank())
    }

    @Test
    fun testWidgetUiState_computesPlanProgressPercentCorrectly() {
        val state = WidgetUiState(
            activePlanName = "Fitness Goal",
            activePlanIcon = "🏃",
            activePlanTotalTasks = 5,
            activePlanCompletedTasks = 3
        )

        assertTrue(state.hasActivePlan())
        assertEquals(60, state.getPlanProgressPercent())
    }

    @Test
    fun testWidgetProviderReceiver_matchesExpectedTskRWidgetReceiverClass() {
        val receiverClass = TskRWidgetReceiver::class.java
        assertEquals("com.brixavier.tskr.widget.TskRWidgetReceiver", receiverClass.name)
    }
}
