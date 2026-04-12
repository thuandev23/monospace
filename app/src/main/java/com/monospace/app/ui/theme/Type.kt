package com.monospace.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.monospace.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)

data class FocusTypography(
    val displayLarge: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle
)

val FocusTypographyDefault = FocusTypography(
    displayLarge = TextStyle(
        fontFamily = Inter,
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold
    ),
    title = TextStyle(
        fontFamily = Inter,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    ),
    headline = TextStyle(
        fontFamily = Inter,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontSize = 16.sp
    ),
    label = TextStyle(
        fontFamily = Inter,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = Inter,
        fontSize = 12.sp
    )
)

val LocalFocusTypography = staticCompositionLocalOf { FocusTypographyDefault }

val Typography = Typography(
    displayLarge = FocusTypographyDefault.displayLarge,
    headlineMedium = FocusTypographyDefault.headline,
    bodyLarge = FocusTypographyDefault.body,
    labelLarge = FocusTypographyDefault.label,
    bodySmall = FocusTypographyDefault.caption
)
