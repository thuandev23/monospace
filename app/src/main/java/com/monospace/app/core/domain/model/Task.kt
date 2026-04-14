package com.monospace.app.core.domain.model

import java.time.Instant
import java.time.LocalTime

data class Task(
    val id: String,
    val title: String,
    val notes: String? = null,
    val status: TaskStatus = TaskStatus.NOT_DONE,
    val listId: String = "default",
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val priority: Priority = Priority.NONE,

    // Schedule Data (Timezone aware via Instant)
    val startDateTime: Instant?,
    val endDateTime: Instant?,
    val isAllDay: Boolean = true,

    // Reminder Logic
    val reminder: ReminderConfig? = null,

    // Repeat Logic
    val repeat: RepeatConfig? = null
)

data class ReminderConfig(
    val value: Int,
    val unit: ReminderUnit,
    val remindTime: LocalTime
)

enum class ReminderUnit { MINUTE, HOUR, DAY, WEEK }

data class RepeatConfig(
    val interval: Int,
    val unit: RepeatUnit,
    val daysOfWeek: Set<Int>? = null // 1 (Mon) to 7 (Sun)
)

enum class RepeatUnit { DAY, WEEK, MONTH, YEAR }

enum class Priority(val value: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3)
}

enum class TaskStatus {
    NOT_DONE, IN_PROGRESS, CANCELLED, DONE
}

enum class SyncStatus {
    SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE
}
