package com.brixavier.tskr.model.document

/**
 * Domain-level keyboard intelligence and block transformation engine.
 * Completely decoupled from Jetpack Compose UI types.
 */
object PlanDocumentEditor {

    enum class StyleToggle { BOLD, ITALIC, LINK }

    /**
     * Handles Enter key block creation, splitting, and list continuation/exit rules,
     * preserving structural TextRun formatting spans on head and tail blocks.
     */
    fun handleEnterKey(
        document: PlanDocument,
        blockIndex: Int,
        cursorPosition: Int
    ): Pair<PlanDocument, Int> {
        val blocks = document.blocks.toMutableList()
        if (blocks.isEmpty()) {
            return Pair(PlanDocument(listOf(DocumentBlock.Paragraph(""))), 0)
        }

        val targetIndex = blockIndex.coerceIn(0, blocks.lastIndex)
        val targetBlock = blocks[targetIndex]
        val text = extractRawText(targetBlock)
        val runs = extractRuns(targetBlock)

        val pos = cursorPosition.coerceIn(0, text.length)
        val (headRuns, tailRuns) = splitBlockRuns(runs, text, pos)
        val headText = if (headRuns.isNotEmpty()) headRuns.joinToString("") { it.text } else text.substring(0, pos)
        val tailText = if (tailRuns.isNotEmpty()) tailRuns.joinToString("") { it.text } else text.substring(pos)

        when (targetBlock) {
            is DocumentBlock.Paragraph -> {
                blocks[targetIndex] = targetBlock.copy(text = headText, runs = headRuns)
                blocks.add(targetIndex + 1, DocumentBlock.Paragraph(text = tailText, runs = tailRuns))
            }
            is DocumentBlock.Heading -> {
                blocks[targetIndex] = targetBlock.copy(text = headText, runs = headRuns)
                blocks.add(targetIndex + 1, DocumentBlock.Paragraph(text = tailText, runs = tailRuns))
            }
            is DocumentBlock.BulletListItem -> {
                if (text.isBlank()) {
                    // Exit bullet list
                    blocks[targetIndex] = DocumentBlock.Paragraph(text = "")
                } else {
                    blocks[targetIndex] = targetBlock.copy(text = headText, runs = headRuns)
                    blocks.add(targetIndex + 1, DocumentBlock.BulletListItem(text = tailText, runs = tailRuns))
                }
            }
            is DocumentBlock.NumberedListItem -> {
                if (text.isBlank()) {
                    // Exit numbered list
                    blocks[targetIndex] = DocumentBlock.Paragraph(text = "")
                } else {
                    blocks[targetIndex] = targetBlock.copy(text = headText, runs = headRuns)
                    blocks.add(targetIndex + 1, DocumentBlock.NumberedListItem(number = targetBlock.number + 1, text = tailText, runs = tailRuns))
                }
            }
            is DocumentBlock.ChecklistItem -> {
                if (text.isBlank()) {
                    // Exit checklist
                    blocks[targetIndex] = DocumentBlock.Paragraph(text = "")
                } else {
                    blocks[targetIndex] = targetBlock.copy(text = headText, runs = headRuns)
                    blocks.add(targetIndex + 1, DocumentBlock.ChecklistItem(text = tailText, isChecked = false, runs = tailRuns))
                }
            }
        }

        val normalizedDoc = PlanDocument(normalizeNumbering(blocks))
        val nextFocusIndex = if (text.isBlank() && targetBlock !is DocumentBlock.Paragraph && targetBlock !is DocumentBlock.Heading) {
            targetIndex
        } else {
            targetIndex + 1
        }
        return Pair(normalizedDoc, nextFocusIndex)
    }

    /**
     * Handles Backspace key block merging at cursor position 0,
     * preserving structural TextRun formatting spans from both blocks.
     */
    fun handleBackspaceKey(
        document: PlanDocument,
        blockIndex: Int
    ): Pair<PlanDocument, Int> {
        val blocks = document.blocks.toMutableList()
        if (blocks.size <= 1 || blockIndex <= 0) {
            // First block or single block: safe boundary, preserve block
            return Pair(document, 0)
        }

        val targetIndex = blockIndex.coerceIn(0, blocks.lastIndex)
        val prevIndex = targetIndex - 1

        val prevBlock = blocks[prevIndex]
        val currentBlock = blocks[targetIndex]

        val prevText = extractRawText(prevBlock)
        val currentText = extractRawText(currentBlock)

        val prevRuns = extractRuns(prevBlock)
        val currentRuns = extractRuns(currentBlock)

        val (mergedText, mergedRuns) = mergeBlockRuns(prevRuns, prevText, currentRuns, currentText)

        val updatedPrevBlock = when (prevBlock) {
            is DocumentBlock.Paragraph -> prevBlock.copy(text = mergedText, runs = mergedRuns)
            is DocumentBlock.Heading -> prevBlock.copy(text = mergedText, runs = mergedRuns)
            is DocumentBlock.BulletListItem -> prevBlock.copy(text = mergedText, runs = mergedRuns)
            is DocumentBlock.NumberedListItem -> prevBlock.copy(text = mergedText, runs = mergedRuns)
            is DocumentBlock.ChecklistItem -> prevBlock.copy(text = mergedText, runs = mergedRuns)
        }

        blocks[prevIndex] = updatedPrevBlock
        blocks.removeAt(targetIndex)

        val normalizedDoc = PlanDocument(normalizeNumbering(blocks))
        return Pair(normalizedDoc, prevIndex)
    }

    /**
     * Synchronizes TextRun list with updated plain text when typing/deleting in BasicTextField.
     * Preserves linkUrl attributes on surrounding text.
     */
    fun syncRunsWithText(
        existingRuns: List<TextRun>,
        oldText: String,
        newText: String,
        cursorPosition: Int
    ): List<TextRun> {
        if (newText.isEmpty()) return emptyList()
        if (existingRuns.isEmpty()) return listOf(TextRun(newText))

        val diff = newText.length - oldText.length
        if (diff == 0) return existingRuns

        if (newText.length > oldText.length) {
            // Text Insertion
            val pos = (cursorPosition - diff).coerceIn(0, oldText.length)
            val (head, tail) = splitBlockRuns(existingRuns, oldText, pos)
            val activeRunAtPos = findRunAtOffset(existingRuns, pos)
            val insertedRun = TextRun(
                text = newText.substring(pos, pos + diff),
                linkUrl = activeRunAtPos?.linkUrl
            )
            return normalizeRuns(head + listOf(insertedRun) + tail)
        } else {
            // Text Deletion
            val pos = cursorPosition.coerceIn(0, newText.length)
            val deleteCount = oldText.length - newText.length
            val (head, tail1) = splitBlockRuns(existingRuns, oldText, pos)
            val (_, tail2) = splitBlockRuns(tail1, oldText.substring(pos), deleteCount)
            return normalizeRuns(head + tail2)
        }
    }

    private fun findRunAtOffset(runs: List<TextRun>, offset: Int): TextRun? {
        var curr = 0
        for (run in runs) {
            val len = run.text.length
            if (offset in curr..(curr + len)) {
                return run
            }
            curr += len
        }
        return null
    }

    /**
     * Applies or toggles a formatting style (Bold, Italic, Link) strictly within character range rangeStart..rangeEnd.
     * When rangeStart == rangeEnd (selection collapsed), expands to word bounds at cursor position.
     */
    fun applyRunStyle(
        runs: List<TextRun>,
        fullText: String,
        rangeStart: Int,
        rangeEnd: Int,
        toggle: StyleToggle,
        linkUrl: String? = null
    ): List<TextRun> {
        val len = fullText.length
        if (len == 0) return emptyList()

        val start = rangeStart.coerceIn(0, len)
        val end = rangeEnd.coerceIn(0, len)

        var targetStart = minOf(start, end)
        var targetEnd = maxOf(start, end)

        // Expand to word bounds at cursor if range is collapsed
        if (targetStart == targetEnd) {
            val pos = targetStart
            var wStart = pos
            var wEnd = pos
            while (wStart > 0 && !fullText[wStart - 1].isWhitespace()) {
                wStart--
            }
            while (wEnd < len && !fullText[wEnd].isWhitespace()) {
                wEnd++
            }
            if (wStart < wEnd) {
                targetStart = wStart
                targetEnd = wEnd
            } else {
                targetStart = 0
                targetEnd = len
            }
        }

        val effectiveStart = targetStart.coerceIn(0, len)
        val effectiveEnd = targetEnd.coerceIn(0, len)

        val effectiveRuns = if (runs.isEmpty()) listOf(TextRun(fullText)) else runs

        val (headRuns, tailRuns1) = splitBlockRuns(effectiveRuns, fullText, effectiveStart)
        val targetLength = effectiveEnd - effectiveStart
        val midText = fullText.substring(effectiveStart, effectiveEnd)
        val (selectedRuns, endRuns) = splitBlockRuns(tailRuns1, midText, targetLength)

        val isCurrentlyActive = when (toggle) {
            StyleToggle.BOLD -> selectedRuns.isNotEmpty() && selectedRuns.all { it.isBold }
            StyleToggle.ITALIC -> selectedRuns.isNotEmpty() && selectedRuns.all { it.isItalic }
            StyleToggle.LINK -> selectedRuns.isNotEmpty() && selectedRuns.all { !it.linkUrl.isNullOrBlank() }
        }

        val updatedSelectedRuns = selectedRuns.map { run ->
            when (toggle) {
                StyleToggle.BOLD -> run.copy(isBold = !isCurrentlyActive)
                StyleToggle.ITALIC -> run.copy(isItalic = !isCurrentlyActive)
                StyleToggle.LINK -> run.copy(linkUrl = if (isCurrentlyActive && linkUrl == null) null else linkUrl)
            }
        }

        return normalizeRuns(headRuns + updatedSelectedRuns + endRuns)
    }

    /**
     * Splits a list of [TextRun] spans at character index [pos], preserving bold, italic, and links.
     */
    fun splitBlockRuns(
        runs: List<TextRun>,
        fullText: String,
        pos: Int
    ): Pair<List<TextRun>, List<TextRun>> {
        if (runs.isEmpty()) {
            val head = fullText.substring(0, pos)
            val tail = fullText.substring(pos)
            return Pair(
                if (head.isEmpty()) emptyList() else listOf(TextRun(head)),
                if (tail.isEmpty()) emptyList() else listOf(TextRun(tail))
            )
        }

        val headRuns = mutableListOf<TextRun>()
        val tailRuns = mutableListOf<TextRun>()
        var curr = 0

        runs.forEach { run ->
            val runLen = run.text.length
            val runEnd = curr + runLen

            when {
                pos <= curr -> {
                    // Entire run is in tail
                    tailRuns.add(run)
                }
                pos >= runEnd -> {
                    // Entire run is in head
                    headRuns.add(run)
                }
                else -> {
                    // Split point is inside this run
                    val splitIdx = pos - curr
                    val leftText = run.text.substring(0, splitIdx)
                    val rightText = run.text.substring(splitIdx)

                    if (leftText.isNotEmpty()) headRuns.add(run.copy(text = leftText))
                    if (rightText.isNotEmpty()) tailRuns.add(run.copy(text = rightText))
                }
            }
            curr += runLen
        }

        return Pair(normalizeRuns(headRuns), normalizeRuns(tailRuns))
    }

    /**
     * Merges two sets of [TextRun] spans, maintaining structural formatting and combining adjacent identical runs.
     */
    fun mergeBlockRuns(
        prevRuns: List<TextRun>,
        prevText: String,
        currentRuns: List<TextRun>,
        currentText: String
    ): Pair<String, List<TextRun>> {
        val effectivePrev = if (prevRuns.isEmpty() && prevText.isNotEmpty()) listOf(TextRun(prevText)) else prevRuns
        val effectiveCurrent = if (currentRuns.isEmpty() && currentText.isNotEmpty()) listOf(TextRun(currentText)) else currentRuns

        val combinedRuns = normalizeRuns(effectivePrev + effectiveCurrent)
        val combinedText = if (combinedRuns.isNotEmpty()) combinedRuns.joinToString("") { it.text } else prevText + currentText

        return Pair(combinedText, combinedRuns)
    }

    /**
     * Normalizes adjacent [TextRun] spans with identical formatting attributes.
     */
    fun normalizeRuns(runs: List<TextRun>): List<TextRun> {
        if (runs.isEmpty()) return emptyList()
        val result = mutableListOf<TextRun>()

        runs.forEach { run ->
            if (run.text.isEmpty()) return@forEach
            val last = result.lastOrNull()
            if (last != null &&
                last.isBold == run.isBold &&
                last.isItalic == run.isItalic &&
                last.linkUrl == run.linkUrl
            ) {
                result[result.lastIndex] = last.copy(text = last.text + run.text)
            } else {
                result.add(run)
            }
        }
        return result
    }

    /**
     * Recalculates numbered list items sequentially across consecutive NumberedListItems.
     */
    fun normalizeNumbering(blocks: List<DocumentBlock>): List<DocumentBlock> {
        var currentNumber = 1
        return blocks.map { block ->
            if (block is DocumentBlock.NumberedListItem) {
                val updated = block.copy(number = currentNumber)
                currentNumber++
                updated
            } else {
                currentNumber = 1
                block
            }
        }
    }

    private fun extractRawText(block: DocumentBlock): String {
        return when (block) {
            is DocumentBlock.Paragraph -> block.text
            is DocumentBlock.Heading -> block.text
            is DocumentBlock.BulletListItem -> block.text
            is DocumentBlock.NumberedListItem -> block.text
            is DocumentBlock.ChecklistItem -> block.text
        }
    }

    private fun extractRuns(block: DocumentBlock): List<TextRun> {
        return when (block) {
            is DocumentBlock.Paragraph -> block.runs
            is DocumentBlock.Heading -> block.runs
            is DocumentBlock.BulletListItem -> block.runs
            is DocumentBlock.NumberedListItem -> block.runs
            is DocumentBlock.ChecklistItem -> block.runs
        }
    }
}
