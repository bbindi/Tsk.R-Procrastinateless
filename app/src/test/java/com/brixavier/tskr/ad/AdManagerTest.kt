package com.brixavier.tskr.ad

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused unit tests for AdMob & UMP consent foundation architecture.
 */
class AdManagerTest {

    private class FakeAdConsentManager(
        initialCanRequestAds: Boolean = false,
        initialPrivacyRequired: Boolean = false,
        private val shouldFailUpdate: Boolean = false
    ) : AdConsentManager {

        private val _canRequestAds = MutableStateFlow(initialCanRequestAds)
        override val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

        private val _isPrivacyOptionsRequired = MutableStateFlow(initialPrivacyRequired)
        override val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

        private val _isGatheringConsent = MutableStateFlow(false)
        override val isGatheringConsent: StateFlow<Boolean> = _isGatheringConsent.asStateFlow()

        var gatherConsentCount = 0
        var showPrivacyFormCount = 0

        override fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
            gatherConsentCount++
            if (!shouldFailUpdate) {
                _canRequestAds.value = true
                _isPrivacyOptionsRequired.value = true
            }
            onComplete()
        }

        override fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit) {
            showPrivacyFormCount++
            onComplete()
        }
    }

    private class TestableAdMobManager(
        private val consentManager: AdConsentManager
    ) : AdManager {

        override val canRequestAds: StateFlow<Boolean> = consentManager.canRequestAds
        override val isPrivacyOptionsRequired: StateFlow<Boolean> = consentManager.isPrivacyOptionsRequired

        private val _isInitialized = MutableStateFlow(false)
        override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

        var initializeSdkCallCount = 0

        override fun initializeSdk(context: Context) {
            if (_isInitialized.value) return
            if (consentManager.isGatheringConsent.value) return
            if (!canRequestAds.value) return

            initializeSdkCallCount++
            _isInitialized.value = true
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

    @Test
    fun test1_initialConsentState_preventsAdRequests() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = false)
        val adManager = TestableAdMobManager(fakeConsent)

        assertFalse(adManager.canRequestAds.value)
        assertFalse(adManager.isInitialized.value)
    }

    @Test
    fun test2_initializeSdk_blocked_whenConsentNotGranted() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = false)
        val adManager = TestableAdMobManager(fakeConsent)

        val dummyActivity = Activity()
        adManager.initializeSdk(dummyActivity)

        assertEquals(0, adManager.initializeSdkCallCount)
        assertFalse(adManager.isInitialized.value)
    }

    @Test
    fun test3_gatherConsent_grantsConsentAndInitializesSdk() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = false)
        val adManager = TestableAdMobManager(fakeConsent)

        val dummyActivity = Activity()
        adManager.gatherConsentAndInitialize(dummyActivity)

        assertEquals(1, fakeConsent.gatherConsentCount)
        assertTrue(adManager.canRequestAds.value)
        assertEquals(1, adManager.initializeSdkCallCount)
        assertTrue(adManager.isInitialized.value)
    }

    @Test
    fun test4_duplicateInitializationCalls_doNotReinitialize() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = true)
        val adManager = TestableAdMobManager(fakeConsent)

        val dummyActivity = Activity()
        adManager.initializeSdk(dummyActivity)
        adManager.initializeSdk(dummyActivity)
        adManager.initializeSdk(dummyActivity)

        assertEquals(1, adManager.initializeSdkCallCount)
        assertTrue(adManager.isInitialized.value)
    }

    @Test
    fun test5_consentUpdateFailure_handledSafelyWithoutCrashing() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = false, shouldFailUpdate = true)
        val adManager = TestableAdMobManager(fakeConsent)

        val dummyActivity = Activity()
        adManager.gatherConsentAndInitialize(dummyActivity)

        assertEquals(1, fakeConsent.gatherConsentCount)
        assertFalse(adManager.canRequestAds.value)
        assertEquals(0, adManager.initializeSdkCallCount)
        assertFalse(adManager.isInitialized.value)
    }

    @Test
    fun test6_privacyOptionsForm_delegatesToConsentManager() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = true, initialPrivacyRequired = true)
        val adManager = TestableAdMobManager(fakeConsent)

        val dummyActivity = Activity()
        adManager.showPrivacyOptionsForm(dummyActivity)

        assertEquals(1, fakeConsent.showPrivacyFormCount)
        assertTrue(adManager.isPrivacyOptionsRequired.value)
    }

    @Test
    fun test7_cachedConsentState_initializedFromConstructor() {
        val fakeConsent = FakeAdConsentManager(initialCanRequestAds = true, initialPrivacyRequired = true)
        val adManager = TestableAdMobManager(fakeConsent)

        assertTrue(adManager.canRequestAds.value)
        assertTrue(adManager.isPrivacyOptionsRequired.value)
    }
}
