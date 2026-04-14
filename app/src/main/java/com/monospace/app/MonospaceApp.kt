package com.monospace.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.monospace.app.core.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MonospaceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        syncScheduler.schedulePeriodicSync()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Channel cho task reminders
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Nhắc nhở task",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở các task đến hạn"
                enableVibration(true)
            }

            // Channel cho sync status (ít ưu tiên hơn)
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Đồng bộ dữ liệu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Trạng thái đồng bộ với server"
            }

            manager.createNotificationChannels(listOf(reminderChannel, syncChannel))
        }
    }

    companion object {
        const val CHANNEL_REMINDER = "monospace_reminder"
        const val CHANNEL_SYNC = "monospace_sync"
    }
}