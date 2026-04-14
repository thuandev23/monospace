package com.monospace.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.monospace.app.core.database.dao.FocusProfileDao
import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.database.dao.TaskListDao
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
    version = 2,
    exportSchema = true
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun focusProfileDao(): FocusProfileDao
}
