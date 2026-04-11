package com.monospace.app.core.database.di

import android.content.Context
import androidx.room.Room
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
        ).build()
    }

    @Provides
    fun provideTaskDao(database: FocusDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideSyncQueueDao(database: FocusDatabase): SyncQueueDao = database.syncQueueDao()
}