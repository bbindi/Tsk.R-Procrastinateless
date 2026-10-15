package com.brixavier.tskr.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Production implementation of [InAppReviewManager] interacting with Google Play ReviewManager API.
 */
class PlayInAppReviewManager(
    context: Context,
    override val eligibilityEngine: InAppReviewEligibilityEngine = InAppReviewEligibilityEngine.create(context),
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context.applicationContext)
) : InAppReviewManager {

    companion object {
        private const val TAG = "PlayInAppReviewManager"
        private const val APP_PACKAGE_NAME = "com.brixavier.tskr"

        private fun logDebug(msg: String) {
            try {
                Log.d(TAG, msg)
            } catch (_: Throwable) {}
        }

        private fun logWarn(msg: String) {
            try {
                Log.w(TAG, msg)
            } catch (_: Throwable) {}
        }

        private fun logError(msg: String, e: Throwable) {
            try {
                Log.e(TAG, msg, e)
            } catch (_: Throwable) {}
        }
    }

    private var hasEvaluatedInSession = false

    override fun requestReviewIfEligible(activity: Activity, onComplete: () -> Unit) {
        if (hasEvaluatedInSession) {
            logDebug("Review prompt eligibility already evaluated in current session. Skipping.")
            onComplete()
            return
        }

        if (!eligibilityEngine.isEligibleForReviewPrompt()) {
            logDebug("App is currently not eligible for Google Play In-App Review prompt.")
            onComplete()
            return
        }

        hasEvaluatedInSession = true
        try {
            logDebug("Requesting Google Play In-App Review flow...")
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        logDebug("In-App Review flow completed asynchronously.")
                        eligibilityEngine.recordPromptAttempt()
                        onComplete()
                    }
                } else {
                    logWarn("Failed to retrieve ReviewInfo from Google Play Review API: ${task.exception?.message}")
                    onComplete()
                }
            }
        } catch (e: Exception) {
            logError("Unexpected error in requestReviewIfEligible", e)
            onComplete()
        }
    }

    override fun launchPlayStoreListing(context: Context) {
        val marketUri = Uri.parse("market://details?id=$APP_PACKAGE_NAME")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$APP_PACKAGE_NAME")

        val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to web browser if Play Store app is unavailable
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (e: Exception) {
                logError("Unable to launch Play Store listing web fallback", e)
            }
        }
    }
}
