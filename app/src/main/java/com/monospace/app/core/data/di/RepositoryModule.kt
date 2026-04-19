package com.monospace.app.core.data.di

import com.monospace.app.core.data.repository.AppRepositoryImpl
import com.monospace.app.core.data.repository.FocusProfileRepositoryImpl
import com.monospace.app.core.data.repository.FocusSessionRepositoryImpl
import com.monospace.app.core.data.repository.SyncQueueImpl
import com.monospace.app.core.data.repository.TaskListRepositoryImpl
import com.monospace.app.core.data.repository.TaskRepositoryImpl
import com.monospace.app.core.domain.repository.AppRepository
import com.monospace.app.core.domain.repository.FocusProfileRepository
import com.monospace.app.core.domain.repository.FocusSessionRepository
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.widget.WidgetRefresher
import com.monospace.app.widget.WidgetRefresherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskListRepository(
        taskListRepositoryImpl: TaskListRepositoryImpl
    ): TaskListRepository

    @Binds
    @Singleton
    abstract fun bindSyncQueue(
        syncQueueImpl: SyncQueueImpl
    ): SyncQueue

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindFocusProfileRepository(
        focusProfileRepositoryImpl: FocusProfileRepositoryImpl
    ): FocusProfileRepository

    @Binds
    @Singleton
    abstract fun bindFocusSessionRepository(
        focusSessionRepositoryImpl: FocusSessionRepositoryImpl
    ): FocusSessionRepository

    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(
        widgetRefresherImpl: WidgetRefresherImpl
    ): WidgetRefresher
}
