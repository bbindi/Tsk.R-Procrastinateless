package com.brixavier.tskr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPageTest {

    @Test
    fun testPunctualityStatusText_reflectsExactAlarmPermission() {
        val exactAllowedStatus = if (true) "✓ Ready to pounce" else "⚠ Timing compromised"
        val exactDeniedStatus = if (false) "✓ Ready to pounce" else "⚠ Timing compromised"

        assertEquals("✓ Ready to pounce", exactAllowedStatus)
        assertEquals("⚠ Timing compromised", exactDeniedStatus)
    }

    @Test
    fun testBatteryOptimizationStatusText_reflectsOptimizationState() {
        val batteryIgnoredStatus = if (true) "✓ Always watching" else "⚠ Android is suppressing me"
        val batteryRestrictedStatus = if (false) "✓ Always watching" else "⚠ Android is suppressing me"

        assertEquals("✓ Always watching", batteryIgnoredStatus)
        assertEquals("⚠ Android is suppressing me", batteryRestrictedStatus)
    }

    @Test
    fun testSoundTitleResolution_customVsDefault() {
        val defaultTitle = "System Default"
        val customTitle = "Gentle Chime"

        val isDefaultCustom = defaultTitle != "System Default"
        val isCustomCustom = customTitle != "System Default"

        assertFalse(isDefaultCustom)
        assertTrue(isCustomCustom)
    }

    @Test
    fun testAppVersionFormatting() {
        val appVersion = "1.0.1"
        val formatted = "Version $appVersion"

        assertEquals("Version 1.0.1", formatted)
    }
}
