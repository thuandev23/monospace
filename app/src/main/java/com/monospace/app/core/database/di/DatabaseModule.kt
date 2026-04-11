package com.monospace.app.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monospace.app.core.database.FocusDatabase
import com.monospace.app.core.database.dao.SyncQueueDao
import com.monospace.app.core.database.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFocusDatabase(@ApplicationContext context: Context): FocusDatabase {
        return Room.databaseBuilder(
            context,
            FocusDatabase::class.java,
            "monospace.db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Chèn list mặc định khi DB được tạo lần đầu
                    db.execSQL("INSERT INTO lists (id, name, sync_status, created_at, updated_at) VALUES ('default', 'My Tasks', 'synced', 0, 0)")
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTaskDao(database: FocusDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideSyncQueueDao(database: FocusDatabase): SyncQueueDao = database.syncQueueDao()
}