package com.brixavier.tskr.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production implementation of [AdManager] coordinating UMP consent and AdMob SDK initialization.
 * No ads are loaded or displayed in Phase 1.
 */
class AdMobManager(
    private val consentManager: AdConsentManager
) : AdManager {

    companion object {
        private const val TAG = "AdMobManager"
    }

    override val canRequestAds: StateFlow<Boolean> = consentManager.canRequestAds
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = consentManager.isPrivacyOptionsRequired

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var isInitializing = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun initializeSdk(context: Context) {
        if (_isInitialized.value || isInitializing) {
            Log.d(TAG, "AdMob SDK already initialized or initializing.")
            return
        }

        if (consentManager.isGatheringConsent.value) {
            Log.d(TAG, "Consent gathering flow is currently active. Deferring SDK initialization until flow completes.")
            return
        }

        if (!canRequestAds.value) {
            Log.d(TAG, "Cannot initialize AdMob SDK: UMP consent state does not permit ad requests.")
            return
        }

        isInitializing = true
        scope.launch {
            try {
                Log.d(TAG, "Initializing AdMob SDK on background thread...")
                MobileAds.initialize(context.applicationContext) { status: InitializationStatus ->
                    _isInitialized.value = true
                    isInitializing = false
                    Log.d(TAG, "AdMob SDK initialization complete. Adapter status map: ${status.adapterStatusMap}")
                }
            } catch (e: Exception) {
                isInitializing = false
                Log.e(TAG, "Failed to initialize AdMob SDK", e)
            }
        }
    }

    override fun gatherConsentAndInitialize(activity: Activity) {
        consentManager.gatherConsent(activity) {
            if (canRequestAds.value) {
                initializeSdk(activity)
            }
        }
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        consentManager.showPrivacyOptionsForm(activity)
    }
}
