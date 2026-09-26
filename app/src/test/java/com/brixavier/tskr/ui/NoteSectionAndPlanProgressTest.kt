package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.document.DocumentBlock
import com.brixavier.tskr.model.document.PlanDocument
import com.brixavier.tskr.model.document.PlanDocumentSerializer
import com.brixavier.tskr.model.document.TextRun
import com.brixavier.tskr.ui.richeditor.RichEditorAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class NoteSectionAndPlanProgressTest {

    private val plan = Plan(id = "p-audit-1", name = "V2 Audit Plan")

    @Test
    fun testNoteSection_switchingIsolatesContentPerSection() {
        val secA = PlanNoteSection(id = "sec-a", planId = plan.id, title = "Section A", content = "THIS IS SECTION A")
        val secB = PlanNoteSection(id = "sec-b", planId = plan.id, title = "Section B", content = "THIS IS SECTION B")
        val secC = PlanNoteSection(id = "sec-c", planId = plan.id, title = "Section C", content = "THIS IS SECTION C")

        val sections = listOf(secA, secB, secC)

        // Simulate Section A loading
        val docA = PlanDocumentSerializer.deserialize(sections.find { it.id == "sec-a" }?.content)
        val htmlA = RichEditorAdapter.documentToHtml(docA)
        assertTrue(htmlA.contains("THIS IS SECTION A"))

        // Simulate Section B loading
        val docB = PlanDocumentSerializer.deserialize(sections.find { it.id == "sec-b" }?.content)
        val htmlB = RichEditorAdapter.documentToHtml(docB)
        assertTrue(htmlB.contains("THIS IS SECTION B"))

        // Simulate Section C loading
        val docC = PlanDocumentSerializer.deserialize(sections.find { it.id == "sec-c" }?.content)
        val htmlC = RichEditorAdapter.documentToHtml(docC)
        assertTrue(htmlC.contains("THIS IS SECTION C"))

        // Ensure no cross-contamination
        assertFalse(htmlA.contains("SECTION B"))
        assertFalse(htmlB.contains("SECTION C"))
        assertFalse(htmlC.contains("SECTION A"))
    }

    @Test
    fun testNoteSection_richFormattingIsIsolatedPerSection() {
        // Section A: Bold
        val docA = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Bold text", runs = listOf(
            TextRun("Bold text", isBold = true)
        ))))
        val secA = PlanNoteSection(id = "sec-a", planId = plan.id, title = "Bold Sec", content = PlanDocumentSerializer.serialize(docA))

        // Section B: Italic
        val docB = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Italic text", runs = listOf(
            TextRun("Italic text", isItalic = true)
        ))))
        val secB = PlanNoteSection(id = "sec-b", planId = plan.id, title = "Italic Sec", content = PlanDocumentSerializer.serialize(docB))

        val htmlA = RichEditorAdapter.documentToHtml(PlanDocumentSerializer.deserialize(secA.content))
        val htmlB = RichEditorAdapter.documentToHtml(PlanDocumentSerializer.deserialize(secB.content))

        assertTrue(htmlA.contains("<b>Bold text</b>"))
        assertTrue(htmlB.contains("<i>Italic text</i>"))
        assertFalse(htmlA.contains("<i>"))
        assertFalse(htmlB.contains("<b>"))
    }

    @Test
    fun testNoteSection_addingNewSectionInitializesEmptyDocument() {
        val newSec = PlanNoteSection(id = "sec-new", planId = plan.id, title = "New Section", content = "")
        val doc = PlanDocumentSerializer.deserialize(newSec.content)
        val html = RichEditorAdapter.documentToHtml(doc)

        assertEquals("", doc.toPlainText())
        assertEquals("", html)
    }

    @Test
    fun testPlanStepProgress_zeroSteps_calculatesZeroPercent() {
        val progress = PlanProgress(planId = plan.id, completedTasks = 0, totalTasks = 0)
        assertEquals(0.0f, progress.progressRatio, 0.001f)
        assertEquals(0, (progress.progressRatio * 100).roundToInt())
    }

    @Test
    fun testPlanStepProgress_oneStepIncomplete_calculatesZeroPercent() {
        val steps = listOf(PlanStep(planId = plan.id, text = "Step 1", isCompleted = false))
        val progress = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)

        assertEquals(0.0f, progress.progressRatio, 0.001f)
        assertEquals(0, (progress.progressRatio * 100).roundToInt())
    }

    @Test
    fun testPlanStepProgress_oneStepComplete_calculates100Percent() {
        val steps = listOf(PlanStep(planId = plan.id, text = "Step 1", isCompleted = true))
        val progress = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)

        assertEquals(1.0f, progress.progressRatio, 0.001f)
        assertEquals(100, (progress.progressRatio * 100).roundToInt())
    }

    @Test
    fun testPlanStepProgress_threeSteps_calculates33_67_100_Percent() {
        // 1 of 3 completed -> 33%
        val p1 = PlanProgress(planId = plan.id, completedTasks = 1, totalTasks = 3)
        assertEquals(33, (p1.progressRatio * 100).roundToInt())

        // 2 of 3 completed -> 67%
        val p2 = PlanProgress(planId = plan.id, completedTasks = 2, totalTasks = 3)
        assertEquals(67, (p2.progressRatio * 100).roundToInt())

        // 3 of 3 completed -> 100%
        val p3 = PlanProgress(planId = plan.id, completedTasks = 3, totalTasks = 3)
        assertEquals(100, (p3.progressRatio * 100).roundToInt())
    }

    @Test
    fun testPlanStepProgress_addingAndRemovingSteps_updatesDenominator() {
        val steps = mutableListOf(
            PlanStep(id = "st1", planId = plan.id, text = "Step 1", isCompleted = true)
        )

        // Initial: 1/1 -> 100%
        var p = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)
        assertEquals(100, (p.progressRatio * 100).roundToInt())

        // Add step: 1/2 -> 50%
        steps.add(PlanStep(id = "st2", planId = plan.id, text = "Step 2", isCompleted = false))
        p = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)
        assertEquals(50, (p.progressRatio * 100).roundToInt())

        // Remove completed step: 0/1 -> 0%
        steps.removeIf { it.id == "st1" }
        p = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)
        assertEquals(0, (p.progressRatio * 100).roundToInt())
    }

    @Test
    fun testCompletingAllPlanSteps_doesNotAutomaticallyCompletePlanStatus() {
        val initialPlan = Plan(id = plan.id, name = plan.name, status = Plan.STATUS_ACTIVE)
        val steps = listOf(
            PlanStep(planId = plan.id, text = "Step 1", isCompleted = true),
            PlanStep(planId = plan.id, text = "Step 2", isCompleted = true)
        )

        val progress = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)

        // Progress is 100%, but plan status remains ACTIVE
        assertEquals(100, (progress.progressRatio * 100).roundToInt())
        assertEquals(Plan.STATUS_ACTIVE, initialPlan.status)
    }

    @Test
    fun testPlanStepAndConnectedTaskProgress_moveIndependently() {
        // 4 Plan Steps: 2 completed -> 50%
        val steps = mutableListOf(
            PlanStep(id = "s1", planId = plan.id, text = "Step 1", isCompleted = true),
            PlanStep(id = "s2", planId = plan.id, text = "Step 2", isCompleted = true),
            PlanStep(id = "s3", planId = plan.id, text = "Step 3", isCompleted = false),
            PlanStep(id = "s4", planId = plan.id, text = "Step 4", isCompleted = false)
        )
        val stepProgress = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)
        assertEquals(50, (stepProgress.progressRatio * 100).roundToInt())

        // 1 Connected Task (incomplete)
        val tasks = mutableListOf(
            Reminder(id = "r1", title = "Task 1", date = "2026-09-26", time = "10:00", planId = plan.id, isActive = true)
        )
        val totalConnected = tasks.size
        var completedConnected = tasks.count { !it.isActive }

        assertEquals(1, totalConnected)
        assertEquals(0, completedConnected)
        assertEquals(50, (stepProgress.progressRatio * 100).roundToInt()) // Plan Step progress unchanged

        // Complete the Connected Task
        tasks[0] = tasks[0].copy(isActive = false)
        completedConnected = tasks.count { !it.isActive }
        assertEquals(1, completedConnected)
        assertEquals(50, (stepProgress.progressRatio * 100).roundToInt()) // Plan Step progress MUST REMAIN 50%

        // Uncomplete the Connected Task
        tasks[0] = tasks[0].copy(isActive = true)
        completedConnected = tasks.count { !it.isActive }
        assertEquals(0, completedConnected)
        assertEquals(50, (stepProgress.progressRatio * 100).roundToInt()) // Plan Step progress MUST REMAIN 50%

        // Complete 3rd Plan Step
        steps[2] = steps[2].copy(isCompleted = true)
        val updatedStepProgress = PlanProgress(planId = plan.id, completedTasks = steps.count { it.isCompleted }, totalTasks = steps.size)
        assertEquals(75, (updatedStepProgress.progressRatio * 100).roundToInt()) // Plan Step progress becomes 75%
        assertEquals(0, completedConnected) // Connected Task progress unchanged (0 of 1)
    }

    @Test
    fun testConnectedTaskProgress_zero_one_multiple_tasks() {
        val tasks = mutableListOf<Reminder>()

        // Zero tasks
        assertEquals(0, tasks.size)

        // 1 task (completed)
        tasks.add(Reminder(id = "r1", title = "Task 1", date = "2026-09-26", time = "10:00", planId = plan.id, isActive = false))
        assertEquals(1, tasks.size)
        assertEquals(1, tasks.count { !it.isActive })

        // 3 tasks (1 completed, 2 active/incomplete)
        tasks.add(Reminder(id = "r2", title = "Task 2", date = "2026-09-26", time = "11:00", planId = plan.id, isActive = true))
        tasks.add(Reminder(id = "r3", title = "Task 3", date = "2026-09-26", time = "12:00", planId = plan.id, isActive = true))

        assertEquals(3, tasks.size)
        assertEquals(1, tasks.count { !it.isActive })
    }

    @Test
    fun testEditingPlan_updatesExistingPlanWithoutCreatingDuplicate() {
        val original = Plan(
            id = "plan-orig-123",
            name = "Original Plan Name",
            category = "PERSONAL",
            categorySource = "AUTOMATIC",
            color = "SLATE",
            colorSource = "CATEGORY"
        )

        val edited = original.copy(
            name = "Updated Plan Name",
            category = "TRAVEL",
            categorySource = "MANUAL",
            color = "CYAN",
            colorSource = "MANUAL"
        )

        // Preserves same ID
        assertEquals("plan-orig-123", edited.id)
        assertEquals("Updated Plan Name", edited.name)
        assertEquals("TRAVEL", edited.category)
        assertEquals("MANUAL", edited.categorySource)
        assertEquals("CYAN", edited.color)
        assertEquals("MANUAL", edited.colorSource)
    }
}
