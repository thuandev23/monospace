package com.monospace.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("list_id"),
        Index("sync_status"),
        Index("external_id")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "external_id") val externalId: String? = null,
    @ColumnInfo(name = "external_source") val externalSource: String? = null,
    val title: String,
    val notes: String? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    val priority: Int = 0,
    @ColumnInfo(name = "parent_task_id") val parentTaskId: String? = null,
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "synced",
    val checksum: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null
)