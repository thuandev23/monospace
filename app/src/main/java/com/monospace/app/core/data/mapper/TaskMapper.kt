package com.monospace.app.core.data.mapper


import android.os.Build
import androidx.annotation.RequiresApi
import com.monospace.app.core.database.entity.TaskEntity
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import java.time.Instant
import java.time.ZoneId

// Chuyển từ Room Entity sang Domain Model
@RequiresApi(Build.VERSION_CODES.O)
fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        title = this.title,
        notes = this.notes,
        isCompleted = this.isCompleted,
        dueDate = this.dueDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        },
        priority = Priority.values().firstOrNull { it.value == this.priority } ?: Priority.NONE,
        listId = this.listId,
        syncStatus = when (this.syncStatus) {
            "synced" -> SyncStatus.SYNCED
            "pending_create" -> SyncStatus.PENDING_CREATE
            "pending_update" -> SyncStatus.PENDING_UPDATE
            "pending_delete" -> SyncStatus.PENDING_DELETE
            else -> SyncStatus.SYNCED
        }
    )
}

// Chuyển từ Domain Model sang Room Entity
@RequiresApi(Build.VERSION_CODES.O)
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        title = this.title,
        notes = this.notes,
        isCompleted = this.isCompleted,
        dueDate = this.dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        priority = this.priority.value,
        listId = this.listId,
        syncStatus = when (this.syncStatus) {
            SyncStatus.SYNCED -> "synced"
            SyncStatus.PENDING_CREATE -> "pending_create"
            SyncStatus.PENDING_UPDATE -> "pending_update"
            SyncStatus.PENDING_DELETE -> "pending_delete"
        }
    )
}