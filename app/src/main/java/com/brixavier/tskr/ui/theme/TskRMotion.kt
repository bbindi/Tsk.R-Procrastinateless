package com.brixavier.tskr.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized motion specifications for Tsk.R UI transitions.
 *
 * Defines standardized durations, easing curves, and lightweight enter/exit transitions
 * designed for 60fps+ performance on all Android devices.
 */
object TskRMotion {

    // Duration Tokens (in milliseconds)
    const val DURATION_MICRO = 150       // Reserved for micro-interactions
    const val DURATION_STANDARD = 220    // Used for screen navigation and tab transitions
    const val DURATION_PERSONALITY = 350 // Reserved for personality / celebration moments

    // Easing Curves
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingEmphasized: Easing = LinearOutSlowInEasing

    // Slide Translation Offset
    val TranslationOffset: Dp = 12.dp

    /**
     * Standard screen enter transition: subtle 12dp horizontal slide + fade in.
     */
    fun enterTransition(
        density: Density,
        slideOffsetDp: Dp = TranslationOffset,
        durationMs: Int = DURATION_STANDARD
    ): EnterTransition {
        val slidePx = with(density) { slideOffsetDp.roundToPx() }
        return slideInHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard),
            initialOffsetX = { slidePx }
        ) + fadeIn(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard)
        )
    }

    /**
     * Standard screen exit transition: subtle 12dp horizontal slide + fade out.
     */
    fun exitTransition(
        density: Density,
        slideOffsetDp: Dp = TranslationOffset,
        durationMs: Int = DURATION_STANDARD
    ): ExitTransition {
        val slidePx = with(density) { slideOffsetDp.roundToPx() }
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard),
            targetOffsetX = { -slidePx }
        ) + fadeOut(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard)
        )
    }

    /**
     * Standard pop enter transition (when returning back): subtle reverse slide + fade in.
     */
    fun popEnterTransition(
        density: Density,
        slideOffsetDp: Dp = TranslationOffset,
        durationMs: Int = DURATION_STANDARD
    ): EnterTransition {
        val slidePx = with(density) { slideOffsetDp.roundToPx() }
        return slideInHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard),
            initialOffsetX = { -slidePx }
        ) + fadeIn(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard)
        )
    }

    /**
     * Standard pop exit transition (when returning back): subtle reverse slide + fade out.
     */
    fun popExitTransition(
        density: Density,
        slideOffsetDp: Dp = TranslationOffset,
        durationMs: Int = DURATION_STANDARD
    ): ExitTransition {
        val slidePx = with(density) { slideOffsetDp.roundToPx() }
        return slideOutHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard),
            targetOffsetX = { slidePx }
        ) + fadeOut(
            animationSpec = tween(durationMillis = durationMs, easing = EasingStandard)
        )
    }

    /**
     * Lightweight tab transition for switching bottom navigation items: subtle fade.
     */
    fun tabTransition(
        durationMs: Int = DURATION_STANDARD
    ): ContentTransform {
        return (fadeIn(animationSpec = tween(durationMillis = durationMs, easing = EasingStandard))) togetherWith
               (fadeOut(animationSpec = tween(durationMillis = durationMs, easing = EasingStandard)))
    }

    /**
     * Calculates a hardware-accelerated parabolic arc offset along progress [t] (0.0 to 1.0)
     * from [startX], [startY] to [endX], [endY] with specified [arcHeightPx].
     */
    fun calculateParabolicOffset(
        t: Float,
        startX: Float,
        endX: Float,
        startY: Float,
        endY: Float,
        arcHeightPx: Float
    ): Pair<Float, Float> {
        val x = startX + t * (endX - startX)
        val y = startY + t * (endY - startY) - (4f * arcHeightPx * t * (1f - t))
        return Pair(x, y)
    }
}
