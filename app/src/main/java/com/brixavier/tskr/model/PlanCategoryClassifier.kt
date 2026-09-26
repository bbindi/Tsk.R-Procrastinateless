package com.brixavier.tskr.model

import java.util.Locale

/**
 * Pure, deterministic, offline category classification engine.
 *
 * Contains ZERO Android Context, Room, or external AI dependencies.
 * Uses weighted phrase & keyword scoring to suggest a [PlanCategory] from a Plan name.
 */
object PlanCategoryClassifier {

    private const val WEIGHT_PHRASE = 3.0f
    private const val WEIGHT_KEYWORD = 1.0f

    // Generic planning verbs with ZERO weight so they never dominate category-specific nouns/phrases
    private val GENERIC_VERBS = setOf(
        "plan", "planning", "prepare", "preparation", "organize", "organizing",
        "schedule", "scheduling", "arrange", "arranging", "make", "create",
        "do", "stuff", "thing", "things"
    )

    // Category Phrases & Keywords Dictionary
    private val PHRASES: Map<PlanCategory, List<String>> = mapOf(
        PlanCategory.TRAVEL to listOf("family trip", "family vacation", "road trip", "flight booking", "hotel reservation"),
        PlanCategory.FINANCE to listOf("save money", "monthly budget", "tax return", "bank account", "pay off"),
        PlanCategory.HOME to listOf("house cleaning", "home repair", "home improvement", "lawn care", "office chair"),
        PlanCategory.FAMILY to listOf("family meeting", "family reunion", "family gathering", "kids activity"),
        PlanCategory.HEALTH to listOf("doctor appointment", "gym workout", "health checkup", "exercise regularly"),
        PlanCategory.EVENTS to listOf("wedding invitation", "birthday party", "anniversary celebration"),
        PlanCategory.SPIRITUAL to listOf("bible study", "sunday meeting", "scripture reading", "field ministry"),
        PlanCategory.SHOPPING to listOf("grocery shopping", "shopping list", "buy new"),
        PlanCategory.GOALS to listOf("2026 goals", "2027 goals", "new year resolution", "life goal"),
        PlanCategory.PROJECTS to listOf("build app", "app feature", "software project", "code release")
    )

    private val KEYWORDS: Map<PlanCategory, List<String>> = mapOf(
        PlanCategory.TRAVEL to listOf("travel", "trip", "vacation", "flight", "hotel", "passport", "luggage", "journey", "holiday", "ghana", "tour", "resort", "airline"),
        PlanCategory.FINANCE to listOf("money", "budget", "savings", "save", "expense", "expenses", "debt", "bank", "payment", "financial", "cost", "invest", "bill", "bills"),
        PlanCategory.HOME to listOf("house", "home", "room", "kitchen", "cleaning", "repair", "renovation", "furniture", "garden", "appliance", "paint", "roof", "yard", "garage"),
        PlanCategory.FAMILY to listOf("family", "children", "child", "kids", "parents", "mother", "father", "mom", "dad", "baby", "reunion", "daughter", "son"),
        PlanCategory.HEALTH to listOf("health", "exercise", "workout", "fitness", "doctor", "appointment", "medicine", "wellness", "diet", "gym", "dentist", "running"),
        PlanCategory.EVENTS to listOf("event", "events", "wedding", "birthday", "party", "ceremony", "program", "celebration", "invitation", "anniversary"),
        PlanCategory.PROJECTS to listOf("project", "projects", "launch", "prototype", "system", "code", "dev", "app", "feature", "build", "redesign"),
        PlanCategory.SPIRITUAL to listOf("bible", "scripture", "prayer", "ministry", "congregation", "meeting", "spiritual", "worship", "assembly", "convention", "faith"),
        PlanCategory.CREATIVE to listOf("design", "writing", "write", "music", "song", "video", "photo", "photography", "drawing", "painting", "art", "book", "blog"),
        PlanCategory.SHOPPING to listOf("buy", "purchase", "shopping", "shop", "order", "groceries", "store", "product", "item"),
        PlanCategory.GOALS to listOf("goal", "goals", "target", "milestone", "resolution", "achievement", "ambition", "dream"),
        PlanCategory.WORK to listOf("work", "office", "job", "colleague", "workplace", "assignment", "professional", "client", "report", "presentation", "career", "exam"),
        PlanCategory.PERSONAL to listOf("personal", "myself", "self", "routine", "habit", "diary", "journal")
    )

    // Deterministic tie-breaking priority
    private val TIE_BREAK_PRIORITY = listOf(
        PlanCategory.TRAVEL,
        PlanCategory.FINANCE,
        PlanCategory.EVENTS,
        PlanCategory.SPIRITUAL,
        PlanCategory.FAMILY,
        PlanCategory.HOME,
        PlanCategory.HEALTH,
        PlanCategory.PROJECTS,
        PlanCategory.CREATIVE,
        PlanCategory.SHOPPING,
        PlanCategory.WORK,
        PlanCategory.GOALS,
        PlanCategory.PLANNING,
        PlanCategory.PERSONAL
    )

    /**
     * Classifies a Plan name into a suggested [PlanCategory].
     * Returns [PlanCategory.PERSONAL] for low confidence or blank input.
     */
    fun classify(planName: String): PlanCategory {
        val normalized = normalize(planName)
        if (normalized.isBlank()) return PlanCategory.PERSONAL

        val scores = mutableMapOf<PlanCategory, Float>()
        PlanCategory.entries.forEach { scores[it] = 0.0f }

        // 1. Evaluate Multi-Word Exact Phrase Matching (Weight: 3.0)
        PHRASES.forEach { (category, phrases) ->
            for (phrase in phrases) {
                if (normalized.contains(phrase)) {
                    scores[category] = (scores[category] ?: 0f) + WEIGHT_PHRASE
                }
            }
        }

        // 2. Evaluate Token Keyword Matching (Weight: 1.0)
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        for (token in tokens) {
            if (GENERIC_VERBS.contains(token)) continue // Zero weight for generic verbs

            KEYWORDS.forEach { (category, keywords) ->
                if (keywords.contains(token)) {
                    scores[category] = (scores[category] ?: 0f) + WEIGHT_KEYWORD
                }
            }
        }

        // 3. Find max score
        val maxScore = scores.values.maxOrNull() ?: 0f
        if (maxScore < 1.0f) {
            // Low confidence fallback -> PERSONAL
            return PlanCategory.PERSONAL
        }

        // 4. Resolve tie using deterministic priority order
        val candidates = scores.filter { it.value == maxScore }.keys
        if (candidates.size == 1) {
            return candidates.first()
        }

        return TIE_BREAK_PRIORITY.firstOrNull { candidates.contains(it) } ?: PlanCategory.PERSONAL
    }

    /**
     * Normalizes a Plan name string:
     * - Lowercase
     * - Replaces non-alphanumeric symbols with spaces
     * - Collapses multiple spaces
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        val lower = text.lowercase(Locale.US)
        val cleaned = lower.replace(Regex("[^a-z0-9\\s]"), " ")
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }
}
