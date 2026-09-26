package com.brixavier.tskr.model.document

/**
 * UI-independent document model representing the structured contents of a Plan Note Section.
 * Completely decoupled from Jetpack Compose and Android View types.
 */
data class PlanDocument(
    val blocks: List<DocumentBlock> = emptyList()
) {
    /**
     * Helper converting the entire document into plain text string representation.
     */
    fun toPlainText(): String {
        return blocks.joinToString("\n") { it.toPlainText() }
    }

    companion object {
        fun fromPlainText(text: String): PlanDocument {
            if (text.isEmpty()) return PlanDocument(emptyList())
            val lines = text.split("\n")
            val blocks = lines.map { line ->
                DocumentBlock.Paragraph(text = line)
            }
            return PlanDocument(blocks)
        }
    }
}

/**
 * Sealed class representing structural block elements in a PlanDocument.
 */
sealed class DocumentBlock {
    abstract fun toPlainText(): String

    data class Paragraph(
        val text: String,
        val runs: List<TextRun> = emptyList()
    ) : DocumentBlock() {
        override fun toPlainText(): String = text
    }

    data class Heading(
        val text: String,
        val level: Int = 1,
        val runs: List<TextRun> = emptyList()
    ) : DocumentBlock() {
        override fun toPlainText(): String = text
    }

    data class BulletListItem(
        val text: String,
        val runs: List<TextRun> = emptyList()
    ) : DocumentBlock() {
        override fun toPlainText(): String = "• $text"
    }

    data class NumberedListItem(
        val number: Int,
        val text: String,
        val runs: List<TextRun> = emptyList()
    ) : DocumentBlock() {
        override fun toPlainText(): String = "$number. $text"
    }

    data class ChecklistItem(
        val text: String,
        val isChecked: Boolean = false,
        val runs: List<TextRun> = emptyList()
    ) : DocumentBlock() {
        override fun toPlainText(): String = if (isChecked) "[x] $text" else "[ ] $text"
    }
}

/**
 * Data class representing a text span with formatting attributes.
 */
data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val linkUrl: String? = null
)
