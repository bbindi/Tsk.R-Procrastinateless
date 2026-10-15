package com.brixavier.tskr.ad

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Reusable Compose component rendering an AdMob Native Ad within Tsk.R's Material 3 Expressive styling.
 * Automatically handles resource cleanup on disposal and collapses to 0.dp height if an ad fails to load.
 */
@Composable
fun NativeAdCard(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    val canRequestAds by adManager.canRequestAds.collectAsState()
    val isInitialized by adManager.isInitialized.collectAsState()
    val context = LocalContext.current

    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var adLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(canRequestAds, isInitialized) {
        if (canRequestAds && isInitialized && nativeAdState == null && !adLoadFailed) {
            NativeAdLoader.loadNativeAd(
                context = context,
                onAdLoaded = { ad ->
                    nativeAdState?.destroy()
                    nativeAdState = ad
                },
                onAdFailed = { _ ->
                    adLoadFailed = true
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAdState?.destroy()
            nativeAdState = null
        }
    }

    val ad = nativeAdState
    if (ad != null && !adLoadFailed) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ad Badge Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Ad",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    ad.store?.let { storeName ->
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // NativeAdView Binding
                AndroidView(
                    factory = { ctx ->
                        val nativeAdView = NativeAdView(ctx)

                        val layout = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                        }

                        val headlineView = TextView(ctx).apply {
                            textSize = 16f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(android.graphics.Color.BLACK)
                        }
                        val bodyView = TextView(ctx).apply {
                            textSize = 14f
                            setTextColor(android.graphics.Color.GRAY)
                        }
                        val ctaView = Button(ctx).apply {
                            textSize = 14f
                        }

                        layout.addView(headlineView)
                        layout.addView(bodyView)
                        layout.addView(ctaView)

                        nativeAdView.addView(layout)
                        nativeAdView.headlineView = headlineView
                        nativeAdView.bodyView = bodyView
                        nativeAdView.callToActionView = ctaView

                        nativeAdView
                    },
                    update = { view ->
                        (view.headlineView as? TextView)?.text = ad.headline
                        (view.bodyView as? TextView)?.text = ad.body
                        (view.callToActionView as? Button)?.text = ad.callToAction ?: "Learn More"

                        view.setNativeAd(ad)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
