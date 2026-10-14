package com.brixavier.tskr.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining In-App Update management operations.
 */
interface InAppUpdateManager {
    val updateStatus: StateFlow<InAppUpdateStatus>
    fun checkForUpdate()
    fun startFlexibleUpdate(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>)
    fun completeUpdate()
    fun dismissUpdatePrompt()
    fun unregisterListener()
}
