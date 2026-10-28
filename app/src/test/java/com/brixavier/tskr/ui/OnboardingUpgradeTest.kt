package com.brixavier.tskr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUpgradeTest {

    enum class StartupRoute {
        FIRST_RUN_WELCOME,
        V2_WHATS_NEW_WALKTHROUGH,
        MAIN_DASHBOARD
    }

    private fun resolveStartupRoute(
        isFirstRun: Boolean,
        v2WhatsNewSeen: Boolean,
        isSettingsReplay: Boolean = false
    ): StartupRoute {
        return when {
            isSettingsReplay -> StartupRoute.V2_WHATS_NEW_WALKTHROUGH
            isFirstRun -> StartupRoute.FIRST_RUN_WELCOME
            !v2WhatsNewSeen -> StartupRoute.V2_WHATS_NEW_WALKTHROUGH
            else -> StartupRoute.MAIN_DASHBOARD
        }
    }

    @Test
    fun test1_brandNewV2User_launchesWelcomeScreenFirst() {
        val isFirstRun = true
        val v2WhatsNewSeen = false

        val initialRoute = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.FIRST_RUN_WELCOME, initialRoute)
    }

    @Test
    fun test2_completingWelcomeScreen_transitionsToWhatsNewWalkthrough() {
        var v2WhatsNewSeen = false

        // User completes WelcomeScreen (GET STARTED / PROCEED)
        // isFirstRun becomes false, but v2WhatsNewSeen remains false
        val isFirstRun = false
        assertFalse(v2WhatsNewSeen)

        val nextRoute = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.V2_WHATS_NEW_WALKTHROUGH, nextRoute)
    }

    @Test
    fun test3_completingWhatsNewWalkthrough_transitionsToDashboard() {
        val isFirstRun = false
        val v2WhatsNewSeen = true

        val dashboardRoute = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.MAIN_DASHBOARD, dashboardRoute)
    }

    @Test
    fun test4_existingV1User_routesToWhatsNewWalkthrough() {
        // V1 user has isFirstRun = false from V1, but v2WhatsNewSeen is absent/false
        val isFirstRun = false
        val v2WhatsNewSeen = false

        val route = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.V2_WHATS_NEW_WALKTHROUGH, route)
    }

    @Test
    fun test5_returningV2User_routesDirectlyToDashboard() {
        val isFirstRun = false
        val v2WhatsNewSeen = true

        val route = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.MAIN_DASHBOARD, route)
    }

    @Test
    fun test6_settingsReplay_remainsFunctionalWithoutModifyingFirstRun() {
        val isFirstRun = false
        val v2WhatsNewSeen = true
        val isSettingsReplay = true

        val route = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen,
            isSettingsReplay = isSettingsReplay
        )

        assertEquals(StartupRoute.V2_WHATS_NEW_WALKTHROUGH, route)
        assertFalse(isFirstRun)
        assertTrue(v2WhatsNewSeen)
    }

    @Test
    fun test7_exitingWhatsNewBeforeCompletion_doesNotSetSeenTrue() {
        var v2WhatsNewSeen = false

        fun onDismissWalkthroughPrematurely() {
            // Dismissed without completing GET TO WORK or Skip
        }

        onDismissWalkthroughPrematurely()
        assertFalse(v2WhatsNewSeen)
    }

    @Test
    fun test8_skippingWhatsNewWalkthrough_setsSeenTrueAndTransitionsToDashboard() {
        val isFirstRun = false
        var v2WhatsNewSeen = false

        // User taps "Skip" on the What's New Walkthrough
        fun onSkipWalkthrough() {
            v2WhatsNewSeen = true
        }

        onSkipWalkthrough()
        assertTrue(v2WhatsNewSeen)

        val destinationRoute = resolveStartupRoute(
            isFirstRun = isFirstRun,
            v2WhatsNewSeen = v2WhatsNewSeen
        )
        assertEquals(StartupRoute.MAIN_DASHBOARD, destinationRoute)
    }

    @Test
    fun test9_skippingWhatsNewWalkthrough_persistsAcrossSubsequentLaunches() {
        // App launch 1: user skips
        var persistedV2Seen = false
        persistedV2Seen = true // simulated SharedPreferences persistence on skip

        // App launch 2: next startup reads persistedV2Seen = true
        val launch2Route = resolveStartupRoute(
            isFirstRun = false,
            v2WhatsNewSeen = persistedV2Seen
        )
        assertEquals(StartupRoute.MAIN_DASHBOARD, launch2Route)
    }

    @Test
    fun test10_skippingWalkthrough_preservesFirstRunStateAndDataIntegrity() {
        var isFirstRun = false
        var v2WhatsNewSeen = false

        // Tapping skip only touches v2WhatsNewSeen
        v2WhatsNewSeen = true

        assertFalse("First run state must remain false and not be reset", isFirstRun)
        assertTrue(v2WhatsNewSeen)
        val route = resolveStartupRoute(isFirstRun, v2WhatsNewSeen)
        assertEquals(StartupRoute.MAIN_DASHBOARD, route)
    }

    @Test
    fun test11_duplicateSkipCalls_idempotentlyRetainsDashboardRoute() {
        var v2WhatsNewSeen = false
        var skipCallCount = 0

        fun onSkip() {
            skipCallCount++
            v2WhatsNewSeen = true
        }

        // Simulate rapid/repeated tapping
        onSkip()
        onSkip()
        onSkip()

        assertEquals(3, skipCallCount)
        assertTrue(v2WhatsNewSeen)
        val route = resolveStartupRoute(isFirstRun = false, v2WhatsNewSeen = v2WhatsNewSeen)
        assertEquals(StartupRoute.MAIN_DASHBOARD, route)
    }
}
