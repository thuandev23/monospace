package com.monospace.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.monospace.app.core.database.entity.FocusProfileEntity
import com.monospace.app.core.database.entity.SyncQueueEntity
import com.monospace.app.core.database.entity.TaskEntity
import com.monospace.app.core.database.entity.TaskListEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskListEntity::class,
        SyncQueueEntity::class,
        FocusProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FocusDatabase : RoomDatabase() {
    // Tạm thời để trống
}