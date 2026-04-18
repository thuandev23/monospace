package com.monospace.app.feature.settings

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.widget.ClockDateWidgetReceiver
import com.monospace.app.widget.TaskListWidgetReceiver
import com.monospace.app.widget.WidgetTheme
import com.monospace.app.widget.WidgetThemeStore
import com.monospace.app.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WallpaperUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = settingsDataStore.wallpaperConfig.first()
        if (!config.autoUpdate) return Result.success()

        val tasks = taskRepository.observeTodayTasks()
            .first()
            .filter { it.status != TaskStatus.DONE }

        val (w, h) = screenDimensions()
        val wm = WallpaperManager.getInstance(context)

        val bitmap = WallpaperRenderer.render(
            context = context,
            config = config,
            tasks = tasks,
            width = w,
            height = h
        )

        runCatching {
            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }.onFailure { return Result.retry() }

        bitmap.recycle()

        val isDark = isColorDark(config.backgroundColorHex)
        val theme = if (isDark) WidgetTheme.DARK else WidgetTheme.LIGHT
        val awm = AppWidgetManager.getInstance(context)
        val ids = awm.getAppWidgetIds(ComponentName(context, TaskListWidgetReceiver::class.java)) +
                  awm.getAppWidgetIds(ComponentName(context, ClockDateWidgetReceiver::class.java))
        ids.forEach { id -> WidgetThemeStore.save(context, id, theme) }
        WidgetUpdater.updateAll(context)

        return Result.success()
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
        val color = runCatching { android.graphics.Color.parseColor(hex) }
            .getOrDefault(android.graphics.Color.BLACK)
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.5
    }
}
