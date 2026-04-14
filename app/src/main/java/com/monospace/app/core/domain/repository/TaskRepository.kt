package com.monospace.app.core.domain.repository


import com.monospace.app.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(listId: String): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun saveTask(task: Task)
    suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean)
    suspend fun deleteTask(taskId: String)
    suspend fun mergeRemoteTasks(remoteTasks: List<Task>)
}