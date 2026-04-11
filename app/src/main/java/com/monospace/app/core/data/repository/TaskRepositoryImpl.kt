package com.monospace.app.core.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun observeTasks(listId: String): Flow<List<Task>> {
        return taskDao.observeTasksByList(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun saveTask(task: Task) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean) {
        taskDao.updateCompletionStatus(taskId, isCompleted)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.markAsDeleted(taskId)
    }
}