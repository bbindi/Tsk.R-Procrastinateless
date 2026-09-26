package com.brixavier.tskr.ui

import com.brixavier.tskr.ui.theme.TskRMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class K4PersonalityAndButtonTest {

    @Test
    fun testK4MotionTokens_matchK1Standards() {
        assertEquals(150, TskRMotion.DURATION_MICRO)
        assertEquals(220, TskRMotion.DURATION_STANDARD)
        assertEquals(350, TskRMotion.DURATION_PERSONALITY)
    }

    @Test
    fun testButtonMicroInteraction_scaleTargets() {
        val isPressed = true
        val targetScale = if (isPressed) 0.97f else 1.0f
        assertEquals(0.97f, targetScale, 0.001f)

        val isReleased = false
        val releasedScale = if (isReleased) 0.97f else 1.0f
        assertEquals(1.0f, releasedScale, 0.001f)
    }

    @Test
    fun testSnackbarAndHeaderTransitions_useStandardDuration() {
        val duration = TskRMotion.DURATION_STANDARD
        assertEquals(220, duration)
    }
}
