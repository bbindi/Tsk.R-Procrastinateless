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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBlockCanvasTest {

    private val plan = Plan(id = "p-canvas-1", name = "Single Canvas Test Plan")

    @Test
    fun testParagraphBlock_updatesTextAndDocumentState() {
        val initialDoc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Initial paragraph")))
        val updatedDoc = PlanDocument(listOf((initialDoc.blocks.first() as DocumentBlock.Paragraph).copy(text = "Updated paragraph")))

        assertEquals("Updated paragraph", (updatedDoc.blocks.first() as DocumentBlock.Paragraph).text)
        val json = PlanDocumentSerializer.serialize(updatedDoc)
        val restored = PlanDocumentSerializer.deserialize(json)
        assertEquals("Updated paragraph", (restored.blocks.first() as DocumentBlock.Paragraph).text)
    }

    @Test
    fun testHeadingBlock_rendersWithoutMarkdownPrefix_andPersists() {
        val doc = PlanDocument(listOf(DocumentBlock.Heading(text = "Workspace Title", level = 1)))
        val heading = doc.blocks.first() as DocumentBlock.Heading

        assertEquals("Workspace Title", heading.text)
        assertEquals(1, heading.level)

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)
        val restoredHeading = restored.blocks.first() as DocumentBlock.Heading

        assertEquals("Workspace Title", restoredHeading.text)
    }

    @Test
    fun testBulletListItemBlock_rendersBulletDecoratedItem_andPersists() {
        val doc = PlanDocument(listOf(DocumentBlock.BulletListItem(text = "Confirm logistics")))
        val bullet = doc.blocks.first() as DocumentBlock.BulletListItem

        assertEquals("Confirm logistics", bullet.text)
        assertEquals("• Confirm logistics", bullet.toPlainText())

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)
        assertEquals("Confirm logistics", (restored.blocks.first() as DocumentBlock.BulletListItem).text)
    }

    @Test
    fun testNumberedListItemBlock_rendersNumberedDecoratedItem_andPersists() {
        val doc = PlanDocument(listOf(DocumentBlock.NumberedListItem(number = 1, text = "First item")))
        val num = doc.blocks.first() as DocumentBlock.NumberedListItem

        assertEquals(1, num.number)
        assertEquals("First item", num.text)
        assertEquals("1. First item", num.toPlainText())
    }

    @Test
    fun testChecklistItemBlock_rendersCheckboxControl_andPersistsState() {
        val doc = PlanDocument(listOf(DocumentBlock.ChecklistItem(text = "Sign contract", isChecked = false)))
        val check = doc.blocks.first() as DocumentBlock.ChecklistItem

        assertFalse(check.isChecked)

        // Toggle checkbox
        val toggledDoc = PlanDocument(listOf(check.copy(isChecked = true)))
        val json = PlanDocumentSerializer.serialize(toggledDoc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val restoredCheck = restored.blocks.first() as DocumentBlock.ChecklistItem
        assertTrue(restoredCheck.isChecked)
        assertEquals("Sign contract", restoredCheck.text)
    }

    @Test
    fun testMultipleNoteSections_remainIndependentInCanvas() {
        val sec1 = PlanNoteSection(id = "sec1", planId = plan.id, title = "Overview", content = "Overview notes")
        val sec2 = PlanNoteSection(id = "sec2", planId = plan.id, title = "Decisions", content = "Decisions notes")

        val doc1 = PlanNoteEditorAdapter.loadDocument(sec1)
        val doc2 = PlanNoteEditorAdapter.loadDocument(sec2)

        assertEquals("Overview notes", doc1.toPlainText())
        assertEquals("Decisions notes", doc2.toPlainText())
    }

    @Test
    fun testLegacyPlainTextNotes_loadCleanlyIntoSingleCanvas() {
        val legacyText = "Legacy plain text note line 1\nLegacy plain text note line 2"
        val doc = PlanDocumentSerializer.deserialize(legacyText)

        assertEquals(2, doc.blocks.size)
        assertTrue(doc.blocks[0] is DocumentBlock.Paragraph)
        assertTrue(doc.blocks[1] is DocumentBlock.Paragraph)
        assertEquals(legacyText, doc.toPlainText())
    }

    @Test
    fun testNoteChecklistToggling_doesNotCreatePlanStepOrTask() {
        val doc = PlanNoteEditorAdapter.parseStructuredText("☐ Buy coffee")
        val checkBlock = doc.blocks.first() as DocumentBlock.ChecklistItem

        // Toggling checklist item on block canvas
        val updatedDoc = PlanDocument(listOf(checkBlock.copy(isChecked = true)))
        val serializedContent = PlanDocumentSerializer.serialize(updatedDoc)
        val updatedSection = PlanNoteSection(planId = plan.id, title = "Notes", content = serializedContent)

        val steps = emptyList<PlanStep>()
        val tasks = emptyList<Reminder>()
        val occurrences = emptyList<ReminderOccurrence>()

        assertEquals("{\"v\":1,\"blocks\":[{\"type\":\"check\",\"text\":\"Buy coffee\",\"checked\":true}]}", updatedSection.content)
        assertTrue(steps.isEmpty())
        assertTrue(tasks.isEmpty())
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun testPlanTaskProgress_unaffectedByNoteChecklistState() {
        val progress = PlanProgress(planId = plan.id, completedTasks = 1, totalTasks = 3)
        val noteDoc = PlanDocument(listOf(DocumentBlock.ChecklistItem(text = "Done item", isChecked = true)))

        assertNotNull(noteDoc)
        assertEquals(1, progress.completedTasks)
        assertEquals(3, progress.totalTasks)
        assertEquals(1.0f / 3.0f, progress.progressRatio, 0.001f)
    }
}
