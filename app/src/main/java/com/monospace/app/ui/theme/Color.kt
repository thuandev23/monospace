package com.monospace.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FocusColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val secondary: Color,
    val destructive: Color,
    val success: Color,
    val divider: Color,
    val onPrimary: Color,
    val onBackground: Color
)

val LightFocusColors = FocusColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF9F9F9),
    surfaceAlt = Color(0xFFF5F5F5),
    primary = Color(0xFF000000),
    secondary = Color(0xFF6B7280),
    destructive = Color(0xFFEF4444),
    success = Color(0xFF22C55E),
    divider = Color(0xFFE5E7EB),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000)
)

val DarkFocusColors = FocusColors(
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    surfaceAlt = Color(0xFF1A1A1A),
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFF9CA3AF),
    destructive = Color(0xFFF87171),
    success = Color(0xFF4ADE80),
    divider = Color(0xFF262626),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF)
)

val RemindersLightFocusColors = FocusColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F7),
    surfaceAlt = Color(0xFFE5E5EA),
    primary = Color(0xFF007AFF),
    secondary = Color(0xFF6B7280),
    destructive = Color(0xFFFF3B30),
    success = Color(0xFF34C759),
    divider = Color(0xFFE5E7EB),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000)
)

val RemindersDarkFocusColors = FocusColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceAlt = Color(0xFF2C2C2E),
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF9CA3AF),
    destructive = Color(0xFFFF453A),
    success = Color(0xFF30D158),
    divider = Color(0xFF38383A),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF)
)

val LocalFocusColors = staticCompositionLocalOf { LightFocusColors }
