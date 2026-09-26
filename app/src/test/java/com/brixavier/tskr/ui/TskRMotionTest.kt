package com.brixavier.tskr.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.brixavier.tskr.ui.theme.TskRMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TskRMotionTest {

    @Test
    fun testMotionSystem_durationTokensAndEasingCurvesAreCorrect() {
        assertEquals(150, TskRMotion.DURATION_MICRO)
        assertEquals(220, TskRMotion.DURATION_STANDARD)
        assertEquals(350, TskRMotion.DURATION_PERSONALITY)

        assertEquals(12.dp, TskRMotion.TranslationOffset)
        assertEquals(FastOutSlowInEasing, TskRMotion.EasingStandard)
        assertEquals(LinearOutSlowInEasing, TskRMotion.EasingEmphasized)
    }

    @Test
    fun testMotionSystem_transitionsConstructNonNullSpecs() {
        val density = Density(1.0f)

        val enter = TskRMotion.enterTransition(density)
        val exit = TskRMotion.exitTransition(density)
        val popEnter = TskRMotion.popEnterTransition(density)
        val popExit = TskRMotion.popExitTransition(density)
        val tabTransform = TskRMotion.tabTransition()

        assertNotNull(enter)
        assertNotNull(exit)
        assertNotNull(popEnter)
        assertNotNull(popExit)
        assertNotNull(tabTransform)
    }
}
