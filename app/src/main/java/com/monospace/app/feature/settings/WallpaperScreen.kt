package com.monospace.app.feature.settings

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.ui.theme.FocusTheme
import com.monospace.app.widget.ClockDateWidgetReceiver
import com.monospace.app.widget.TaskListWidgetReceiver
import com.monospace.app.widget.WidgetTheme
import com.monospace.app.widget.WidgetThemeStore
import com.monospace.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedWallpaper by remember { mutableStateOf<WallpaperOption?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val wallpaperOptions = listOf(
        WallpaperOption(
            id = "dark_minimal",
            label = "Dark Minimal",
            isDark = true,
            primaryColor = Color(0xFF111111),
            accentColor = Color(0xFF333333)
        ),
        WallpaperOption(
            id = "light_minimal",
            label = "Light Minimal",
            isDark = false,
            primaryColor = Color(0xFFF5F5F5),
            accentColor = Color(0xFFDDDDDD)
        )
    )

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wallpapers",
                        style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FocusTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FocusTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Tooltip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FocusTheme.colors.surface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Minimalist Home Screen",
                        style = FocusTheme.typography.label.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        "Set one of these wallpapers as your home screen background for a clean, distraction-free look.",
                        style = FocusTheme.typography.caption.copy(
                            color = FocusTheme.colors.secondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Wallpaper options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                wallpaperOptions.forEach { option ->
                    val isSelected = selectedWallpaper?.id == option.id
                    WallpaperCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = { selectedWallpaper = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selectedWallpaper != null) FocusTheme.colors.primary
                        else FocusTheme.colors.surface
                    )
                    .clickable(enabled = selectedWallpaper != null && !isSaving) {
                        scope.launch {
                            val option = selectedWallpaper ?: return@launch
                            isSaving = true
                            withContext(Dispatchers.IO) {
                                val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(option.primaryColor.toArgb())
                                WallpaperManager.getInstance(context).setBitmap(
                                    bitmap, null, true,
                                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                )
                                bitmap.recycle()
                            }
                            val theme = if (option.isDark) WidgetTheme.DARK else WidgetTheme.LIGHT
                            val awm = AppWidgetManager.getInstance(context)
                            (awm.getAppWidgetIds(ComponentName(context, TaskListWidgetReceiver::class.java)) +
                             awm.getAppWidgetIds(ComponentName(context, ClockDateWidgetReceiver::class.java)))
                                .forEach { id -> WidgetThemeStore.save(context, id, theme) }
                            WidgetUpdater.updateAll(context)
                            isSaving = false
                            onNavigateBack()
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isSaving) "Saving…" else "Save",
                    style = FocusTheme.typography.label.copy(
                        color = if (selectedWallpaper != null) FocusTheme.colors.background
                        else FocusTheme.colors.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class WallpaperOption(
    val id: String,
    val label: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val accentColor: Color
)

@Composable
private fun WallpaperCard(
    option: WallpaperOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(option.primaryColor)
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) FocusTheme.colors.primary else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Simulated wallpaper preview
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(option.accentColor, RoundedCornerShape(1.dp))
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(FocusTheme.colors.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = FocusTheme.colors.background,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            option.label,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 12.sp
            )
        )
    }
}
