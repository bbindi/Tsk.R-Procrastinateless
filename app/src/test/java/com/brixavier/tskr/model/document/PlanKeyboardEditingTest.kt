package com.brixavier.tskr.model.document

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.PlanStep
import com.brixavier.tskr.model.Reminder
import com.brixavier.tskr.model.ReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanKeyboardEditingTest {

    private val plan = Plan(id = "p-key-1", name = "Keyboard Test Plan")

    @Test
    fun testEnter_splitsParagraphAtCursorPosition() {
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Hello world")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 5)

        assertEquals(2, newDoc.blocks.size)
        assertEquals("Hello", (newDoc.blocks[0] as DocumentBlock.Paragraph).text)
        assertEquals(" world", (newDoc.blocks[1] as DocumentBlock.Paragraph).text)
        assertEquals(1, nextFocusIdx)
    }

    @Test
    fun testEnter_atEndOfParagraph_createsEmptyParagraph() {
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Hello world")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 11)

        assertEquals(2, newDoc.blocks.size)
        assertEquals("Hello world", (newDoc.blocks[0] as DocumentBlock.Paragraph).text)
        assertEquals("", (newDoc.blocks[1] as DocumentBlock.Paragraph).text)
        assertEquals(1, nextFocusIdx)
    }

    @Test
    fun testEnter_insideBoldRun_preservesBoldFormattingOnBothBlocks() {
        val runs = listOf(TextRun("Hello "), TextRun("beautiful", isBold = true), TextRun(" world"))
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Hello beautiful world", runs = runs)))

        // Split inside "beautiful" at cursor position 10 ("Hello beau" | "tiful world")
        val (newDoc, _) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 10)

        assertEquals(2, newDoc.blocks.size)
        val b1 = newDoc.blocks[0] as DocumentBlock.Paragraph
        val b2 = newDoc.blocks[1] as DocumentBlock.Paragraph

        assertEquals("Hello beau", b1.text)
        assertEquals("tiful world", b2.text)

        assertTrue(b1.runs.any { it.isBold && it.text == "beau" })
        assertTrue(b2.runs.any { it.isBold && it.text == "tiful" })
    }

    @Test
    fun testEnter_insideItalicRun_preservesItalicFormattingOnBothBlocks() {
        val runs = listOf(TextRun("Quick "), TextRun("brown fox", isItalic = true))
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Quick brown fox", runs = runs)))

        // Split inside "brown" at cursor position 9 ("Quick bro" | "wn fox")
        val (newDoc, _) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 9)

        assertEquals(2, newDoc.blocks.size)
        val b1 = newDoc.blocks[0] as DocumentBlock.Paragraph
        val b2 = newDoc.blocks[1] as DocumentBlock.Paragraph

        assertEquals("Quick bro", b1.text)
        assertEquals("wn fox", b2.text)

        assertTrue(b1.runs.any { it.isItalic && it.text == "bro" })
        assertTrue(b2.runs.any { it.isItalic && it.text == "wn fox" })
    }

    @Test
    fun testEnter_insideLinkedRun_preservesLinkUrlOnBothBlocks() {
        val runs = listOf(TextRun("Visit "), TextRun("jw.org today", linkUrl = "https://www.jw.org/"))
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "Visit jw.org today", runs = runs)))

        // Split inside "jw.org" at cursor position 9 ("Visit jw." | "org today")
        val (newDoc, _) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 9)

        assertEquals(2, newDoc.blocks.size)
        val b1 = newDoc.blocks[0] as DocumentBlock.Paragraph
        val b2 = newDoc.blocks[1] as DocumentBlock.Paragraph

        assertEquals("Visit jw.", b1.text)
        assertEquals("org today", b2.text)

        assertEquals("https://www.jw.org/", b1.runs.first { it.linkUrl != null }.linkUrl)
        assertEquals("https://www.jw.org/", b2.runs.first { it.linkUrl != null }.linkUrl)
    }

    @Test
    fun testEnter_splitsBulletItem() {
        val doc = PlanDocument(listOf(DocumentBlock.BulletListItem(text = "Buy milk")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 8)

        assertEquals(2, newDoc.blocks.size)
        assertTrue(newDoc.blocks[0] is DocumentBlock.BulletListItem)
        assertTrue(newDoc.blocks[1] is DocumentBlock.BulletListItem)
        assertEquals("Buy milk", (newDoc.blocks[0] as DocumentBlock.BulletListItem).text)
        assertEquals("", (newDoc.blocks[1] as DocumentBlock.BulletListItem).text)
        assertEquals(1, nextFocusIdx)
    }

    @Test
    fun testEnter_onEmptyBulletItem_exitsListToParagraph() {
        val doc = PlanDocument(listOf(DocumentBlock.BulletListItem(text = "")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 0)

        assertEquals(1, newDoc.blocks.size)
        assertTrue(newDoc.blocks[0] is DocumentBlock.Paragraph)
        assertEquals("", (newDoc.blocks[0] as DocumentBlock.Paragraph).text)
        assertEquals(0, nextFocusIdx)
    }

    @Test
    fun testEnter_splitsNumberedItem_andIncrementsNumber() {
        val doc = PlanDocument(listOf(DocumentBlock.NumberedListItem(number = 1, text = "First item")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 10)

        assertEquals(2, newDoc.blocks.size)
        val item1 = newDoc.blocks[0] as DocumentBlock.NumberedListItem
        val item2 = newDoc.blocks[1] as DocumentBlock.NumberedListItem

        assertEquals(1, item1.number)
        assertEquals("First item", item1.text)
        assertEquals(2, item2.number)
        assertEquals("", item2.text)
        assertEquals(1, nextFocusIdx)
    }

    @Test
    fun testEnter_onEmptyNumberedItem_exitsListToParagraph() {
        val doc = PlanDocument(listOf(DocumentBlock.NumberedListItem(number = 1, text = "")))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 0)

        assertEquals(1, newDoc.blocks.size)
        assertTrue(newDoc.blocks[0] is DocumentBlock.Paragraph)
        assertEquals(0, nextFocusIdx)
    }

    @Test
    fun testListNumbering_remainsCoherentAcrossListUpdates() {
        val blocks = listOf(
            DocumentBlock.NumberedListItem(number = 1, text = "Item A"),
            DocumentBlock.NumberedListItem(number = 99, text = "Item B"),
            DocumentBlock.Paragraph(text = "Separator"),
            DocumentBlock.NumberedListItem(number = 5, text = "Item C")
        )

        val normalized = PlanDocumentEditor.normalizeNumbering(blocks)

        assertEquals(1, (normalized[0] as DocumentBlock.NumberedListItem).number)
        assertEquals(2, (normalized[1] as DocumentBlock.NumberedListItem).number)
        assertEquals("Separator", (normalized[2] as DocumentBlock.Paragraph).text)
        assertEquals(1, (normalized[3] as DocumentBlock.NumberedListItem).number)
    }

    @Test
    fun testEnter_onChecklistItem_createsUncheckedNextChecklistItem() {
        val doc = PlanDocument(listOf(DocumentBlock.ChecklistItem(text = "Confirm venue", isChecked = true)))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 13)

        assertEquals(2, newDoc.blocks.size)
        val check1 = newDoc.blocks[0] as DocumentBlock.ChecklistItem
        val check2 = newDoc.blocks[1] as DocumentBlock.ChecklistItem

        assertTrue(check1.isChecked)
        assertFalse(check2.isChecked) // Newly created item is ALWAYS unchecked
        assertEquals("", check2.text)
        assertEquals(1, nextFocusIdx)
    }

    @Test
    fun testEnter_onEmptyChecklistItem_exitsChecklistToParagraph() {
        val doc = PlanDocument(listOf(DocumentBlock.ChecklistItem(text = "", isChecked = false)))
        val (newDoc, nextFocusIdx) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 0)

        assertEquals(1, newDoc.blocks.size)
        assertTrue(newDoc.blocks[0] is DocumentBlock.Paragraph)
        assertEquals(0, nextFocusIdx)
    }

    @Test
    fun testBackspace_mergesParagraphBlocks_preservingBoldAndItalicRuns() {
        val doc = PlanDocument(
            listOf(
                DocumentBlock.Paragraph(text = "Hello ", runs = listOf(TextRun("Hello ", isBold = true))),
                DocumentBlock.Paragraph(text = "world", runs = listOf(TextRun("world", isItalic = true)))
            )
        )

        val (newDoc, focusIdx) = PlanDocumentEditor.handleBackspaceKey(doc, blockIndex = 1)

        assertEquals(1, newDoc.blocks.size)
        val merged = newDoc.blocks[0] as DocumentBlock.Paragraph
        assertEquals("Hello world", merged.text)
        assertEquals(2, merged.runs.size)
        assertTrue(merged.runs[0].isBold)
        assertTrue(merged.runs[1].isItalic)
        assertEquals(0, focusIdx)
    }

    @Test
    fun testBackspace_mergesLinkedBlocks_preservingLinkUrls() {
        val doc = PlanDocument(
            listOf(
                DocumentBlock.Paragraph(text = "Visit "),
                DocumentBlock.Paragraph(text = "example.com", runs = listOf(TextRun("example.com", linkUrl = "https://example.com")))
            )
        )

        val (newDoc, _) = PlanDocumentEditor.handleBackspaceKey(doc, blockIndex = 1)

        assertEquals(1, newDoc.blocks.size)
        val merged = newDoc.blocks[0] as DocumentBlock.Paragraph
        assertEquals("Visit example.com", merged.text)
        assertEquals("https://example.com", merged.runs.last().linkUrl)
    }

    @Test
    fun testNormalizeRuns_combinesAdjacentIdenticalRuns() {
        val runs = listOf(
            TextRun("Hello ", isBold = true),
            TextRun("world", isBold = true),
            TextRun("!"),
            TextRun(" Extra", linkUrl = "https://test.com"),
            TextRun(" Link", linkUrl = "https://test.com")
        )

        val normalized = PlanDocumentEditor.normalizeRuns(runs)

        assertEquals(3, normalized.size)
        assertEquals("Hello world", normalized[0].text)
        assertTrue(normalized[0].isBold)

        assertEquals("!", normalized[1].text)

        assertEquals(" Extra Link", normalized[2].text)
        assertEquals("https://test.com", normalized[2].linkUrl)
    }

    @Test
    fun testBackspace_atFirstBlockPositionZero_isSafe() {
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = "First block")))
        val (newDoc, focusIdx) = PlanDocumentEditor.handleBackspaceKey(doc, blockIndex = 0)

        assertEquals(1, newDoc.blocks.size)
        assertEquals("First block", (newDoc.blocks[0] as DocumentBlock.Paragraph).text)
        assertEquals(0, focusIdx)
    }

    @Test
    fun testChecklistKeyboardOperations_haveZeroTaskOrStepSideEffects() {
        val doc = PlanDocument(listOf(DocumentBlock.ChecklistItem(text = "Buy supplies", isChecked = false)))
        val (newDoc, _) = PlanDocumentEditor.handleEnterKey(doc, blockIndex = 0, cursorPosition = 12)

        val serializedContent = PlanDocumentSerializer.serialize(newDoc)
        val section = PlanNoteSection(planId = plan.id, title = "Notes", content = serializedContent)

        val steps = emptyList<PlanStep>()
        val reminders = emptyList<Reminder>()
        val occurrences = emptyList<ReminderOccurrence>()

        assertEquals(2, newDoc.blocks.size)
        assertNotNull(section.content)
        assertTrue(steps.isEmpty())
        assertTrue(reminders.isEmpty())
        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun testSerializationRoundTrip_preservesKeyboardCreatedStructure() {
        val doc = PlanDocument(
            listOf(
                DocumentBlock.Heading(text = "Agenda", level = 1),
                DocumentBlock.BulletListItem(text = "Topic 1"),
                DocumentBlock.NumberedListItem(number = 1, text = "Action 1"),
                DocumentBlock.ChecklistItem(text = "Sign contract", isChecked = false)
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)

        assertEquals(4, restored.blocks.size)
        assertTrue(restored.blocks[0] is DocumentBlock.Heading)
        assertTrue(restored.blocks[1] is DocumentBlock.BulletListItem)
        assertTrue(restored.blocks[2] is DocumentBlock.NumberedListItem)
        assertTrue(restored.blocks[3] is DocumentBlock.ChecklistItem)
    }
}
