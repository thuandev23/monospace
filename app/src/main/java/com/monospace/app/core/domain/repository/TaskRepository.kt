package com.monospace.app.core.domain.repository


import com.monospace.app.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(listId: String): Flow<List<Task>>
    fun observeAllTasksSortedByDate(): Flow<List<Task>>
    fun observeTodayTasks(): Flow<List<Task>>
    fun observeAllActiveTaskCount(): Flow<Int>
    fun observeTodayTaskCount(): Flow<Int>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun saveTask(task: Task)
    suspend fun markTaskCompleted(taskId: String, isCompleted: Boolean)
    suspend fun deleteTask(taskId: String)
    suspend fun mergeRemoteTasks(remoteTasks: List<Task>)
    suspend fun getPendingTasks(): List<Task>
    suspend fun hardDeleteTask(taskId: String)
}