package com.brixavier.tskr.ad

/**
 * Pure, testable business rules governing Native Ad placement eligibility across Tsk.R screens.
 */
object NativeAdEligibility {

    const val TASK_LIBRARY_MIN_THRESHOLD = 6

    /**
     * Determines whether the Dashboard is eligible to render its single native ad placement.
     */
    fun shouldShowDashboardAd(canRequestAds: Boolean, isSdkInitialized: Boolean): Boolean {
        return canRequestAds && isSdkInitialized
    }

    /**
     * Determines whether the Task Library list is eligible to render a native ad.
     * Must have at least 6 tasks ([TASK_LIBRARY_MIN_THRESHOLD]) and active consent/initialization.
     */
    fun shouldShowTaskLibraryAd(
        taskCount: Int,
        canRequestAds: Boolean,
        isSdkInitialized: Boolean
    ): Boolean {
        return taskCount >= TASK_LIBRARY_MIN_THRESHOLD && canRequestAds && isSdkInitialized
    }

    /**
     * Determines the zero-based list insertion index for the native ad in the Task Library.
     * Returns -1 if the list is shorter than [TASK_LIBRARY_MIN_THRESHOLD].
     */
    fun getTaskLibraryAdIndex(taskCount: Int): Int {
        if (taskCount < TASK_LIBRARY_MIN_THRESHOLD) return -1
        return 5 // Insert after the 5th item
    }

    /**
     * Verifies whether a given screen is authorized to display native ads.
     * Only "DASHBOARD" and "TASK_LIBRARY" are authorized.
     */
    fun isScreenAdPermitted(screenName: String): Boolean {
        val permittedScreens = setOf("DASHBOARD", "TASK_LIBRARY")
        return permittedScreens.contains(screenName.trim().uppercase())
    }
}
