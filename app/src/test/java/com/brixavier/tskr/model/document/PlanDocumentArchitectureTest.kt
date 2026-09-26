package com.brixavier.tskr.model.document

import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.ui.PlanNoteEditorAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanDocumentArchitectureTest {

    @Test
    fun testPlainText_convertsIntoValidDocument_andPreservesText() {
        val plainText = "Discuss Q4 Roadmap\nConfirm budget allocation\nSchedule team review"
        val doc = PlanDocument.fromPlainText(plainText)

        assertEquals(3, doc.blocks.size)
        assertTrue(doc.blocks[0] is DocumentBlock.Paragraph)
        assertEquals("Discuss Q4 Roadmap", (doc.blocks[0] as DocumentBlock.Paragraph).text)
        assertEquals(plainText, doc.toPlainText())
    }

    @Test
    fun testSerialization_isDeterministic() {
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(text = "Meeting Overview"),
                DocumentBlock.Heading(text = "Action Items", level = 2),
                DocumentBlock.BulletListItem(text = "Review prior minutes"),
                DocumentBlock.NumberedListItem(number = 1, text = "First priority"),
                DocumentBlock.ChecklistItem(text = "Sign contract", isChecked = true)
            )
        )

        val json1 = PlanDocumentSerializer.serialize(doc)
        val json2 = PlanDocumentSerializer.serialize(doc)

        assertEquals(json1, json2)
        assertTrue(json1.contains("\"v\":1"))
        assertTrue(json1.contains("\"check\""))
    }

    @Test
    fun testRoundTrip_serializeThenDeserialize_preservesDocumentStructureAndContent() {
        val originalDoc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(text = "Paragraph 1"),
                DocumentBlock.Heading(text = "Key Heading", level = 1),
                DocumentBlock.BulletListItem(text = "Bullet Item"),
                DocumentBlock.ChecklistItem(text = "Done item", isChecked = true)
            )
        )

        val json = PlanDocumentSerializer.serialize(originalDoc)
        val restoredDoc = PlanDocumentSerializer.deserialize(json)

        assertEquals(4, restoredDoc.blocks.size)
        assertTrue(restoredDoc.blocks[0] is DocumentBlock.Paragraph)
        assertTrue(restoredDoc.blocks[1] is DocumentBlock.Heading)
        assertTrue(restoredDoc.blocks[2] is DocumentBlock.BulletListItem)
        assertTrue(restoredDoc.blocks[3] is DocumentBlock.ChecklistItem)

        assertEquals("Key Heading", (restoredDoc.blocks[1] as DocumentBlock.Heading).text)
        assertTrue((restoredDoc.blocks[3] as DocumentBlock.ChecklistItem).isChecked)
    }

    @Test
    fun testBackwardCompatibility_existingPlainTextInDatabase_deserializesSafely() {
        val legacyDatabaseContent = "Legacy plain text note created in Phase 3\nNo JSON tags present."
        val doc = PlanDocumentSerializer.deserialize(legacyDatabaseContent)

        assertEquals(2, doc.blocks.size)
        assertEquals(legacyDatabaseContent, doc.toPlainText())
    }

    @Test
    fun testInvalidOrCorruptJson_failsSafelyToPlainTextWithoutCrashing() {
        val corruptJson = "{ \"v\": 1, \"blocks\": [ { \"type\": \"p\", \"text\": \"Unclosed JSON string"
        val doc = PlanDocumentSerializer.deserialize(corruptJson)

        assertNotNull(doc)
        assertEquals(corruptJson, doc.toPlainText())
    }

    @Test
    fun testEmptyContent_handledCorrectly() {
        val emptyDoc = PlanDocumentSerializer.deserialize("")
        assertTrue(emptyDoc.blocks.isEmpty())
        assertEquals("", emptyDoc.toPlainText())

        val serializedEmpty = PlanDocumentSerializer.serialize(emptyDoc)
        assertEquals("", serializedEmpty)
    }

    @Test
    fun testPlanNoteEditorAdapter_toDisplayTextAndToSerializedContent_roundTrip() {
        val section = PlanNoteSection(
            planId = "p1",
            title = "Notes",
            content = "First line of notes\nSecond line of notes"
        )

        val displayText = PlanNoteEditorAdapter.toDisplayText(section)
        assertEquals("First line of notes\nSecond line of notes", displayText)

        val newText = "First line of notes\nSecond line of notes\nThird line added"
        val serialized = PlanNoteEditorAdapter.toSerializedContent(newText, section)

        val updatedSection = section.copy(content = serialized)
        val reloadedText = PlanNoteEditorAdapter.toDisplayText(updatedSection)

        assertEquals(newText, reloadedText)
    }

    @Test
    fun testHyperlink_creationAndRoundTripPersistence_preservesDisplayTextAndUrl() {
        val linkRun = TextRun(text = "JW.org translation guidelines", linkUrl = "https://www.jw.org/en/guidelines")
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "JW.org translation guidelines",
                    runs = listOf(linkRun)
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        assertTrue(json.contains("\"url\":\"https://www.jw.org/en/guidelines\""))

        val restored = PlanDocumentSerializer.deserialize(json)
        assertEquals(1, restored.blocks.size)

        val block = restored.blocks[0] as DocumentBlock.Paragraph
        assertEquals("JW.org translation guidelines", block.text)
        assertEquals(1, block.runs.size)
        assertEquals("https://www.jw.org/en/guidelines", block.runs[0].linkUrl)
    }

    @Test
    fun testHyperlink_syncRunsWithText_preservesLinkUrlDuringTyping() {
        val linkRun = TextRun(text = "JW.org", linkUrl = "https://www.jw.org")
        val runs = listOf(TextRun(text = "Read the "), linkRun, TextRun(text = " now."))
        val oldText = "Read the JW.org now."
        val newText = "Read the JW.org site now." // Inserted " site" inside link run

        val updatedRuns = PlanDocumentEditor.syncRunsWithText(runs, oldText, newText, 19)

        val linkRuns = updatedRuns.filter { !it.linkUrl.isNullOrBlank() }
        assertTrue(linkRuns.isNotEmpty())
        assertEquals("https://www.jw.org", linkRuns[0].linkUrl)
    }

    @Test
    fun testHyperlink_removeLink_preservesDisplayText() {
        val linkRun = TextRun(text = "Guidelines", linkUrl = "https://example.com")
        val runs = listOf(linkRun)

        val updated = PlanDocumentEditor.applyRunStyle(
            runs = runs,
            fullText = "Guidelines",
            rangeStart = 0,
            rangeEnd = 10,
            toggle = PlanDocumentEditor.StyleToggle.LINK,
            linkUrl = null // Remove link
        )

        assertEquals(1, updated.size)
        assertEquals("Guidelines", updated[0].text)
        assertEquals(null, updated[0].linkUrl)
    }

    @Test
    fun testBackwardCompatibility_legacyBoldAndItalicAndHeadingJson_deserializesSafely() {
        val legacyJson = "{\"v\":1,\"blocks\":[{\"type\":\"h\",\"text\":\"Main Title\",\"level\":1,\"runs\":[{\"text\":\"Main Title\",\"b\":true}]},{\"type\":\"p\",\"text\":\"Body text\",\"runs\":[{\"text\":\"Body \",\"b\":true},{\"text\":\"text\",\"i\":true}]}]}"

        val restored = PlanDocumentSerializer.deserialize(legacyJson)

        assertEquals(2, restored.blocks.size)
        assertEquals("Main Title", restored.blocks[0].toPlainText())
        assertEquals("Body text", restored.blocks[1].toPlainText())
    }

    @Test
    fun testHyperlink_rawUrlAssertion_noMarkdownSyntaxInLinkUrl() {
        val rawUrl = "https://www.jw.org/en/guidelines"
        val linkRun = TextRun(text = "JW.org translation guidelines", linkUrl = rawUrl)
        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "JW.org translation guidelines",
                    runs = listOf(linkRun)
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        // Assert raw URL in JSON and TextRun
        assertEquals(rawUrl, linkRun.linkUrl)
        assertTrue(json.contains("\"url\":\"$rawUrl\""))
        assertFalse(json.contains("[https://"))

        val restored = PlanDocumentSerializer.deserialize(json)
        val block = restored.blocks.first() as DocumentBlock.Paragraph
        assertEquals(rawUrl, block.runs.first().linkUrl)
    }

    @Test
    fun testHyperlink_multipleLinksInSameParagraph_retainsDistinctRawUrls() {
        val url1 = "https://www.jw.org/en/guidelines"
        val url2 = "https://example.com/docs"
        val runs = listOf(
            TextRun(text = "Read "),
            TextRun(text = "JW.org guidelines", linkUrl = url1),
            TextRun(text = " and "),
            TextRun(text = "Example Docs", linkUrl = url2)
        )

        val doc = PlanDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    text = "Read JW.org guidelines and Example Docs",
                    runs = runs
                )
            )
        )

        val json = PlanDocumentSerializer.serialize(doc)
        val restored = PlanDocumentSerializer.deserialize(json)
        val block = restored.blocks.first() as DocumentBlock.Paragraph

        val linkRuns = block.runs.filter { !it.linkUrl.isNullOrBlank() }
        assertEquals(2, linkRuns.size)
        assertEquals("JW.org guidelines", linkRuns[0].text)
        assertEquals(url1, linkRuns[0].linkUrl)
        assertEquals("Example Docs", linkRuns[1].text)
        assertEquals(url2, linkRuns[1].linkUrl)
    }

    @Test
    fun testHyperlink_updateExistingUrl_preservesDisplayTextAndUpdatesRawUrl() {
        val oldUrl = "https://example.com/old"
        val newUrl = "https://example.com/new"
        val runs = listOf(TextRun(text = "Documentation", linkUrl = oldUrl))

        val updated = PlanDocumentEditor.applyRunStyle(
            runs = runs,
            fullText = "Documentation",
            rangeStart = 0,
            rangeEnd = 13,
            toggle = PlanDocumentEditor.StyleToggle.LINK,
            linkUrl = newUrl
        )

        assertEquals(1, updated.size)
        assertEquals("Documentation", updated[0].text)
        assertEquals(newUrl, updated[0].linkUrl)
    }

    @Test
    fun testHyperlink_blockSplittingAndMerging_preservesLinkBoundaries() {
        val linkRun = TextRun(text = "JW.org guidelines", linkUrl = "https://www.jw.org")
        val runs = listOf(TextRun("Read "), linkRun, TextRun(" today."))
        val text = "Read JW.org guidelines today."
        val doc = PlanDocument(listOf(DocumentBlock.Paragraph(text = text, runs = runs)))

        // Split block at index 18 (inside "JW.org guidelines")
        val (splitDoc, _) = PlanDocumentEditor.handleEnterKey(doc, 0, 18)
        assertEquals(2, splitDoc.blocks.size)

        val headBlock = splitDoc.blocks[0] as DocumentBlock.Paragraph
        val tailBlock = splitDoc.blocks[1] as DocumentBlock.Paragraph

        assertTrue(headBlock.runs.any { !it.linkUrl.isNullOrBlank() })
        assertTrue(tailBlock.runs.any { !it.linkUrl.isNullOrBlank() })

        // Merge back via backspace
        val (mergedDoc, _) = PlanDocumentEditor.handleBackspaceKey(splitDoc, 1)
        assertEquals(1, mergedDoc.blocks.size)
        val mergedBlock = mergedDoc.blocks[0] as DocumentBlock.Paragraph
        assertEquals(text, mergedBlock.text)
        assertTrue(mergedBlock.runs.any { it.linkUrl == "https://www.jw.org" })
    }
}
