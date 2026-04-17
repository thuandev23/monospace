package com.monospace.app.core.data.repository

import android.content.Context
import com.monospace.app.core.data.mapper.toDomain
import com.monospace.app.core.data.mapper.toEntity
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    @ApplicationContext private val context: Context
) : TaskRepository {

    override fun observeTasks(listId: String): Flow<List<Task>> {
        return taskDao.observeTasksByList(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAllTasksSortedByDate(): Flow<List<Task>> {
        return taskDao.observeAllTasksSortedByDate().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTodayTasks(): Flow<List<Task>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return taskDao.observeTodayTasks(dayStart, dayEnd).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAllActiveTaskCount(): Flow<Int> =
        taskDao.observeAllActiveTaskCount()

    override fun observeTodayTaskCount(): Flow<Int> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return taskDao.observeTodayTaskCount(dayStart, dayEnd)
    }

    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun saveTask(task: Task) {
        taskDao.upsert(task.toEntity())
        WidgetUpdater.updateAll(context)
    }

    override suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean) {
        val status = if (isCompleted) "DONE" else "NOT_DONE"
        taskDao.updateTaskStatus(taskId, status)
        WidgetUpdater.updateAll(context)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.markAsDeleted(taskId)
        WidgetUpdater.updateAll(context)
    }

    override suspend fun mergeRemoteTasks(remoteTasks: List<Task>) {
        for (remote in remoteTasks) {
            val local = taskDao.getTaskById(remote.id)
            when {
                // Task chưa có local → insert thẳng từ server
                local == null -> taskDao.upsert(remote.toEntity().copy(syncStatus = "synced"))

                // Local đang có pending changes → giữ local, không ghi đè
                local.syncStatus != "synced" -> continue

                // Server mới hơn local → dùng server version
                remote.toEntity().updatedAt > local.updatedAt ->
                    taskDao.upsert(remote.toEntity().copy(syncStatus = "synced"))

                // Local mới hơn hoặc bằng → giữ local
                else -> continue
            }
        }
    }
}