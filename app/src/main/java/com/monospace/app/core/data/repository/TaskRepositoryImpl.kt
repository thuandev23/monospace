package com.monospace.app.core.data.repository

import com.monospace.app.core.data.mapper.toDomain
import com.monospace.app.core.data.mapper.toEntity
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
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

    override fun observeAllActiveTaskCount(): Flow<Int> =
        taskDao.observeAllActiveTaskCount()

    override fun observeTodayTaskCount(): Flow<Int> {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val dayEnd = cal.timeInMillis
        return taskDao.observeTodayTaskCount(dayStart, dayEnd)
    }

    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun saveTask(task: Task) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean) {
        val status = if (isCompleted) "DONE" else "NOT_DONE"
        taskDao.updateTaskStatus(taskId, status)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.markAsDeleted(taskId)
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