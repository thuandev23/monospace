package com.monospace.app.core.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.sync.ReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

class ExpandRepeatTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue,
    private val reminderScheduler: ReminderScheduler
) {
    /**
     * Khi một task có RepeatConfig được đánh dấu hoàn thành,
     * tạo instance tiếp theo với thời gian được tính theo lịch lặp.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(completedTask: Task) {
        val repeat = completedTask.repeat ?: return
        val startDateTime = completedTask.startDateTime ?: return

        val zone = ZoneId.systemDefault()
        val startZdt = startDateTime.atZone(zone)

        val nextStart: ZonedDateTime = when (repeat.unit) {
            RepeatUnit.DAY   -> findNextDay(startZdt, repeat.interval)
            RepeatUnit.WEEK  -> findNextWeek(startZdt, repeat.interval, repeat.daysOfWeek)
            RepeatUnit.MONTH -> startZdt.plusMonths(repeat.interval.toLong())
            RepeatUnit.YEAR  -> startZdt.plusYears(repeat.interval.toLong())
        }

        // Giữ nguyên duration nếu có endDateTime
        val nextEnd: Instant? = completedTask.endDateTime?.let {
            val durationMs = it.toEpochMilli() - startDateTime.toEpochMilli()
            nextStart.toInstant().plusMillis(durationMs)
        }

        val nextTask = completedTask.copy(
            id = UUID.randomUUID().toString(),
            isCompleted = false,
            startDateTime = nextStart.toInstant(),
            endDateTime = nextEnd,
            syncStatus = SyncStatus.PENDING_CREATE
        )

        repository.saveTask(nextTask)
        syncQueue.enqueue(nextTask.id, SyncOperationType.CREATE, nextTask.id)
        reminderScheduler.scheduleReminder(nextTask)
    }

    private fun findNextDay(from: ZonedDateTime, interval: Int): ZonedDateTime =
        from.plusDays(interval.toLong())

    private fun findNextWeek(
        from: ZonedDateTime,
        interval: Int,
        daysOfWeek: Set<Int>?
    ): ZonedDateTime {
        if (daysOfWeek.isNullOrEmpty()) return from.plusWeeks(interval.toLong())

        // Tìm ngày tiếp theo trong tuần hiện tại hoặc tuần sau
        val currentDayValue = from.dayOfWeek.value // 1=Mon ... 7=Sun
        val sortedDays = daysOfWeek.sorted()

        // Ngày tiếp theo trong cùng tuần
        val nextInWeek = sortedDays.firstOrNull { it > currentDayValue }
        return if (nextInWeek != null) {
            from.plusDays((nextInWeek - currentDayValue).toLong())
        } else {
            // Sang tuần sau, lấy ngày đầu tiên trong daysOfWeek
            val firstDay = sortedDays.first()
            from.plusWeeks(interval.toLong())
                .plusDays((firstDay - currentDayValue).toLong())
        }
    }
}
