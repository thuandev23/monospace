package com.monospace.app.feature.settings

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "wallpaper_daily_update"
    }

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WallpaperUpdateWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextSixAm(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun millisUntilNextSixAm(): Long {
        val now = LocalDateTime.now()
        val today6am = now.toLocalDate().atTime(6, 0)
        val next6am = if (now.isBefore(today6am)) today6am else today6am.plusDays(1)
        return ChronoUnit.MILLIS.between(now, next6am)
    }
}
