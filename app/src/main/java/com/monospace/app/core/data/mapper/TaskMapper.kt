package com.monospace.app.core.data.mapper

import com.monospace.app.core.database.entity.TaskEntity
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalTime

// Chuyển từ Room Entity sang Domain Model
fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        title = this.title,
        notes = this.notes,
        status = TaskStatus.entries.firstOrNull { it.name == this.taskStatus } ?: TaskStatus.NOT_DONE,
        priority = Priority.entries.firstOrNull { it.value == this.priority } ?: Priority.NONE,
        listId = this.listId,
        syncStatus = when (this.syncStatus) {
            "synced" -> SyncStatus.SYNCED
            "pending_create" -> SyncStatus.PENDING_CREATE
            "pending_update" -> SyncStatus.PENDING_UPDATE
            "pending_delete" -> SyncStatus.PENDING_DELETE
            else -> SyncStatus.SYNCED
        },
        externalSource = this.externalSource,
        startDateTime = this.startDateTime?.let { Instant.ofEpochMilli(it) },
        endDateTime = this.endDateTime?.let { Instant.ofEpochMilli(it) },
        isAllDay = this.isAllDay,
        reminder = if (this.reminderValue != null && this.reminderUnit != null && this.reminderTime != null) {
            ReminderConfig(
                value = this.reminderValue,
                unit = ReminderUnit.valueOf(this.reminderUnit),
                remindTime = LocalTime.parse(this.reminderTime)
            )
        } else null,
        repeat = if (this.repeatInterval != null && this.repeatUnit != null) {
            RepeatConfig(
                interval = this.repeatInterval,
                unit = RepeatUnit.valueOf(this.repeatUnit),
                daysOfWeek = this.repeatDaysOfWeek?.split(",")?.map { it.toInt() }?.toSet()
            )
        } else null,
        createdAt = Instant.ofEpochMilli(this.createdAt)
    )
}

// Chuyển từ Domain Model sang Room Entity
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        title = this.title,
        notes = this.notes,
        taskStatus = this.status.name,
        priority = this.priority.value,
        listId = this.listId,
        syncStatus = when (this.syncStatus) {
            SyncStatus.SYNCED -> "synced"
            SyncStatus.PENDING_CREATE -> "pending_create"
            SyncStatus.PENDING_UPDATE -> "pending_update"
            SyncStatus.PENDING_DELETE -> "pending_delete"
        },
        externalSource = this.externalSource,
        startDateTime = this.startDateTime?.toEpochMilli(),
        endDateTime = this.endDateTime?.toEpochMilli(),
        isAllDay = this.isAllDay,
        reminderValue = this.reminder?.value,
        reminderUnit = this.reminder?.unit?.name,
        reminderTime = this.reminder?.remindTime?.toString(),
        repeatInterval = this.repeat?.interval,
        repeatUnit = this.repeat?.unit?.name,
        repeatDaysOfWeek = this.repeat?.daysOfWeek?.joinToString(","),
        createdAt = this.createdAt.toEpochMilli()
    )
}
