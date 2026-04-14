package com.monospace.app.core.network.dto

import com.monospace.app.core.database.entity.TaskEntity

fun TaskEntity.toDto(): TaskDto = TaskDto(
    id = this.id,
    title = this.title,
    notes = this.notes,
    taskStatus = this.taskStatus,
    listId = this.listId,
    priority = this.priority,
    startDateTime = this.startDateTime,
    endDateTime = this.endDateTime,
    isAllDay = this.isAllDay,
    reminderValue = this.reminderValue,
    reminderUnit = this.reminderUnit,
    reminderTime = this.reminderTime,
    repeatInterval = this.repeatInterval,
    repeatUnit = this.repeatUnit,
    repeatDaysOfWeek = this.repeatDaysOfWeek,
    updatedAt = this.updatedAt
)
