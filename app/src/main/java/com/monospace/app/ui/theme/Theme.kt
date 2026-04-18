package com.monospace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.monospace.app.core.domain.model.AppTheme

object FocusTheme {
    val colors: FocusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusColors.current

    val typography: FocusTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFocusTypography.current
}

@Composable
fun MONOSPACETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.MINIMALIST,
    content: @Composable () -> Unit
) {
    val colors = when (appTheme) {
        AppTheme.MINIMALIST -> if (darkTheme) DarkFocusColors else LightFocusColors
        AppTheme.REMINDERS -> if (darkTheme) RemindersDarkFocusColors else RemindersLightFocusColors
    }

    CompositionLocalProvider(
        LocalFocusColors provides colors,
        LocalFocusTypography provides FocusTypographyDefault
    ) {
        MaterialTheme(
            typography = Typography,
            content = content
        )
    }
}
