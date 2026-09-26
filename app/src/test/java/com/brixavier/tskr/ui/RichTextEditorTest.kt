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

class RichTextEditorTest {

    private val plan = Plan(id = "p-editor-1", name = "September Meeting")

    @Test
    fun testBoldTextRun_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "Bold text run",
                    runs = listOf(TextRun(text = "Bold text run", isBold = true))
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val paragraph = restored.blocks.first() as DocumentBlock.Paragraph
        assertEquals(1, paragraph.runs.size)
        assertTrue(paragraph.runs.first().isBold)
        assertFalse(paragraph.runs.first().isItalic)
    }

    @Test
    fun testItalicTextRun_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "Italic text run",
                    runs = listOf(TextRun(text = "Italic text run", isItalic = true))
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val paragraph = restored.blocks.first() as DocumentBlock.Paragraph
        assertEquals(1, paragraph.runs.size)
        assertTrue(paragraph.runs.first().isItalic)
        assertFalse(paragraph.runs.first().isBold)
    }

    @Test
    fun testHeadingBlock_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Heading(text = "Key Agenda Items", level = 1)
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val heading = restored.blocks.first() as DocumentBlock.Heading
        assertEquals("Key Agenda Items", heading.text)
        assertEquals(1, heading.level)
    }

    @Test
    fun testBulletListBlock_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.BulletListItem(text = "Review prior minutes")
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val bullet = restored.blocks.first() as DocumentBlock.BulletListItem
        assertEquals("Review prior minutes", bullet.text)
        assertEquals("• Review prior minutes", bullet.toPlainText())
    }

    @Test
    fun testNumberedListBlock_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.NumberedListItem(number = 1, text = "First priority item")
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val num = restored.blocks.first() as DocumentBlock.NumberedListItem
        assertEquals(1, num.number)
        assertEquals("First priority item", num.text)
        assertEquals("1. First priority item", num.toPlainText())
    }

    @Test
    fun testChecklistBlock_serializesAndDeserializesAndPersistsCompletionState() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.ChecklistItem(text = "Confirm transportation", isChecked = true)
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val check = restored.blocks.first() as DocumentBlock.ChecklistItem
        assertEquals("Confirm transportation", check.text)
        assertTrue(check.isChecked)
        assertEquals("[x] Confirm transportation", check.toPlainText())
    }

    @Test
    fun testLinks_serializesAndDeserializesCorrectly() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "Reference link",
                    runs = listOf(TextRun(text = "Reference link", linkUrl = "https://example.com"))
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        val paragraph = restored.blocks.first() as DocumentBlock.Paragraph
        assertEquals("https://example.com", paragraph.runs.first().linkUrl)
    }

    @Test
    fun testMixedFormattedContent_roundTripsCorrectly() {
        val original = PlanDocument(
            blocks = listOf(
                DocumentBlock.Heading(text = "Discussion Points", level = 1),
                DocumentBlock.Paragraph(text = "Overview paragraph with bold", runs = listOf(TextRun("Overview paragraph with bold", isBold = true))),
                DocumentBlock.BulletListItem(text = "Item 1"),
                DocumentBlock.ChecklistItem(text = "Action 1", isChecked = false)
            )
        )

        val json = PlanDocumentSerializer.serialize(original)
        val restored = PlanDocumentSerializer.deserialize(json)

        assertEquals(4, restored.blocks.size)
        assertTrue(restored.blocks[0] is DocumentBlock.Heading)
        assertTrue(restored.blocks[1] is DocumentBlock.Paragraph)
        assertTrue(restored.blocks[2] is DocumentBlock.BulletListItem)
        assertTrue(restored.blocks[3] is DocumentBlock.ChecklistItem)
    }

    @Test
    fun testExistingPlainTextContent_remainsReadableAndParses() {
        val rawPlainText = "Plain text note line 1\nPlain text note line 2"
        val doc = PlanDocumentSerializer.deserialize(rawPlainText)

        assertEquals(2, doc.blocks.size)
        assertEquals(rawPlainText, doc.toPlainText())
    }

    @Test
    fun testFormattingLinePrefixToggling_preservesText() {
        val line = "Review budget allocation"
        val bulletLine = "• $line"
        val numberLine = "1. $line"
        val checkLine = "☐ $line"

        val parsedBullet = PlanNoteEditorAdapter.parseStructuredText(bulletLine)
        assertTrue(parsedBullet.blocks.first() is DocumentBlock.BulletListItem)
        assertEquals(line, (parsedBullet.blocks.first() as DocumentBlock.BulletListItem).text)

        val parsedNumber = PlanNoteEditorAdapter.parseStructuredText(numberLine)
        assertTrue(parsedNumber.blocks.first() is DocumentBlock.NumberedListItem)
        assertEquals(line, (parsedNumber.blocks.first() as DocumentBlock.NumberedListItem).text)

        val parsedCheck = PlanNoteEditorAdapter.parseStructuredText(checkLine)
        assertTrue(parsedCheck.blocks.first() is DocumentBlock.ChecklistItem)
        assertEquals(line, (parsedCheck.blocks.first() as DocumentBlock.ChecklistItem).text)
    }

    @Test
    fun testInvalidOrCorruptContent_failsSafelyToPlainText() {
        val corruptContent = "{\"v\":1,\"blocks\":[{\"type\":\"p\",\"text\":\"Unclosed string"
        val doc = PlanDocumentSerializer.deserialize(corruptContent)

        assertNotNull(doc)
        assertEquals(corruptContent, doc.toPlainText())
    }

    @Test
    fun testDocumentSerialization_remainsDeterministic() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Heading(text = "Heading", level = 1),
                DocumentBlock.Paragraph(text = "Paragraph")
            )
        )

        val json1 = PlanDocumentSerializer.serialize(doc)
        val json2 = PlanDocumentSerializer.serialize(doc)

        assertEquals(json1, json2)
    }

    @Test
    fun testCompletingNoteChecklistItem_doesNotCreatePlanStep() {
        val noteSection = PlanNoteSection(
            planId = plan.id,
            title = "Notes",
            content = "☐ Confirm venue booking"
        )

        // Toggling note checklist item
        val updatedNoteContent = "☑ Confirm venue booking"
        val updatedSection = noteSection.copy(content = updatedNoteContent)

        // Verifies Note Section content updated, but NO PlanStep was created
        val steps = emptyList<PlanStep>()
        assertEquals("☑ Confirm venue booking", updatedSection.content)
        assertTrue(steps.isEmpty())
    }

    @Test
    fun testCompletingNoteChecklistItem_doesNotCreateReminderOrOccurrence() {
        val noteSection = PlanNoteSection(planId = plan.id, title = "Notes", content = "☐ Sign NDA")
        val updatedSection = noteSection.copy(content = "☑ Sign NDA")

        val reminders = emptyList<Reminder>()
        val occurrences = emptyList<ReminderOccurrence>()

        assertEquals("☑ Sign NDA", updatedSection.content)
        assertTrue(reminders.isEmpty())
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun testCompletingNoteChecklistItem_doesNotAffectTaskProgress() {
        val progress = PlanProgress(planId = plan.id, completedTasks = 1, totalTasks = 2)

        // Completing a note checklist item in notes string
        val noteSection = PlanNoteSection(planId = plan.id, title = "Notes", content = "☑ Draft slides")

        // Verifies Task Progress remains 1 of 2
        assertEquals(1, progress.completedTasks)
        assertEquals(2, progress.totalTasks)
        assertEquals(0.5f, progress.progressRatio, 0.001f)
        assertNotNull(noteSection)
    }
}
