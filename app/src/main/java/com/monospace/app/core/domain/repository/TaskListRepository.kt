package com.monospace.app.core.domain.repository

import com.monospace.app.core.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    fun observeAllLists(): Flow<List<TaskList>>
    suspend fun saveList(list: TaskList)
    suspend fun deleteList(id: String)
    suspend fun updateSortOrder(id: String, order: Int)
}
