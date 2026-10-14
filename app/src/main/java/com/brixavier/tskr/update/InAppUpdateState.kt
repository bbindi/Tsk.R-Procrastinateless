package com.brixavier.tskr.update

/**
 * Domain state representing the status of Google Play In-App Updates.
 */
sealed class InAppUpdateStatus {
    object Idle : InAppUpdateStatus()
    object UpdateAvailable : InAppUpdateStatus()
    data class Downloading(val bytesDownloaded: Long, val totalBytesToDownload: Long) : InAppUpdateStatus()
    object Downloaded : InAppUpdateStatus()
    data class Failed(val errorCode: Int) : InAppUpdateStatus()
    object Canceled : InAppUpdateStatus()
    object Dismissed : InAppUpdateStatus()
}
