package com.monospace.app.core.domain.model

import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val notes: String?,
    val isCompleted: Boolean,
    val dueDate: LocalDate?,
    val priority: Priority,
    val listId: String,
    val syncStatus: SyncStatus
)

enum class Priority(val value: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3)
}

enum class SyncStatus {
    SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE
}