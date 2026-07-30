package com.brixavier.tskr.engine

object PersonalityEngine {
    private val quotes = listOf(
        "Your future self keeps calling...",
        "This task won't finish itself. I checked.",
        "You're only one tap away from pretending to be productive.",
        "The hardest part is starting. Literally.",
        "Small progress still counts.",
        "You've spent longer deciding than doing.",
        "Your couch believes in you... but I believe in finishing this task.",
        "Let's make Future You ridiculously grateful.",
        "You can doom-scroll later.",
        "Congratulations! You're procrastinating inside an anti-procrastination app.",
        "You're negotiating with yourself again, aren't you?",
        "Do it now. Your future self has enough problems."
    )

    fun getRandomQuote(): String {
        return quotes.random()
    }
}
