package com.monospace.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "focus_profiles")
data class FocusProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(name = "allowed_app_ids") val allowedAppIds: String,
    @ColumnInfo(name = "linked_list_id") val linkedListId: String? = null,
    @ColumnInfo(name = "schedule_json") val scheduleJson: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false
)