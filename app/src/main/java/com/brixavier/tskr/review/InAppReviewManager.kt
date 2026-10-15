package com.brixavier.tskr.review

import android.app.Activity
import android.content.Context

/**
 * Interface defining Google Play In-App Review operations.
 */
interface InAppReviewManager {
    val eligibilityEngine: InAppReviewEligibilityEngine
    fun requestReviewIfEligible(activity: Activity, onComplete: () -> Unit = {})
    fun launchPlayStoreListing(context: Context)
}
