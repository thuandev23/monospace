package com.monospace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

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
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkFocusColors else LightFocusColors
    
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
