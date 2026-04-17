package com.monospace.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.monospace.app.ui.theme.FocusTheme
import com.monospace.app.ui.theme.MONOSPACETheme
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Cancel if no valid widget ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setResult(RESULT_CANCELED) // default until user picks

        val activity = this
        setContent {
            MONOSPACETheme {
                var selected by remember {
                    mutableStateOf(WidgetThemeStore.load(activity, appWidgetId))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FocusTheme.colors.background)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chọn màu widget",
                        style = FocusTheme.typography.title.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(32.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WidgetTheme.entries.forEach { theme ->
                            ThemeOption(
                                theme = theme,
                                isSelected = selected == theme,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = theme }
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusTheme.colors.primary)
                            .clickable { confirmSelection(selected) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Xác nhận",
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.background,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }

    private fun confirmSelection(theme: WidgetTheme) {
        WidgetThemeStore.save(this, appWidgetId, theme)

        lifecycleScope.launch {
            WidgetUpdater.updateAll(this@WidgetConfigurationActivity)
        }

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
    }
}

@androidx.compose.runtime.Composable
private fun ThemeOption(
    theme: WidgetTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val previewBg = when (theme) {
        WidgetTheme.AUTO -> Color(0xFF2C2C2E)
        WidgetTheme.DARK -> Color(0xFF1C1C1E)
        WidgetTheme.LIGHT -> Color(0xFFF2F2F7)
    }
    val previewText = when (theme) {
        WidgetTheme.DARK -> Color(0xFFE8E8E8)
        else -> Color(0xFF1C1C1E)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.divider,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = previewText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            theme.label,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.primary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
