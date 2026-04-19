package com.monospace.app.feature.wallpaper

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.AppShortcut
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.model.WallpaperConfig
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.feature.settings.HitMap
import com.monospace.app.feature.settings.WallpaperRenderer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LiveEngine()

    inner class LiveEngine : Engine() {

        private lateinit var taskRepository: TaskRepository
        private lateinit var settingsDataStore: SettingsDataStore

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private var currentConfig = WallpaperConfig()
        private var currentTasks: List<Task> = emptyList()
        private var currentShortcuts: List<AppShortcut> = emptyList()
        private var currentTime = LocalDateTime.now()
        private var hitMap = HitMap(emptyList(), emptyList())
        private var isVisible = false

        private val iconCache = mutableMapOf<String, Bitmap>()
        private val iconScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var clockJob: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            val ep = EntryPointAccessors.fromApplication(
                applicationContext, LiveWallpaperEntryPoint::class.java
            )
            taskRepository = ep.taskRepository()
            settingsDataStore = ep.settingsDataStore()

            scope.launch {
                combine(
                    taskRepository.observeTodayTasks(),
                    settingsDataStore.wallpaperConfig,
                    settingsDataStore.launcherShortcuts
                ) { tasks, config, shortcuts -> Triple(tasks, config, shortcuts) }
                    .collect { (tasks, config, shortcuts) ->
                        currentTasks = tasks.filter { it.status != TaskStatus.DONE }
                        currentConfig = config
                        val newPkgs = shortcuts.map { it.packageName }.toSet()
                        currentShortcuts = shortcuts
                        preloadIcons(newPkgs)
                        if (isVisible) drawFrame()
                    }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                currentTime = LocalDateTime.now()
                drawFrame()
                startClockTick()
            } else {
                stopClockTick()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (isVisible) drawFrame()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action != MotionEvent.ACTION_UP) return
            val x = event.x
            val y = event.y

            hitMap.taskHits.firstOrNull { y in it.topY..it.bottomY }?.let {
                openMonospace()
                return
            }

            hitMap.shortcutHits.firstOrNull { x in it.rect.left..it.rect.right && y in it.rect.top..it.rect.bottom }?.let {
                launchApp(it.packageName)
            }
        }

        override fun onDestroy() {
            scope.cancel()
            iconScope.cancel()
            super.onDestroy()
        }

        private fun startClockTick() {
            clockJob?.cancel()
            clockJob = scope.launch {
                val secondsUntilNextMinute = 60 - LocalDateTime.now().second
                delay(secondsUntilNextMinute * 1000L)
                while (isActive) {
                    currentTime = LocalDateTime.now()
                    if (isVisible) drawFrame()
                    delay(60_000L)
                }
            }
        }

        private fun stopClockTick() {
            clockJob?.cancel()
            clockJob = null
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            } ?: return
            try {
                hitMap = WallpaperRenderer.renderToCanvas(
                    canvas = canvas,
                    context = applicationContext,
                    config = currentConfig,
                    tasks = currentTasks,
                    shortcuts = currentShortcuts,
                    iconCache = iconCache,
                    width = canvas.width,
                    height = canvas.height,
                    now = currentTime
                )
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun preloadIcons(packages: Set<String>) {
            val missing = packages.filter { it !in iconCache }
            if (missing.isEmpty()) return
            iconScope.launch {
                missing.forEach { pkg ->
                    val bmp = WallpaperRenderer.rasterizeIcon(applicationContext, pkg) ?: return@forEach
                    withContext(Dispatchers.Main.immediate) {
                        iconCache[pkg] = bmp
                        if (isVisible) drawFrame()
                    }
                }
            }
        }

        private fun openMonospace() {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            intent?.let { startActivity(it) }
        }

        private fun launchApp(pkg: String) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            intent?.let { startActivity(it) }
        }
    }
}
