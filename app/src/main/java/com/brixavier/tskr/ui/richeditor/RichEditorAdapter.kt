package com.brixavier.tskr.ui.richeditor

import com.brixavier.tskr.model.document.DocumentBlock
import com.brixavier.tskr.model.document.PlanDocument
import com.brixavier.tskr.model.document.TextRun
import com.mohamedrejeb.richeditor.model.RichTextState
import java.lang.StringBuilder

object RichEditorAdapter {
    /**
     * Converts a [PlanDocument] into a simple HTML string that [RichTextState] can parse.
     */
    fun documentToHtml(document: PlanDocument): String {
        val sb = StringBuilder()
        var currentListType: String? = null

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    if (currentListType != null) {
                        sb.append("</$currentListType>")
                        currentListType = null
                    }
                    sb.append("<p>").append(runsToHtml(block.runs, block.text)).append("</p>")
                }
                is DocumentBlock.BulletListItem -> {
                    if (currentListType != "ul") {
                        if (currentListType != null) sb.append("</$currentListType>")
                        sb.append("<ul>")
                        currentListType = "ul"
                    }
                    sb.append("<li>").append(runsToHtml(block.runs, block.text)).append("</li>")
                }
                is DocumentBlock.NumberedListItem -> {
                    if (currentListType != "ol") {
                        if (currentListType != null) sb.append("</$currentListType>")
                        sb.append("<ol>")
                        currentListType = "ol"
                    }
                    sb.append("<li>").append(runsToHtml(block.runs, block.text)).append("</li>")
                }
                // Checklists and Headings are mapped to plain paragraphs for this prototype
                is DocumentBlock.Heading -> {
                    if (currentListType != null) {
                        sb.append("</$currentListType>")
                        currentListType = null
                    }
                    val headingTag = "h${block.level.coerceIn(1, 6)}"
                    sb.append("<$headingTag>").append(runsToHtml(block.runs, block.text)).append("</$headingTag>")
                }
                is DocumentBlock.ChecklistItem -> {
                    if (currentListType != null) {
                        sb.append("</$currentListType>")
                        currentListType = null
                    }
                    // For prototype, map checklist to paragraph
                    sb.append("<p>").append(block.toPlainText()).append("</p>")
                }
            }
        }
        if (currentListType != null) {
            sb.append("</$currentListType>")
        }
        return sb.toString()
    }

    private fun runsToHtml(runs: List<TextRun>, fallbackText: String): String {
        if (runs.isEmpty()) {
            return escapeHtml(fallbackText)
        }
        val sb = StringBuilder()
        for (run in runs) {
            var htmlText = escapeHtml(run.text)
            
            // Note: we might need to handle line breaks in htmlText
            htmlText = htmlText.replace("\n", "<br>")

            if (run.isBold) htmlText = "<b>$htmlText</b>"
            if (run.isItalic) htmlText = "<i>$htmlText</i>"
            if (!run.linkUrl.isNullOrBlank()) {
                val url = escapeHtml(run.linkUrl)
                htmlText = "<a href=\"$url\">$htmlText</a>"
            }
            sb.append(htmlText)
        }
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    // Helper for debugging/fallback
    fun htmlToDocument(html: String): PlanDocument {
        // Since we are parsing the output of RichTextState.toHtml() for the prototype, 
        // we can implement a basic HTML to PlanDocument parser. 
        // However, we can also use RichTextState.toMarkdown() as an intermediate if that's easier.
        // Actually, for this prototype, if we use RichTextState, we can get HTML and we need to map it back to PlanDocument.
        
        // Wait, RichTextState.toMarkdown() might be easier to parse since it generates simpler markdown text.
        // Let's implement a rudimentary HTML parser or Markdown parser here.
        // Or we can just store the HTML as a single paragraph with runs, wait no, we need to preserve blocks.
        // Since Tsk.R needs the serialized JSON, we must generate a PlanDocument.
        // Let's just create a basic regex-based HTML parser for the prototype.
        
        val blocks = mutableListOf<DocumentBlock>()
        
        // Very rudimentary parser: split by block tags
        val blockRegex = "<(p|h[1-6]|ul|ol|li)[^>]*>(.*?)</\\1>".toRegex(RegexOption.IGNORE_CASE)
        val listRegex = "<(ul|ol)[^>]*>(.*?)</\\1>".toRegex(RegexOption.IGNORE_CASE)
        val liRegex = "<li[^>]*>(.*?)</li[>]*>".toRegex(RegexOption.IGNORE_CASE)
        
        var currentMatch = listRegex.find(html) ?: blockRegex.find(html)
        var searchIndex = 0
        
        while (currentMatch != null && searchIndex < html.length) {
            val tag = currentMatch.groupValues[1].lowercase()
            val innerHtml = currentMatch.groupValues[2]
            
            if (tag == "ul" || tag == "ol") {
                val lis = liRegex.findAll(innerHtml)
                var num = 1
                for (li in lis) {
                    val liHtml = li.groupValues[1]
                    if (tag == "ul") {
                        blocks.add(DocumentBlock.BulletListItem(
                            text = stripTags(liHtml),
                            runs = parseRuns(liHtml)
                        ))
                    } else {
                        blocks.add(DocumentBlock.NumberedListItem(
                            number = num++,
                            text = stripTags(liHtml),
                            runs = parseRuns(liHtml)
                        ))
                    }
                }
            } else if (tag.startsWith("h")) {
                val level = tag.substring(1).toIntOrNull() ?: 1
                blocks.add(DocumentBlock.Heading(
                    level = level,
                    text = stripTags(innerHtml),
                    runs = parseRuns(innerHtml)
                ))
            } else if (tag == "p") {
                val pText = stripTags(innerHtml)
                val runs = parseRuns(innerHtml)
                
                if (pText.startsWith("[ ] ") || pText.startsWith("☐ ")) {
                    val textStr = pText.substring(4)
                    val newRuns = removePrefixFromRuns(runs, pText.substring(0, 4))
                    blocks.add(DocumentBlock.ChecklistItem(
                        text = textStr,
                        isChecked = false,
                        runs = newRuns
                    ))
                } else if (pText.startsWith("[x] ") || pText.startsWith("☑ ")) {
                    val textStr = pText.substring(4)
                    val newRuns = removePrefixFromRuns(runs, pText.substring(0, 4))
                    blocks.add(DocumentBlock.ChecklistItem(
                        text = textStr,
                        isChecked = true,
                        runs = newRuns
                    ))
                } else if (pText.startsWith("# ") || pText.startsWith("## ") || pText.startsWith("### ")) {
                    val hashCount = pText.takeWhile { it == '#' }.length
                    val prefixLen = hashCount + 1 // including space
                    val textStr = pText.substring(prefixLen)
                    val newRuns = removePrefixFromRuns(runs, pText.substring(0, prefixLen))
                    blocks.add(DocumentBlock.Heading(
                        level = hashCount.coerceIn(1, 6),
                        text = textStr,
                        runs = newRuns
                    ))
                } else if (pText.startsWith("```")) {
                    // Treat as paragraph but could be parsed as CodeBlock if domain model supported it
                    blocks.add(DocumentBlock.Paragraph(
                        text = pText,
                        runs = runs
                    ))
                } else {
                    blocks.add(DocumentBlock.Paragraph(
                        text = pText,
                        runs = runs
                    ))
                }
            }
            
            searchIndex = currentMatch.range.last + 1
            currentMatch = listRegex.find(html, searchIndex) ?: blockRegex.find(html, searchIndex)
        }
        
        if (blocks.isEmpty()) {
            return PlanDocument.fromPlainText(stripTags(html))
        }
        
        return PlanDocument(blocks)
    }

    private fun parseRuns(html: String): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        
        // This is a naive regex parser for runs. 
        // A better approach would be to process character by character.
        var i = 0
        var isBold = false
        var isItalic = false
        var linkUrl: String? = null
        val currentText = StringBuilder()
        
        fun addRun() {
            if (currentText.isNotEmpty()) {
                runs.add(TextRun(
                    text = unescapeHtml(currentText.toString()),
                    isBold = isBold,
                    isItalic = isItalic,
                    linkUrl = linkUrl
                ))
                currentText.clear()
            }
        }
        
        while (i < html.length) {
            if (html.startsWith("<b>", i) || html.startsWith("<strong>", i)) {
                addRun()
                isBold = true
                i += if (html.startsWith("<b>", i)) 3 else 8
            } else if (html.startsWith("</b>", i) || html.startsWith("</strong>", i)) {
                addRun()
                isBold = false
                i += if (html.startsWith("</b>", i)) 4 else 9
            } else if (html.startsWith("<i>", i) || html.startsWith("<em>", i)) {
                addRun()
                isItalic = true
                i += if (html.startsWith("<i>", i)) 3 else 4
            } else if (html.startsWith("</i>", i) || html.startsWith("</em>", i)) {
                addRun()
                isItalic = false
                i += if (html.startsWith("</i>", i)) 4 else 5
            } else if (html.startsWith("<a ", i)) {
                addRun()
                val endTag = html.indexOf(">", i)
                if (endTag != -1) {
                    val aTag = html.substring(i, endTag + 1)
                    val hrefRegex = "href=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
                    linkUrl = hrefRegex.find(aTag)?.groupValues?.get(1)
                    i = endTag + 1
                } else {
                    currentText.append(html[i])
                    i++
                }
            } else if (html.startsWith("</a>", i)) {
                addRun()
                linkUrl = null
                i += 4
            } else if (html.startsWith("<br>", i) || html.startsWith("<br/>", i)) {
                currentText.append("\n")
                i += if (html.startsWith("<br>", i)) 4 else 5
            } else {
                currentText.append(html[i])
                i++
            }
        }
        
        addRun()
        
        if (runs.isEmpty()) {
            runs.add(TextRun(text = unescapeHtml(html)))
        }
        return runs
    }
    
    private fun removePrefixFromRuns(runs: List<TextRun>, prefix: String): List<TextRun> {
        val result = mutableListOf<TextRun>()
        var charsToRemove = prefix.length
        
        for (run in runs) {
            if (charsToRemove > 0) {
                if (run.text.length <= charsToRemove) {
                    charsToRemove -= run.text.length
                } else {
                    result.add(run.copy(text = run.text.substring(charsToRemove)))
                    charsToRemove = 0
                }
            } else {
                result.add(run)
            }
        }
        return result
    }
    
    private fun stripTags(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").let { unescapeHtml(it) }
    }
    
    private fun unescapeHtml(text: String): String {
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
}
