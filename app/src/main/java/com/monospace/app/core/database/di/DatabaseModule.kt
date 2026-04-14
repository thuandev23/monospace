package com.monospace.app.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monospace.app.R
import com.monospace.app.core.database.DatabaseMigrations
import com.monospace.app.core.database.FocusDatabase
import com.monospace.app.core.database.dao.FocusProfileDao
import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.database.dao.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "monospace_focus_db"

    @Provides
    @Singleton
    fun provideFocusDatabase(@ApplicationContext context: Context): FocusDatabase {
        return Room.databaseBuilder(
            context,
            FocusDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val defaultListName = context.getString(R.string.default_task_list_name)
                    db.execSQL(
                        "INSERT INTO lists (id, name, sync_status, created_at, updated_at) VALUES ('default', ?, 'synced', 0, 0)",
                        arrayOf(defaultListName)
                    )
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: FocusDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideTaskListDao(database: FocusDatabase): TaskListDao = database.taskListDao()

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: FocusDatabase): SyncQueueDao = database.syncQueueDao()

    @Provides
    @Singleton
    fun provideFocusProfileDao(database: FocusDatabase): FocusProfileDao = database.focusProfileDao()
}
