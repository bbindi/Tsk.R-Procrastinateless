package com.brixavier.tskr.ad

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining User Messaging Platform (UMP) consent management.
 */
interface AdConsentManager {
    val canRequestAds: StateFlow<Boolean>
    val isPrivacyOptionsRequired: StateFlow<Boolean>
    val isGatheringConsent: StateFlow<Boolean>
    fun gatherConsent(activity: Activity, onComplete: () -> Unit = {})
    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit = {})
}
