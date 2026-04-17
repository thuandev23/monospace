package com.monospace.app.core.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.monospace.app.MainActivity
import com.monospace.app.MonospaceApp
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.repository.FocusProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockingService : Service() {

    @Inject
    lateinit var focusProfileRepository: FocusProfileRepository

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeProfile: FocusProfile? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        AppBlockingState.setBlockedPackage(null)
        super.onDestroy()
    }

    private fun startMonitoring() {
        scope.launch {
            focusProfileRepository.observeActive().collectLatest { profile ->
                activeProfile = profile
            }
        }

        scope.launch {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@launch
            while (true) {
                checkAndBlock(usm)
                delay(500L)
            }
        }
    }

    private fun checkAndBlock(usm: UsageStatsManager) {
        val profile = activeProfile
        if (profile == null || profile.allowedAppIds.isEmpty()) {
            AppBlockingState.setBlockedPackage(null)
            return
        }

        if (AppBlockingState.isTemporarilyUnlocked()) {
            AppBlockingState.setBlockedPackage(null)
            return
        }

        val topApp = getForegroundApp(usm) ?: return
        if (topApp == packageName || topApp in profile.allowedAppIds) {
            AppBlockingState.setBlockedPackage(null)
            return
        }

        AppBlockingState.setBlockedPackage(topApp)
        bringToFront()
    }

    private fun getForegroundApp(usm: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5_000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun bringToFront() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, MonospaceApp.CHANNEL_FOCUS)
            .setContentTitle("Focus đang hoạt động")
            .setContentText("Đang chặn ứng dụng bị hạn chế")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AppBlockingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppBlockingService::class.java))
        }
    }
}
