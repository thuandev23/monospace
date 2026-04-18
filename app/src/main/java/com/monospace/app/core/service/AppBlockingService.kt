package com.monospace.app.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.monospace.app.MainActivity
import com.monospace.app.MonospaceApp
import com.monospace.app.R
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.repository.FocusProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    
    private var countdownJob: Job? = null
    private var lastBlockedApp: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BLOCK_DEBUG", "AppBlockingService: onStartCommand")
        startForeground(NOTIFICATION_ID, buildBaseNotification())
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("BLOCK_DEBUG", "AppBlockingService: onDestroy")
        scope.cancel()
        countdownJob?.cancel()
        AppBlockingState.setBlockedPackage(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startMonitoring() {
        scope.launch {
            focusProfileRepository.observeActive().collectLatest { profile ->
                activeProfile = profile
            }
        }

        scope.launch {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@launch
            while (true) {
                checkAndBlock(usm)
                delay(1_000L)
            }
        }
    }

    private fun checkAndBlock(usm: UsageStatsManager) {
        val profile = activeProfile ?: return
        if (profile.allowedAppIds.isEmpty() || AppBlockingState.isTemporarilyUnlocked()) {
            resetBlockingState()
            return
        }

        val topApp = getForegroundApp(usm) ?: return
        
        // 1. Nếu người dùng quay lại Monospace tự giác
        if (topApp == packageName) {
            if (countdownJob?.isActive == true) {
                Log.d("BLOCK_DEBUG", "User returned to Monospace, stopping countdown")
                resetBlockingState()
            }
            return
        }

        // 2. Nếu app hiện tại nằm trong danh sách chặn
        if (topApp in profile.allowedAppIds) {
            if (lastBlockedApp != topApp) {
                lastBlockedApp = topApp
                startCountdown(topApp)
            }
        } else {
            // 3. Nếu là app an toàn (Launcher...)
            resetBlockingState()
        }
    }

    private fun startCountdown(blockedPackage: String) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            for (i in 5 downTo 1) {
                Log.d("BLOCK_DEBUG", "Countdown: $i s for $blockedPackage")
                // Cancel trước để force Android hiện banner mới (Heads-up) thay vì update im lặng
                nm.cancel(COUNTDOWN_NOTIFICATION_ID)
                delay(80L)
                nm.notify(COUNTDOWN_NOTIFICATION_ID, buildCountdownNotification(i))
                delay(920L)
            }

            // HẾT 5 GIÂY: dọn countdown notification rồi nhảy về overlay
            Log.d("BLOCK_DEBUG", "Countdown finished -> INITIATING AUTO-JUMP to overlay")
            nm.cancel(COUNTDOWN_NOTIFICATION_ID)
            AppBlockingState.setBlockedPackage(blockedPackage)
            bringToFront()
            countdownJob = null
        }
    }

    private fun resetBlockingState() {
        if (countdownJob != null || lastBlockedApp != null) {
            Log.d("BLOCK_DEBUG", "Resetting blocking/countdown state")
            countdownJob?.cancel()
            countdownJob = null
            lastBlockedApp = null

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(COUNTDOWN_NOTIFICATION_ID)

            if (AppBlockingState.blockedPackage.value != null) {
                AppBlockingState.setBlockedPackage(null)
            }
        }
    }

    private fun getForegroundApp(usm: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun bringToFront() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        
        val canOverlay = Settings.canDrawOverlays(this)
        Log.d("BLOCK_DEBUG", "bringToFront: Executing jump. CanDrawOverlays=$canOverlay")

        try {
            // Mở trực tiếp Activity
            startActivity(intent)
            
            // Ép hệ thống ưu tiên mở qua PendingIntent
            val pi = PendingIntent.getActivity(this, 1005, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            pi.send()
        } catch (e: Exception) {
            Log.e("BLOCK_DEBUG", "bringToFront: Jump failed", e)
        }

        // Thông báo dự phòng mức cao nhất (Full Screen Intent)
        val piFull = PendingIntent.getActivity(this, 1002, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, MonospaceApp.CHANNEL_FOCUS)
            .setContentTitle(getString(R.string.notif_app_restricted_title))
            .setContentText("Thời gian đã hết! Vui lòng tập trung.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setFullScreenIntent(piFull, true)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildCountdownNotification(secondsLeft: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return Notification.Builder(this, MonospaceApp.CHANNEL_FOCUS)
            .setContentTitle(getString(R.string.notif_app_restricted_title))
            .setContentText(getString(R.string.notif_app_restricted_desc, secondsLeft))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setOnlyAlertOnce(false) // Cho phép nảy banner lại nếu cần
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(pi, true) 
            .build()
    }

    private fun buildBaseNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, MonospaceApp.CHANNEL_FOCUS)
            .setContentTitle(getString(R.string.notif_focus_active_title))
            .setContentText(getString(R.string.notif_focus_active_desc))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val COUNTDOWN_NOTIFICATION_ID = 1003

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AppBlockingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppBlockingService::class.java))
        }
    }
}
