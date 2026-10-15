package com.brixavier.tskr.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering [InAppUpdateManager] state machine logic.
 */
class InAppUpdateManagerTest {

    private class FakeInAppUpdateManager(
        var isUpdateAvailable: Boolean = true,
        var isFlexibleAllowed: Boolean = true
    ) : InAppUpdateManager {

        private val _updateStatus = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle)
        override val updateStatus: StateFlow<InAppUpdateStatus> = _updateStatus.asStateFlow()

        var checkCount = 0
        var startCount = 0
        var completeCount = 0
        var unregisterCount = 0

        override fun checkForUpdate() {
            checkCount++
            if (_updateStatus.value is InAppUpdateStatus.Dismissed || _updateStatus.value is InAppUpdateStatus.Downloaded) return

            if (isUpdateAvailable && isFlexibleAllowed) {
                _updateStatus.value = InAppUpdateStatus.UpdateAvailable
            }
        }

        override fun startFlexibleUpdate(
            activity: Activity,
            launcher: ActivityResultLauncher<IntentSenderRequest>
        ) {
            startCount++
            _updateStatus.value = InAppUpdateStatus.Downloading(500, 1000)
        }

        fun simulateDownloadComplete() {
            _updateStatus.value = InAppUpdateStatus.Downloaded
        }

        fun simulateDownloadFailure(errorCode: Int) {
            _updateStatus.value = InAppUpdateStatus.Failed(errorCode)
        }

        fun simulateCancel() {
            _updateStatus.value = InAppUpdateStatus.Canceled
        }

        override fun completeUpdate() {
            completeCount++
            _updateStatus.value = InAppUpdateStatus.Idle
        }

        override fun dismissUpdatePrompt() {
            _updateStatus.value = InAppUpdateStatus.Dismissed
        }

        override fun unregisterListener() {
            unregisterCount++
        }
    }

    @Test
    fun test1_initialStatusIsIdle() {
        val manager = FakeInAppUpdateManager()
        assertEquals(InAppUpdateStatus.Idle, manager.updateStatus.value)
    }

    @Test
    fun test2_checkForUpdate_transitionsToUpdateAvailable_whenAllowed() {
        val manager = FakeInAppUpdateManager(isUpdateAvailable = true, isFlexibleAllowed = true)
        manager.checkForUpdate()
        assertEquals(1, manager.checkCount)
        assertEquals(InAppUpdateStatus.UpdateAvailable, manager.updateStatus.value)
    }

    @Test
    fun test3_checkForUpdate_remainsIdle_whenNoUpdateAvailable() {
        val manager = FakeInAppUpdateManager(isUpdateAvailable = false)
        manager.checkForUpdate()
        assertEquals(1, manager.checkCount)
        assertEquals(InAppUpdateStatus.Idle, manager.updateStatus.value)
    }

    @Test
    fun test4_startFlexibleUpdate_transitionsToDownloading() {
        val manager = FakeInAppUpdateManager()
        manager.checkForUpdate()

        // Create dummy objects (not invoked directly in unit test)
        val dummyActivity = Activity()
        val dummyLauncher = object : ActivityResultLauncher<IntentSenderRequest>() {
            override fun launch(input: IntentSenderRequest?, options: ActivityOptionsCompat?) {}
            override fun unregister() {}
            override fun getContract(): ActivityResultContract<IntentSenderRequest, *> {
                throw UnsupportedOperationException()
            }
        }

        manager.startFlexibleUpdate(dummyActivity, dummyLauncher)
        assertEquals(1, manager.startCount)
        assertTrue(manager.updateStatus.value is InAppUpdateStatus.Downloading)

        val downloading = manager.updateStatus.value as InAppUpdateStatus.Downloading
        assertEquals(500L, downloading.bytesDownloaded)
        assertEquals(1000L, downloading.totalBytesToDownload)
    }

    @Test
    fun test5_downloadComplete_transitionsToDownloaded() {
        val manager = FakeInAppUpdateManager()
        manager.simulateDownloadComplete()
        assertEquals(InAppUpdateStatus.Downloaded, manager.updateStatus.value)
    }

    @Test
    fun test6_completeUpdate_resetsToIdle() {
        val manager = FakeInAppUpdateManager()
        manager.simulateDownloadComplete()
        assertEquals(InAppUpdateStatus.Downloaded, manager.updateStatus.value)

        manager.completeUpdate()
        assertEquals(1, manager.completeCount)
        assertEquals(InAppUpdateStatus.Idle, manager.updateStatus.value)
    }

    @Test
    fun test7_dismissUpdatePrompt_preventsRepeatedUpdateCheck() {
        val manager = FakeInAppUpdateManager()
        manager.checkForUpdate()
        assertEquals(InAppUpdateStatus.UpdateAvailable, manager.updateStatus.value)

        manager.dismissUpdatePrompt()
        assertEquals(InAppUpdateStatus.Dismissed, manager.updateStatus.value)

        // Second check call should be ignored
        manager.checkForUpdate()
        assertEquals(2, manager.checkCount)
        assertEquals(InAppUpdateStatus.Dismissed, manager.updateStatus.value)
    }

    @Test
    fun test8_failedAndCanceledUpdates_handledSafely() {
        val manager = FakeInAppUpdateManager()
        manager.simulateDownloadFailure(-100)
        assertTrue(manager.updateStatus.value is InAppUpdateStatus.Failed)

        manager.simulateCancel()
        assertEquals(InAppUpdateStatus.Canceled, manager.updateStatus.value)
    }

    @Test
    fun test9_checkForUpdate_preservesDownloadedState_whenUpdateAlreadyDownloaded() {
        val manager = FakeInAppUpdateManager()
        manager.simulateDownloadComplete()
        assertEquals(InAppUpdateStatus.Downloaded, manager.updateStatus.value)

        manager.checkForUpdate()
        assertEquals(InAppUpdateStatus.Downloaded, manager.updateStatus.value)
    }
}
