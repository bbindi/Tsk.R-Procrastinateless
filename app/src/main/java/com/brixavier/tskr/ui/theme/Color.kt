package com.brixavier.tskr.ui.theme

import androidx.compose.ui.graphics.Color

// Functional Color Tokens
val PrimaryBlue = Color(0xFF2196F3)
val CriticalPurple = Color(0xFF9C27B0)
val WarningOrange = Color(0xFFFF9800)
val SuccessGreen = Color(0xFF4CAF50)
val SecondaryGray = Color(0xFF757575)

// Semantic Task Status Colors
val StatusOverdueLight = Color(0xFFC62828)
val StatusOverdueDark = Color(0xFFFF5252)

val StatusUpcomingLight = Color(0xFFE65100)
val StatusUpcomingDark = Color(0xFFFFB74D)

val StatusCompletedLight = Color(0xFF2E7D32)
val StatusCompletedDark = Color(0xFF69F0AE)

fun getStatusOverdueColor(isDark: Boolean): Color = if (isDark) StatusOverdueDark else StatusOverdueLight
fun getStatusUpcomingColor(isDark: Boolean): Color = if (isDark) StatusUpcomingDark else StatusUpcomingLight
fun getStatusCompletedColor(isDark: Boolean): Color = if (isDark) StatusCompletedDark else StatusCompletedLight

// Theme Colors - Dark
val DarkPrimary = PrimaryBlue
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF004BA0)
val DarkOnPrimaryContainer = Color(0xFFD1E4FF)

val DarkBackground = Color(0xFF0A0E29)
val DarkOnBackground = Color(0xFFEEEEEE)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnSurfaceVariant = Color(0xFFCCCCCC)

val DarkOutline = Color(0xFF555555)
val DarkError = CriticalPurple

// Theme Colors - Light
val LightPrimary = PrimaryBlue
val LightOnPrimary = Color.White
val LightBackground = Color(0xFFFDFBFF)
val LightOnBackground = Color(0xFF191C1E)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF191C1E)
val LightOutline = Color(0xFF74777F)
