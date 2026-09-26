package com.brixavier.tskr.ui

import com.brixavier.tskr.ui.theme.TskRMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class ParabolicMotionTest {

    @Test
    fun testParabolicOffset_atStartAndEnd() {
        val startX = 0f
        val endX = 100f
        val startY = 0f
        val endY = 0f
        val arcHeight = 12f

        val (x0, y0) = TskRMotion.calculateParabolicOffset(0f, startX, endX, startY, endY, arcHeight)
        assertEquals(0f, x0, 0.001f)
        assertEquals(0f, y0, 0.001f)

        val (x1, y1) = TskRMotion.calculateParabolicOffset(1f, startX, endX, startY, endY, arcHeight)
        assertEquals(100f, x1, 0.001f)
        assertEquals(0f, y1, 0.001f)
    }

    @Test
    fun testParabolicOffset_atPeakMidpoint() {
        val startX = 0f
        val endX = 100f
        val startY = 0f
        val endY = 0f
        val arcHeight = 12f

        // At t = 0.5, y = startY + 0.5*(endY - startY) - (4 * arcHeight * 0.5 * 0.5)
        // = 0 + 0 - (4 * 12 * 0.25) = -12f
        val (xMid, yMid) = TskRMotion.calculateParabolicOffset(0.5f, startX, endX, startY, endY, arcHeight)
        assertEquals(50f, xMid, 0.001f)
        assertEquals(-12f, yMid, 0.001f)
    }
}
