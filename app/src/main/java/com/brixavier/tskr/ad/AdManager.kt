package com.brixavier.tskr.ad

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Foundation interface for AdMob initialization and consent coordination.
 */
interface AdManager {
    val canRequestAds: StateFlow<Boolean>
    val isInitialized: StateFlow<Boolean>
    val isPrivacyOptionsRequired: StateFlow<Boolean>
    fun initializeSdk(context: Context)
    fun gatherConsentAndInitialize(activity: Activity)
    fun showPrivacyOptionsForm(activity: Activity)
}
