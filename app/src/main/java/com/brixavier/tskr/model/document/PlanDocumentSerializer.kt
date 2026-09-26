package com.brixavier.tskr.model.document

/**
 * Deterministic serializer and deserializer for [PlanDocument].
 *
 * Handles round-trip conversion between [PlanDocument] and lightweight JSON string persistence format.
 * Cross-platform & 100% host JVM unit test safe (decoupled from Android stub org.json.JSONObject).
 * Provides 100% backward compatibility for legacy plain-text note content.
 */
object PlanDocumentSerializer {

    private const val HEADER_PREFIX = "{\"v\":1,\"blocks\":["
    private const val HEADER_SUFFIX = "]}"

    /**
     * Serializes a [PlanDocument] into a deterministic JSON string for Room storage.
     */
    fun serialize(document: PlanDocument): String {
        if (document.blocks.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append(HEADER_PREFIX)

        document.blocks.forEachIndexed { index, block ->
            if (index > 0) sb.append(",")
            sb.append("{")
            when (block) {
                is DocumentBlock.Paragraph -> {
                    sb.append("\"type\":\"p\",\"text\":").append(escapeJson(block.text))
                    if (block.runs.isNotEmpty()) {
                        sb.append(",\"runs\":").append(serializeRuns(block.runs))
                    }
                }
                is DocumentBlock.Heading -> {
                    sb.append("\"type\":\"h\",\"text\":").append(escapeJson(block.text))
                    sb.append(",\"level\":").append(block.level)
                    if (block.runs.isNotEmpty()) {
                        sb.append(",\"runs\":").append(serializeRuns(block.runs))
                    }
                }
                is DocumentBlock.BulletListItem -> {
                    sb.append("\"type\":\"bullet\",\"text\":").append(escapeJson(block.text))
                    if (block.runs.isNotEmpty()) {
                        sb.append(",\"runs\":").append(serializeRuns(block.runs))
                    }
                }
                is DocumentBlock.NumberedListItem -> {
                    sb.append("\"type\":\"num\",\"text\":").append(escapeJson(block.text))
                    sb.append(",\"number\":").append(block.number)
                    if (block.runs.isNotEmpty()) {
                        sb.append(",\"runs\":").append(serializeRuns(block.runs))
                    }
                }
                is DocumentBlock.ChecklistItem -> {
                    sb.append("\"type\":\"check\",\"text\":").append(escapeJson(block.text))
                    sb.append(",\"checked\":").append(block.isChecked)
                    if (block.runs.isNotEmpty()) {
                        sb.append(",\"runs\":").append(serializeRuns(block.runs))
                    }
                }
            }
            sb.append("}")
        }

        sb.append(HEADER_SUFFIX)
        return sb.toString()
    }

    /**
     * Deserializes a raw content string into a [PlanDocument].
     * Gracefully falls back to plain text parsing for legacy content or non-JSON strings.
     */
    fun deserialize(rawContent: String?): PlanDocument {
        if (rawContent.isNullOrBlank()) return PlanDocument(emptyList())

        val trimmed = rawContent.trim()
        if (!trimmed.startsWith(HEADER_PREFIX) || !trimmed.endsWith(HEADER_SUFFIX)) {
            return PlanDocument.fromPlainText(rawContent)
        }

        try {
            val contentInner = trimmed.substring(HEADER_PREFIX.length, trimmed.length - HEADER_SUFFIX.length)
            if (contentInner.isBlank()) return PlanDocument(emptyList())

            val blockJsonStrings = parseBlockObjects(contentInner)
            val blocks = mutableListOf<DocumentBlock>()

            for (blockJson in blockJsonStrings) {
                val type = extractStringField(blockJson, "type") ?: "p"
                val text = extractStringField(blockJson, "text") ?: ""
                val runs = parseRunsField(blockJson)

                val block = when (type) {
                    "h" -> DocumentBlock.Heading(
                        text = text,
                        level = extractIntField(blockJson, "level") ?: 1,
                        runs = runs
                    )
                    "bullet" -> DocumentBlock.BulletListItem(
                        text = text,
                        runs = runs
                    )
                    "num" -> DocumentBlock.NumberedListItem(
                        number = extractIntField(blockJson, "number") ?: 1,
                        text = text,
                        runs = runs
                    )
                    "check" -> DocumentBlock.ChecklistItem(
                        text = text,
                        isChecked = extractBooleanField(blockJson, "checked") ?: false,
                        runs = runs
                    )
                    else -> DocumentBlock.Paragraph(
                        text = text,
                        runs = runs
                    )
                }
                blocks.add(block)
            }

            return PlanDocument(blocks)
        } catch (_: Exception) {
            return PlanDocument.fromPlainText(rawContent)
        }
    }

    private fun serializeRuns(runs: List<TextRun>): String {
        val sb = StringBuilder("[")
        runs.forEachIndexed { i, run ->
            if (i > 0) sb.append(",")
            sb.append("{\"text\":").append(escapeJson(run.text))
            if (run.isBold) sb.append(",\"b\":true")
            if (run.isItalic) sb.append(",\"i\":true")
            if (!run.linkUrl.isNullOrBlank()) sb.append(",\"url\":").append(escapeJson(run.linkUrl))
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseRunsField(json: String): List<TextRun> {
        val idx = json.indexOf("\"runs\":[")
        if (idx == -1) return emptyList()
        val start = idx + "\"runs\":[".length
        val end = json.indexOf("]", start)
        if (end == -1) return emptyList()

        val runsJson = json.substring(start, end)
        if (runsJson.isBlank()) return emptyList()

        val runObjects = parseBlockObjects(runsJson)
        return runObjects.map { runJson ->
            TextRun(
                text = extractStringField(runJson, "text") ?: "",
                isBold = extractBooleanField(runJson, "b") ?: false,
                isItalic = extractBooleanField(runJson, "i") ?: false,
                linkUrl = extractStringField(runJson, "url")
            )
        }
    }

    private fun parseBlockObjects(json: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var isEscaped = false

        for (i in json.indices) {
            val c = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && start != -1) {
                        result.add(json.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return result
    }

    private fun escapeJson(str: String): String {
        val sb = StringBuilder("\"")
        for (c in str) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun extractStringField(json: String, key: String): String? {
        val pattern = "\"$key\":\""
        val idx = json.indexOf(pattern)
        if (idx == -1) return null
        val start = idx + pattern.length
        val sb = StringBuilder()
        var isEscaped = false
        for (i in start until json.length) {
            val c = json[i]
            if (isEscaped) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(c)
                }
                isEscaped = false
            } else if (c == '\\') {
                isEscaped = true
            } else if (c == '"') {
                return sb.toString()
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun extractIntField(json: String, key: String): Int? {
        val pattern = "\"$key\":"
        val idx = json.indexOf(pattern)
        if (idx == -1) return null
        val start = idx + pattern.length
        val sb = StringBuilder()
        for (i in start until json.length) {
            val c = json[i]
            if (c.isDigit()) sb.append(c) else break
        }
        return sb.toString().toIntOrNull()
    }

    private fun extractBooleanField(json: String, key: String): Boolean? {
        val pattern = "\"$key\":"
        val idx = json.indexOf(pattern)
        if (idx == -1) return null
        val start = idx + pattern.length
        if (json.startsWith("true", start)) return true
        if (json.startsWith("false", start)) return false
        return null
    }
}
