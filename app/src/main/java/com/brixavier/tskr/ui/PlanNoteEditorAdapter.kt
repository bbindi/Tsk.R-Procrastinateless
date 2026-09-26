package com.brixavier.tskr.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.document.DocumentBlock
import com.brixavier.tskr.model.document.PlanDocument
import com.brixavier.tskr.model.document.PlanDocumentEditor
import com.brixavier.tskr.model.document.PlanDocumentSerializer
import com.brixavier.tskr.model.document.TextRun

/**
 * Adapter providing a clean boundary between Room string persistence,
 * [PlanDocument] domain model, and UI text editor state.
 */
object PlanNoteEditorAdapter {

    /**
     * Reads a [PlanNoteSection] and returns its parsed [PlanDocument] domain model.
     */
    fun loadDocument(section: PlanNoteSection?): PlanDocument {
        return PlanDocumentSerializer.deserialize(section?.content)
    }

    /**
     * Extracts displayable text from a [PlanNoteSection] for text editing.
     */
    fun toDisplayText(section: PlanNoteSection?): String {
        val doc = loadDocument(section)
        return doc.toPlainText()
    }

    /**
     * Converts a [PlanDocument] into a Jetpack Compose [AnnotatedString] with rich text styling and link annotations.
     */
    fun toAnnotatedString(document: PlanDocument): AnnotatedString {
        return buildAnnotatedString {
            document.blocks.forEachIndexed { blockIndex, block ->
                if (blockIndex > 0) append("\n")

                when (block) {
                    is DocumentBlock.Paragraph -> {
                        appendRuns(block.text, block.runs)
                    }
                    is DocumentBlock.Heading -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            appendRuns(block.text, block.runs)
                        }
                    }
                    is DocumentBlock.BulletListItem -> {
                        append("• ")
                        appendRuns(block.text, block.runs)
                    }
                    is DocumentBlock.NumberedListItem -> {
                        append("${block.number}. ")
                        appendRuns(block.text, block.runs)
                    }
                    is DocumentBlock.ChecklistItem -> {
                        append(if (block.isChecked) "☑ " else "☐ ")
                        if (block.isChecked) {
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                appendRuns(block.text, block.runs)
                            }
                        } else {
                            appendRuns(block.text, block.runs)
                        }
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendRuns(defaultText: String, runs: List<TextRun>) {
        val effectiveRuns = if (runs.isEmpty()) parseInlineRuns(defaultText) else runs
        if (effectiveRuns.isEmpty()) {
            append(defaultText)
            return
        }

        effectiveRuns.forEach { run ->
            var style = SpanStyle()
            if (run.isBold) style = style.copy(fontWeight = FontWeight.Bold)
            if (run.isItalic) style = style.copy(fontStyle = FontStyle.Italic)
            if (!run.linkUrl.isNullOrBlank()) {
                style = style.copy(
                    color = Color(0xFF1E88E5), // Material 3 Link Blue
                    textDecoration = TextDecoration.Underline
                )
            }

            val start = length
            withStyle(style) {
                append(run.text)
            }
            val end = length
            if (!run.linkUrl.isNullOrBlank()) {
                addStringAnnotation(tag = "URL", annotation = run.linkUrl, start = start, end = end)
            }
        }
    }

    /**
     * Converts updated plain text into a serialized document string for persistence.
     * Preserves structured formatting if existing document had blocks, or creates plain paragraphs.
     */
    fun toSerializedContent(text: String, existingSection: PlanNoteSection? = null): String {
        val existingDoc = loadDocument(existingSection)
        val doc = parseStructuredText(text, existingDoc)
        return PlanDocumentSerializer.serialize(doc)
    }

    /**
     * Handles automatic list and checklist continuation when the user presses Enter.
     */
    fun handleListContinuation(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        val oldText = oldValue.text
        val newText = newValue.text

        if (newText.length == oldText.length + 1 && newText.endsWith("\n") && !oldText.endsWith("\n")) {
            val lines = oldText.split("\n")
            val lastLine = lines.lastOrNull() ?: ""

            when {
                // Empty list item -> exit list
                lastLine == "• " || lastLine == "- " || lastLine == "☐ " || lastLine == "[ ] " -> {
                    val updatedLines = lines.dropLast(1)
                    val cleanText = updatedLines.joinToString("\n") + "\n"
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                // Empty numbered item -> exit list
                isNumberedLine(lastLine) && lastLine.substringAfter(". ").isBlank() -> {
                    val updatedLines = lines.dropLast(1)
                    val cleanText = updatedLines.joinToString("\n") + "\n"
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                // Bullet list continuation
                lastLine.startsWith("• ") -> {
                    val cleanText = newText + "• "
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                lastLine.startsWith("- ") -> {
                    val cleanText = newText + "- "
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                // Checklist continuation
                lastLine.startsWith("☐ ") -> {
                    val cleanText = newText + "☐ "
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                lastLine.startsWith("[ ] ") -> {
                    val cleanText = newText + "[ ] "
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
                // Numbered list continuation
                isNumberedLine(lastLine) -> {
                    val currentNum = lastLine.substringBefore(".").toIntOrNull() ?: 1
                    val nextNum = currentNum + 1
                    val cleanText = newText + "$nextNum. "
                    return TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                }
            }
        }

        return newValue
    }

    /**
     * Determines active formatting state at current cursor/selection position for toolbar highlight.
     */
    fun getActiveFormattingState(text: String, selection: TextRange): FormattingState {
        if (text.isEmpty()) return FormattingState()

        val lines = text.split("\n")
        val lineIdx = getLineIndexAtSelection(text, selection)
        val line = lines.getOrNull(lineIdx)?.trim() ?: ""

        return FormattingState(
            isHeading = line.startsWith("# ") || line.startsWith("## "),
            isBullet = line.startsWith("• ") || line.startsWith("- "),
            isNumbered = isNumberedLine(line),
            isChecklist = line.startsWith("☐ ") || line.startsWith("☑ ") || line.startsWith("[ ] ") || line.startsWith("[x] "),
            isBold = false,
            isItalic = false
        )
    }

    private fun getLineIndexAtSelection(text: String, selection: TextRange): Int {
        val selStart = selection.start.coerceIn(0, text.length)
        var lineCount = 0
        for (i in 0 until selStart) {
            if (text[i] == '\n') lineCount++
        }
        return lineCount
    }

    /**
     * Parses multiline text into structured blocks (Paragraphs, Headings, Bullet items, Numbered items, Checklist items)
     * and extracts inline formatting runs (bold, italic, links) into structural [TextRun] objects.
     */
    fun parseStructuredText(text: String, existingDoc: PlanDocument? = null): PlanDocument {
        if (text.isEmpty()) return PlanDocument(emptyList())

        val lines = text.split("\n")
        val blocks = lines.mapIndexed { index, line ->
            val trimmed = line.trim()
            val existingBlock = existingDoc?.blocks?.getOrNull(index)

            val rawLine = when {
                trimmed.startsWith("# ") -> trimmed.substring(2).trim()
                trimmed.startsWith("## ") -> trimmed.substring(3).trim()
                trimmed.startsWith("• ") -> trimmed.substring(2).trim()
                trimmed.startsWith("- ") -> trimmed.substring(2).trim()
                trimmed.startsWith("[x] ") -> trimmed.substring(4).trim()
                trimmed.startsWith("[ ] ") -> trimmed.substring(4).trim()
                trimmed.startsWith("☑ ") -> trimmed.substring(2).trim()
                trimmed.startsWith("☐ ") -> trimmed.substring(2).trim()
                isNumberedLine(trimmed) -> {
                    val dotIdx = trimmed.indexOf('.')
                    trimmed.substring(dotIdx + 1).trim()
                }
                else -> line
            }

            val existingRuns = when (existingBlock) {
                is DocumentBlock.Paragraph -> existingBlock.runs
                is DocumentBlock.Heading -> existingBlock.runs
                is DocumentBlock.BulletListItem -> existingBlock.runs
                is DocumentBlock.NumberedListItem -> existingBlock.runs
                is DocumentBlock.ChecklistItem -> existingBlock.runs
                else -> emptyList()
            }

            val oldText = existingBlock?.toPlainText()?.removePrefix("• ")?.removePrefix("- ")
                ?.removePrefix("[ ] ")?.removePrefix("[x] ")?.removePrefix("☐ ")?.removePrefix("☑ ") ?: ""

            val runs = if (existingRuns.isNotEmpty() && oldText.isNotEmpty()) {
                PlanDocumentEditor.syncRunsWithText(existingRuns, oldText, rawLine, rawLine.length)
            } else {
                parseInlineRuns(rawLine)
            }

            val cleanText = if (runs.isNotEmpty()) runs.joinToString("") { it.text } else rawLine

            when {
                trimmed.startsWith("# ") || trimmed.startsWith("## ") -> DocumentBlock.Paragraph(text = cleanText, runs = runs)
                trimmed.startsWith("• ") || trimmed.startsWith("- ") -> DocumentBlock.BulletListItem(text = cleanText, runs = runs)
                trimmed.startsWith("[x] ") || trimmed.startsWith("☑ ") -> DocumentBlock.ChecklistItem(text = cleanText, isChecked = true, runs = runs)
                trimmed.startsWith("[ ] ") || trimmed.startsWith("☐ ") -> DocumentBlock.ChecklistItem(text = cleanText, isChecked = false, runs = runs)
                isNumberedLine(trimmed) -> {
                    val dotIdx = trimmed.indexOf('.')
                    val num = trimmed.substring(0, dotIdx).toIntOrNull() ?: 1
                    DocumentBlock.NumberedListItem(number = num, text = cleanText, runs = runs)
                }
                else -> DocumentBlock.Paragraph(text = cleanText, runs = runs)
            }
        }
        return PlanDocument(blocks)
    }

    /**
     * Parses inline markdown/formatting markers ([text](url)) into structural [TextRun] list.
     */
    fun parseInlineRuns(text: String): List<TextRun> {
        if (text.isEmpty()) return emptyList()
        val runs = mutableListOf<TextRun>()

        // Link pattern: [text](url)
        val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")

        val currentText = text

        // Check link
        val linkMatch = linkRegex.find(currentText)
        if (linkMatch != null) {
            val linkLabel = linkMatch.groupValues[1]
            val linkUrl = linkMatch.groupValues[2]
            val before = currentText.substring(0, linkMatch.range.first)
            val after = currentText.substring(linkMatch.range.last + 1)

            if (before.isNotEmpty()) runs.addAll(parseInlineRuns(before))
            runs.add(TextRun(text = linkLabel, linkUrl = linkUrl))
            if (after.isNotEmpty()) runs.addAll(parseInlineRuns(after))
            return runs
        }

        // Plain text run
        return emptyList()
    }

    private fun isNumberedLine(line: String): Boolean {
        val dotIdx = line.indexOf('.')
        if (dotIdx <= 0 || dotIdx > 3) return false
        val numPart = line.substring(0, dotIdx)
        return numPart.all { it.isDigit() } && line.length > dotIdx + 1 && line[dotIdx + 1] == ' '
    }
}

/**
 * Data class representing formatting status at current cursor position.
 */
data class FormattingState(
    val isHeading: Boolean = false,
    val isBullet: Boolean = false,
    val isNumbered: Boolean = false,
    val isChecklist: Boolean = false,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)
