package com.monospace.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.monospace.app.R

enum class WidgetTheme(val label: String) {
    AUTO("Tự động"),
    DARK("Dark"),
    LIGHT("Light")
}

object WidgetThemeStore {
    private const val PREFS = "widget_theme_prefs"
    private const val KEY_PREFIX = "theme_"

    fun save(context: Context, appWidgetId: Int, theme: WidgetTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("$KEY_PREFIX$appWidgetId", theme.name).apply()
    }

    fun load(context: Context, appWidgetId: Int): WidgetTheme {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("$KEY_PREFIX$appWidgetId", WidgetTheme.AUTO.name)
        return runCatching { WidgetTheme.valueOf(name!!) }.getOrDefault(WidgetTheme.AUTO)
    }

    fun remove(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("$KEY_PREFIX$appWidgetId").apply()
    }
}

data class WidgetColorSet(
    val surface: ColorProvider,
    val primary: ColorProvider,
    val onSurface: ColorProvider,
    val secondary: ColorProvider,
    val accent: ColorProvider
)

fun WidgetTheme.toColorSet(): WidgetColorSet = when (this) {
    WidgetTheme.AUTO -> WidgetColorSet(
        surface   = ColorProvider(R.color.widget_surface),
        primary   = ColorProvider(R.color.widget_primary),
        onSurface = ColorProvider(R.color.widget_on_surface),
        secondary = ColorProvider(R.color.widget_secondary),
        accent    = ColorProvider(R.color.widget_accent)
    )
    WidgetTheme.DARK -> WidgetColorSet(
        surface   = ColorProvider(Color(0xFF1C1C1E)),
        primary   = ColorProvider(Color(0xFFE8E8E8)),
        onSurface = ColorProvider(Color(0xFFD0D0D0)),
        secondary = ColorProvider(Color(0xFF8A8A8E)),
        accent    = ColorProvider(Color(0xFFFFFFFF))
    )
    WidgetTheme.LIGHT -> WidgetColorSet(
        surface   = ColorProvider(Color(0xFFF2F2F7)),
        primary   = ColorProvider(Color(0xFF1C1C1E)),
        onSurface = ColorProvider(Color(0xFF2C2C2E)),
        secondary = ColorProvider(Color(0xFF6C6C70)),
        accent    = ColorProvider(Color(0xFF000000))
    )
}
