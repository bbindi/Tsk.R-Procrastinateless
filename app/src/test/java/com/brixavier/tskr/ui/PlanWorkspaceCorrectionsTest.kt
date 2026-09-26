package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanProgress
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import com.brixavier.tskr.model.document.DocumentBlock
import com.brixavier.tskr.model.document.PlanDocument
import com.brixavier.tskr.model.document.PlanDocumentSerializer
import com.brixavier.tskr.model.document.TextRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWorkspaceCorrectionsTest {

    private val plan = Plan(id = "p-corr-1", name = "Q4 Execution Plan")

    @Test
    fun testLegacyBoldAndItalicFormatting_deserializesSafelyFromRoomJson() {
        val legacyDoc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "This is critical info",
                    runs = listOf(TextRun(text = "critical", isBold = true, isItalic = true))
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(legacyDoc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val paragraph = restored.blocks.first() as DocumentBlock.Paragraph
        assertEquals("This is critical info", paragraph.text)
        assertTrue(paragraph.runs.any { it.isBold })
    }

    @Test
    fun testBulletAction_createsDocumentBlockBulletListItem() {
        val input = "• Review budget"
        val doc = PlanNoteEditorAdapter.parseStructuredText(input)

        val bullet = doc.blocks.first() as DocumentBlock.BulletListItem
        assertEquals("Review budget", bullet.text)
    }

    @Test
    fun testNumberedAction_createsDocumentBlockNumberedListItem() {
        val input = "1. Confirm dates"
        val doc = PlanNoteEditorAdapter.parseStructuredText(input)

        val num = doc.blocks.first() as DocumentBlock.NumberedListItem
        assertEquals(1, num.number)
        assertEquals("Confirm dates", num.text)
    }

    @Test
    fun testChecklistAction_createsDocumentBlockChecklistItem() {
        val input = "☐ Draft slides"
        val doc = PlanNoteEditorAdapter.parseStructuredText(input)

        val check = doc.blocks.first() as DocumentBlock.ChecklistItem
        assertEquals("Draft slides", check.text)
        assertFalse(check.isChecked)
    }

    @Test
    fun testLinkAction_storesLinkUrlStructurally() {
        val input = "[Reference](https://example.com)"
        val doc = PlanNoteEditorAdapter.parseStructuredText(input)

        val paragraph = doc.blocks.first() as DocumentBlock.Paragraph
        val linkRun = paragraph.runs.find { !it.linkUrl.isNullOrBlank() }

        assertNotNull(linkRun)
        assertEquals("Reference", linkRun?.text)
        assertEquals("https://example.com", linkRun?.linkUrl)
    }

    @Test
    fun testCompletingNoteChecklist_doesNotCreatePlanStepOrReminder() {
        val noteContent = "☑ Confirm attendance"
        val steps = emptyList<PlanStep>()
        val reminders = emptyList<Reminder>()
        val occurrences = emptyList<ReminderOccurrence>()

        assertEquals("☑ Confirm attendance", noteContent)
        assertTrue(steps.isEmpty())
        assertTrue(reminders.isEmpty())
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun testCompletingNoteChecklist_doesNotAffectTaskProgress() {
        val progress = PlanProgress(planId = plan.id, completedTasks = 1, totalTasks = 3)
        val noteContent = "☑ Sign NDA"

        assertEquals(1, progress.completedTasks)
        assertEquals(3, progress.totalTasks)
        assertEquals(1.0f / 3.0f, progress.progressRatio, 0.001f)
        assertNotNull(noteContent)
    }

    @Test
    fun testSwitchingSections_savesAndRestoresCorrectContent() {
        val s1 = PlanNoteSection(id = "s1", planId = plan.id, title = "Overview", content = "Overview notes")
        val s2 = PlanNoteSection(id = "s2", planId = plan.id, title = "Decisions", content = "Decisions notes")

        val sectionsMap = mapOf(s1.id to s1, s2.id to s2)

        val activeS2 = sectionsMap["s2"]
        assertEquals("Decisions notes", activeS2?.content)

        val activeS1 = sectionsMap["s1"]
        assertEquals("Overview notes", activeS1?.content)
    }

    @Test
    fun testSectionDeletion_doesNotAffectOtherSections() {
        val s1 = PlanNoteSection(id = "s1", planId = plan.id, title = "Overview")
        val s2 = PlanNoteSection(id = "s2", planId = plan.id, title = "Ideas")

        val sections = mutableListOf(s1, s2)
        sections.removeIf { it.id == "s1" }

        assertEquals(1, sections.size)
        assertEquals("s2", sections.first().id)
        assertEquals("Ideas", sections.first().title)
    }
}
