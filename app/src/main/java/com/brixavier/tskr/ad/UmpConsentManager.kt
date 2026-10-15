package com.brixavier.tskr.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production UMP consent manager implementing [AdConsentManager].
 */
class UmpConsentManager(
    private val context: Context
) : AdConsentManager {

    companion object {
        private const val TAG = "UmpConsentManager"
    }

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context.applicationContext)
    }

    private val _canRequestAds = MutableStateFlow(false)
    override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    override val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _isGatheringConsent = MutableStateFlow(false)
    override val isGatheringConsent: StateFlow<Boolean> = _isGatheringConsent.asStateFlow()

    init {
        updateConsentState()
    }

    override fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        if (_isGatheringConsent.value) {
            Log.d(TAG, "Consent gathering process already in progress.")
            onComplete()
            return
        }

        _isGatheringConsent.value = true

        val params = ConsentRequestParameters.Builder().build()

        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    // Consent info update success
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        _isGatheringConsent.value = false
                        if (formError != null) {
                            Log.w(TAG, "Consent form error: ${formError.errorCode} - ${formError.message}")
                        }
                        updateConsentState()
                        onComplete()
                    }
                },
                { requestConsentError ->
                    // Consent info update failure (e.g. offline, network error)
                    _isGatheringConsent.value = false
                    Log.w(TAG, "Consent info update failed: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                    updateConsentState()
                    onComplete()
                }
            )
        } catch (e: Exception) {
            _isGatheringConsent.value = false
            Log.e(TAG, "Unexpected error in gatherConsent", e)
            updateConsentState()
            onComplete()
        }
    }

    override fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.w(TAG, "Privacy options form error: ${formError.errorCode} - ${formError.message}")
                }
                updateConsentState()
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in showPrivacyOptionsForm", e)
            onComplete()
        }
    }

    private fun updateConsentState() {
        try {
            val canRequest = consentInformation.canRequestAds()
            val privacyRequired = consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

            _canRequestAds.value = canRequest
            _isPrivacyOptionsRequired.value = privacyRequired
            Log.d(TAG, "Updated consent state: canRequestAds=$canRequest, privacyRequired=$privacyRequired")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating consent state", e)
        }
    }
}
