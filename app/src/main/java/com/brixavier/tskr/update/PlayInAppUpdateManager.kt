package com.brixavier.tskr.update

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [InAppUpdateManager] interacting with Google Play AppUpdateManager.
 */
class PlayInAppUpdateManager(
    context: Context,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
) : InAppUpdateManager {

    companion object {
        private const val TAG = "InAppUpdateManager"
    }

    private val _updateStatus = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle)
    override val updateStatus: StateFlow<InAppUpdateStatus> = _updateStatus.asStateFlow()

    private var cachedAppUpdateInfo: AppUpdateInfo? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val downloaded = state.bytesDownloaded()
                val total = state.totalBytesToDownload()
                Log.d(TAG, "Update downloading: $downloaded / $total bytes")
                _updateStatus.value = InAppUpdateStatus.Downloading(downloaded, total)
            }
            InstallStatus.DOWNLOADED -> {
                Log.d(TAG, "Update downloaded and ready to install.")
                _updateStatus.value = InAppUpdateStatus.Downloaded
            }
            InstallStatus.FAILED -> {
                Log.e(TAG, "Update failed with error code: ${state.installErrorCode()}")
                _updateStatus.value = InAppUpdateStatus.Failed(state.installErrorCode())
            }
            InstallStatus.CANCELED -> {
                Log.d(TAG, "Update canceled by user.")
                _updateStatus.value = InAppUpdateStatus.Canceled
            }
            InstallStatus.INSTALLED -> {
                Log.d(TAG, "Update installed.")
                _updateStatus.value = InAppUpdateStatus.Idle
                unregisterListener()
            }
            else -> {
                // Other statuses
            }
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register InstallStateUpdatedListener", e)
        }
    }

    override fun checkForUpdate() {
        if (_updateStatus.value is InAppUpdateStatus.Dismissed) {
            Log.d(TAG, "Update prompt previously dismissed in session. Skipping check.")
            return
        }

        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                cachedAppUpdateInfo = appUpdateInfo

                when (appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            Log.d(TAG, "Update is already DOWNLOADED.")
                            _updateStatus.value = InAppUpdateStatus.Downloaded
                        } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            Log.d(TAG, "Flexible update available.")
                            _updateStatus.value = InAppUpdateStatus.UpdateAvailable
                        } else {
                            Log.d(TAG, "Update available, but flexible update type is not allowed.")
                        }
                    }
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            Log.d(TAG, "In-progress update is already DOWNLOADED.")
                            _updateStatus.value = InAppUpdateStatus.Downloaded
                        } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING) {
                            Log.d(TAG, "In-progress update is currently DOWNLOADING.")
                            _updateStatus.value = InAppUpdateStatus.Downloading(
                                appUpdateInfo.bytesDownloaded(),
                                appUpdateInfo.totalBytesToDownload()
                            )
                        }
                    }
                    else -> {
                        Log.d(TAG, "Update availability status: ${appUpdateInfo.updateAvailability()}")
                    }
                }
            }.addOnFailureListener { e ->
                Log.d(TAG, "Failed to check for app update (Play Store likely unavailable on device): ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during checkForUpdate", e)
        }
    }

    override fun startFlexibleUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val info = cachedAppUpdateInfo
        if (info == null) {
            Log.e(TAG, "Cannot start flexible update: cachedAppUpdateInfo is null.")
            return
        }

        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting flexible update flow", e)
        }
    }

    override fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error completing update", e)
        }
    }

    override fun dismissUpdatePrompt() {
        _updateStatus.value = InAppUpdateStatus.Dismissed
    }

    override fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering listener", e)
        }
    }
}
