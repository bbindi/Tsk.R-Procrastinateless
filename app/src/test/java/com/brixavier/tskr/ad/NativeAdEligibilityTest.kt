package com.brixavier.tskr.ad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering Native Ad placement eligibility rules for Dashboard and Task Library.
 */
class NativeAdEligibilityTest {

    @Test
    fun test1_dashboardAd_shownOnlyWhenConsentAndSdkInitialized() {
        assertFalse(NativeAdEligibility.shouldShowDashboardAd(canRequestAds = false, isSdkInitialized = false))
        assertFalse(NativeAdEligibility.shouldShowDashboardAd(canRequestAds = true, isSdkInitialized = false))
        assertFalse(NativeAdEligibility.shouldShowDashboardAd(canRequestAds = false, isSdkInitialized = true))
        assertTrue(NativeAdEligibility.shouldShowDashboardAd(canRequestAds = true, isSdkInitialized = true))
    }

    @Test
    fun test2_taskLibraryAd_shownOnlyWhenTaskCountIsSixOrMore() {
        // Less than threshold (fewer than 6 tasks) -> false
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 0, canRequestAds = true, isSdkInitialized = true))
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 1, canRequestAds = true, isSdkInitialized = true))
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 4, canRequestAds = true, isSdkInitialized = true))
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 5, canRequestAds = true, isSdkInitialized = true))

        // Threshold met (6 or more tasks) -> true
        assertTrue(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 6, canRequestAds = true, isSdkInitialized = true))
        assertTrue(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 10, canRequestAds = true, isSdkInitialized = true))
    }

    @Test
    fun test3_taskLibraryAd_hiddenWhenConsentOrSdkNotReady() {
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 10, canRequestAds = false, isSdkInitialized = true))
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = 10, canRequestAds = true, isSdkInitialized = false))
    }

    @Test
    fun test4_taskLibraryAdIndex_returnsIndexFive_whenThresholdMet() {
        assertEquals(-1, NativeAdEligibility.getTaskLibraryAdIndex(0))
        assertEquals(-1, NativeAdEligibility.getTaskLibraryAdIndex(1))
        assertEquals(-1, NativeAdEligibility.getTaskLibraryAdIndex(4))
        assertEquals(-1, NativeAdEligibility.getTaskLibraryAdIndex(5))
        assertEquals(5, NativeAdEligibility.getTaskLibraryAdIndex(6))
        assertEquals(5, NativeAdEligibility.getTaskLibraryAdIndex(12))
    }

    @Test
    fun test5_isScreenAdPermitted_permitsOnlyAuthorizedScreens() {
        assertTrue(NativeAdEligibility.isScreenAdPermitted("DASHBOARD"))
        assertTrue(NativeAdEligibility.isScreenAdPermitted("TASK_LIBRARY"))

        // All other screens MUST remain completely ad-free
        assertFalse(NativeAdEligibility.isScreenAdPermitted("PLANS"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("PLAN_DETAIL"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("CALENDAR"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("RICH_NOTES"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("TASK_DETAIL"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("MISSION_MODE"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("WELCOME"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("SETTINGS"))
        assertFalse(NativeAdEligibility.isScreenAdPermitted("WIDGET"))
    }

    @Test
    fun test6_filteredList_appliesThresholdCorrectly() {
        val filteredListSize3 = 3
        val filteredListSize6 = 6

        // Even if total tasks = 10, if search/filter reduces count to 3 (< 6), no ad is shown
        assertFalse(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = filteredListSize3, canRequestAds = true, isSdkInitialized = true))

        // If search/filter returns 6 tasks (>= 6), ad is shown
        assertTrue(NativeAdEligibility.shouldShowTaskLibraryAd(taskCount = filteredListSize6, canRequestAds = true, isSdkInitialized = true))
    }
}
