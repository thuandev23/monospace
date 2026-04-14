package com.monospace.app.core.sync

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Schedule reminder cho một task.
     * Tự tính thời điểm thông báo = startDateTime - reminderOffset.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleReminder(task: Task) {
        val reminder = task.reminder ?: return
        val start = task.startDateTime ?: return

        val reminderInstant = calculateReminderInstant(start, reminder) ?: return
        val delayMs = reminderInstant.toEpochMilli() - System.currentTimeMillis()
        if (delayMs <= 0) return // Đã qua giờ nhắc

        val inputData: Data = workDataOf(
            ReminderWorker.KEY_TASK_ID to task.id,
            ReminderWorker.KEY_TASK_TITLE to task.title
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_${task.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${task.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Hủy reminder của một task (khi task bị xóa hoặc reminder bị tắt).
     */
    fun cancelReminder(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$taskId")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateReminderInstant(start: Instant, reminder: ReminderConfig): Instant? {
        // Kết hợp ngày từ startDateTime + giờ từ remindTime
        val zone = ZoneId.systemDefault()
        val startZdt: ZonedDateTime = start.atZone(zone)
        val remindZdt: ZonedDateTime = startZdt
            .withHour(reminder.remindTime.hour)
            .withMinute(reminder.remindTime.minute)
            .withSecond(0)
            .withNano(0)

        // Trừ đi offset reminder (ví dụ: 1 DAY trước)
        return when (reminder.unit) {
            ReminderUnit.MINUTE -> remindZdt.minusMinutes(reminder.value.toLong()).toInstant()
            ReminderUnit.HOUR   -> remindZdt.minusHours(reminder.value.toLong()).toInstant()
            ReminderUnit.DAY    -> remindZdt.minusDays(reminder.value.toLong()).toInstant()
            ReminderUnit.WEEK   -> remindZdt.minusWeeks(reminder.value.toLong()).toInstant()
        }
    }
}
