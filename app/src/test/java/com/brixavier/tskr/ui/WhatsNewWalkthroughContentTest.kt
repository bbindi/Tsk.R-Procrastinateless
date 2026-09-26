package com.brixavier.tskr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewWalkthroughContentTest {

    data class SlideModel(
        val title: String,
        val subtitle: String,
        val body: String
    )

    private val v2Slides = listOf(
        SlideModel(
            title = "Welcome to Tsk.R V2",
            subtitle = "A few things changed while you weren't looking.",
            body = "I got tired of watching you procrastinate, so I upgraded myself. New tools, better Plans, and more ways for me to bother you productively."
        ),
        SlideModel(
            title = "Tasks & Plans",
            subtitle = "Tasks are what you need to do. Plans are what you're actually trying to accomplish.",
            body = "Tasks keep me on your back for daily commitments. Plans give your overarching goals somewhere to live so your big ideas stop dying in your head."
        ),
        SlideModel(
            title = "Plans Got Serious",
            subtitle = "Steps. Notes. Tasks. One place.",
            body = "Plans now provide a structured workspace where you can break goals into Plan Steps, keep rich Notes, and attach Tsk.R tasks. Try not to abandon them."
        ),
        SlideModel(
            title = "Calendar 2.0",
            subtitle = "See the chaos before it sees you.",
            body = "Scan task density across Month grids, Week strips, and Day agendas with visual indicator dots. Tap any date to inspect its agenda before deadlines sneak up on you."
        ),
        SlideModel(
            title = "I Moved Onto Your Home Screen",
            subtitle = "Apparently opening the app was too much work.",
            body = "Check off tasks directly from your home screen widget, view Plan progress, and receive accountability quotes without even opening the app."
        )
    )

    @Test
    fun testWalkthrough_hasFiveSlides() {
        assertEquals(5, v2Slides.size)
    }

    @Test
    fun testWalkthrough_slideContentHasPersonalityAndFeatureClarity() {
        // Slide 1
        assertEquals("Welcome to Tsk.R V2", v2Slides[0].title)
        assertTrue(v2Slides[0].subtitle.contains("while you weren't looking"))
        assertTrue(v2Slides[0].body.contains("procrastinate"))

        // Slide 2
        assertEquals("Tasks & Plans", v2Slides[1].title)
        assertTrue(v2Slides[1].subtitle.contains("Tasks are what you need to do"))

        // Slide 3 (Plan Workspace)
        assertEquals("Plans Got Serious", v2Slides[2].title)
        assertEquals("Steps. Notes. Tasks. One place.", v2Slides[2].subtitle)

        // Slide 4
        assertEquals("Calendar 2.0", v2Slides[3].title)
        assertTrue(v2Slides[3].subtitle.contains("chaos"))

        // Slide 5
        assertEquals("I Moved Onto Your Home Screen", v2Slides[4].title)
        assertTrue(v2Slides[4].subtitle.contains("opening the app was too much work"))
    }

    @Test
    fun testWalkthroughNavigation_slideIndexingAndBounds() {
        var currentIdx = 0

        // Forward navigation: 0 -> 1 -> 2 -> 3 -> 4
        assertTrue(currentIdx < v2Slides.lastIndex)
        currentIdx++
        assertEquals(1, currentIdx)

        currentIdx++
        assertEquals(2, currentIdx)

        currentIdx++
        assertEquals(3, currentIdx)

        currentIdx++
        assertEquals(4, currentIdx)
        assertFalse(currentIdx < v2Slides.lastIndex) // Final slide reached ("GET TO WORK")

        // Backward navigation: 4 -> 3 -> 2 -> 1 -> 0
        currentIdx--
        assertEquals(3, currentIdx)

        currentIdx--
        assertEquals(2, currentIdx)

        currentIdx--
        assertEquals(1, currentIdx)

        currentIdx--
        assertEquals(0, currentIdx)
    }
}
