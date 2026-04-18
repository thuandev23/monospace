package com.monospace.app.feature.settings

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.model.WallpaperConfig
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.widget.ClockDateWidgetReceiver
import com.monospace.app.widget.TaskListWidgetReceiver
import com.monospace.app.widget.WidgetTheme
import com.monospace.app.widget.WidgetThemeStore
import com.monospace.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val wallpaperScheduler: WallpaperScheduler,
    getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    val config: StateFlow<WallpaperConfig> = settingsDataStore.wallpaperConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WallpaperConfig())

    val todayTasks: StateFlow<List<Task>> = getTasksUseCase("today")
        .map { tasks -> tasks.filter { it.status != TaskStatus.DONE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _applyResult = MutableSharedFlow<Boolean>()
    val applyResult: SharedFlow<Boolean> = _applyResult.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.wallpaperConfig.collect { cfg ->
                if (cfg.autoUpdate) wallpaperScheduler.schedule() else wallpaperScheduler.cancel()
            }
        }
    }

    fun updateConfig(config: WallpaperConfig) {
        viewModelScope.launch { settingsDataStore.setWallpaperConfig(config) }
    }

    fun applyWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = runCatching {
                val (w, h) = screenDimensions()
                val bitmap = WallpaperRenderer.render(
                    context = context,
                    config = config.value,
                    tasks = todayTasks.value,
                    width = w,
                    height = h
                )
                val wm = WallpaperManager.getInstance(context)
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                bitmap.recycle()
            }.isSuccess

            // Widget sync is best-effort — failure here must not mark wallpaper as failed
            runCatching {
                val isDark = isColorDark(config.value.backgroundColorHex)
                val theme = if (isDark) WidgetTheme.DARK else WidgetTheme.LIGHT
                val awm = AppWidgetManager.getInstance(context)
                val ids = awm.getAppWidgetIds(ComponentName(context, TaskListWidgetReceiver::class.java)) +
                          awm.getAppWidgetIds(ComponentName(context, ClockDateWidgetReceiver::class.java))
                ids.forEach { id -> WidgetThemeStore.save(context, id, theme) }
                WidgetUpdater.updateAll(context)
            }
            _applyResult.emit(success)
        }
    }

    private fun screenDimensions(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = wm.currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val dm = context.resources.displayMetrics
        return dm.widthPixels to dm.heightPixels
    }

    private fun isColorDark(hex: String): Boolean {
        val color = runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(android.graphics.Color.BLACK)
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.5
    }
}
