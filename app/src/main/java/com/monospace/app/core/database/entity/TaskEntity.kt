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
    val priority: Int = 0,
    @ColumnInfo(name = "parent_task_id") val parentTaskId: String? = null,
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending_create",
    
    // Scheduled Time (Stored as Long epoch millis)
    @ColumnInfo(name = "start_date_time") val startDateTime: Long? = null,
    @ColumnInfo(name = "end_date_time") val endDateTime: Long? = null,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = true,
    
    // Flattened Reminder Config
    @ColumnInfo(name = "reminder_value") val reminderValue: Int? = null,
    @ColumnInfo(name = "reminder_unit") val reminderUnit: String? = null,
    @ColumnInfo(name = "reminder_time") val reminderTime: String? = null, // "HH:mm"
    
    // Flattened Repeat Config
    @ColumnInfo(name = "repeat_interval") val repeatInterval: Int? = null,
    @ColumnInfo(name = "repeat_unit") val repeatUnit: String? = null,
    @ColumnInfo(name = "repeat_days_of_week") val repeatDaysOfWeek: String? = null, // "1,2,3"
    
    val checksum: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null
)
