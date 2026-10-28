package com.brixavier.tskr.ad

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
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

    val colorScheme = MaterialTheme.colorScheme
    val headlineColor = colorScheme.onSurface.toArgb()
    val bodyColor = colorScheme.onSurfaceVariant.toArgb()
    val advertiserColor = colorScheme.onSurfaceVariant.copy(alpha = 0.85f).toArgb()
    val ctaBgColor = colorScheme.primary.toArgb()
    val ctaTextColor = colorScheme.onPrimary.toArgb()

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
                // Ad Badge Header Row (keeps Ad badge clearly visible and distinct)
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

                // NativeAdView Binding with full asset handling
                AndroidView(
                    factory = { ctx ->
                        val density = ctx.resources.displayMetrics.density
                        val dpToPx = { dp: Int -> (dp * density).toInt() }

                        val nativeAdView = NativeAdView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }

                        val rootLayout = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }

                        // 1. App Info Header Row (Icon + Headline / Advertiser)
                        // Top-right margin reserves room for AdMob AdChoices overlay
                        val appInfoRow = LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, dpToPx(28), 0)
                            }
                        }

                        val iconView = ImageView(ctx).apply {
                            val iconSize = dpToPx(44)
                            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                                marginEnd = dpToPx(12)
                            }
                            outlineProvider = object : ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 10f * density)
                                }
                            }
                            clipToOutline = true
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            visibility = View.GONE
                        }

                        val textColumn = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                        }

                        val headlineView = TextView(ctx).apply {
                            textSize = 15f
                            setTypeface(null, Typeface.BOLD)
                            maxLines = 2
                            ellipsize = TextUtils.TruncateAt.END
                        }

                        val advertiserView = TextView(ctx).apply {
                            textSize = 12f
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(2)
                            }
                            visibility = View.GONE
                        }

                        textColumn.addView(headlineView)
                        textColumn.addView(advertiserView)

                        appInfoRow.addView(iconView)
                        appInfoRow.addView(textColumn)

                        // 2. MediaView for visual creatives (collapses to GONE if not provided)
                        val mediaView = MediaView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                dpToPx(180)
                            ).apply {
                                topMargin = dpToPx(8)
                                bottomMargin = dpToPx(4)
                            }
                            outlineProvider = object : ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: android.graphics.Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 12f * density)
                                }
                            }
                            clipToOutline = true
                            visibility = View.GONE
                        }

                        // 3. Body text (collapses to GONE if empty)
                        val bodyView = TextView(ctx).apply {
                            textSize = 13f
                            maxLines = 3
                            ellipsize = TextUtils.TruncateAt.END
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(6)
                                bottomMargin = dpToPx(6)
                            }
                            visibility = View.GONE
                        }

                        // 4. Call To Action button (styled with M3 primary tokens)
                        val ctaView = TextView(ctx).apply {
                            textSize = 14f
                            setTypeface(null, Typeface.BOLD)
                            gravity = Gravity.CENTER
                            val padV = dpToPx(10)
                            val padH = dpToPx(16)
                            setPadding(padH, padV, padH, padV)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = dpToPx(8)
                            }
                            isClickable = true
                            isFocusable = true
                        }

                        rootLayout.addView(appInfoRow)
                        rootLayout.addView(mediaView)
                        rootLayout.addView(bodyView)
                        rootLayout.addView(ctaView)

                        nativeAdView.addView(rootLayout)

                        nativeAdView.headlineView = headlineView
                        nativeAdView.advertiserView = advertiserView
                        nativeAdView.iconView = iconView
                        nativeAdView.mediaView = mediaView
                        nativeAdView.bodyView = bodyView
                        nativeAdView.callToActionView = ctaView

                        nativeAdView
                    },
                    update = { view ->
                        val headlineView = view.headlineView as? TextView
                        val advertiserView = view.advertiserView as? TextView
                        val iconView = view.iconView as? ImageView
                        val mediaView = view.mediaView
                        val bodyView = view.bodyView as? TextView
                        val ctaView = view.callToActionView as? TextView

                        // Headline text and theme-aware contrast color
                        headlineView?.apply {
                            text = ad.headline
                            setTextColor(headlineColor)
                        }

                        // Advertiser / Store text
                        val advertiserText = ad.advertiser ?: ad.store
                        if (!advertiserText.isNullOrBlank()) {
                            advertiserView?.apply {
                                text = advertiserText
                                setTextColor(advertiserColor)
                                visibility = View.VISIBLE
                            }
                        } else {
                            advertiserView?.visibility = View.GONE
                        }

                        // App Icon
                        val icon = ad.icon
                        if (icon != null && icon.drawable != null) {
                            iconView?.apply {
                                setImageDrawable(icon.drawable)
                                visibility = View.VISIBLE
                            }
                        } else {
                            iconView?.visibility = View.GONE
                        }

                        // Media Content
                        val mediaContent = ad.mediaContent
                        if (mediaContent != null && (mediaContent.hasVideoContent() || mediaContent.aspectRatio > 0f || ad.images.isNotEmpty())) {
                            mediaView?.apply {
                                setMediaContent(mediaContent)
                                setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                                visibility = View.VISIBLE
                            }
                        } else {
                            mediaView?.visibility = View.GONE
                        }

                        // Body text
                        if (!ad.body.isNullOrBlank()) {
                            bodyView?.apply {
                                text = ad.body
                                setTextColor(bodyColor)
                                visibility = View.VISIBLE
                            }
                        } else {
                            bodyView?.visibility = View.GONE
                        }

                        // Call To Action button
                        ctaView?.apply {
                            text = ad.callToAction ?: "Learn More"
                            setTextColor(ctaTextColor)
                            val ctaBg = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = 12f * context.resources.displayMetrics.density
                                setColor(ctaBgColor)
                            }
                            background = ctaBg
                        }

                        view.setNativeAd(ad)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
