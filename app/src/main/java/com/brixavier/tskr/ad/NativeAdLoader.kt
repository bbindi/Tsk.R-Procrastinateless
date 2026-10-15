package com.brixavier.tskr.ad

import android.content.Context
import android.util.Log
import com.brixavier.tskr.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Utility responsible for loading native advertisements using Google's official AdLoader.
 */
object NativeAdLoader {
    private const val TAG = "NativeAdLoader"

    /**
     * Default Native Ad Unit ID injected via Gradle BuildConfig.
     * Defaults to Google sample test ID in development.
     */
    val DEFAULT_NATIVE_AD_UNIT_ID: String = BuildConfig.ADMOB_NATIVE_AD_UNIT_ID

    fun loadNativeAd(
        context: Context,
        adUnitId: String = DEFAULT_NATIVE_AD_UNIT_ID,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: (LoadAdError) -> Unit
    ) {
        try {
            val builder = AdLoader.Builder(context.applicationContext, adUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "Native ad loaded successfully.")
                    onAdLoaded(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native ad failed to load: ${error.code} - ${error.message}")
                        onAdFailed(error)
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )

            val adLoader = builder.build()
            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading native ad", e)
        }
    }
}
