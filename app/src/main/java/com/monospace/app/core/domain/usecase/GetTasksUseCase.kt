package com.monospace.app.core.domain.usecase


import com.monospace.app.core.domain.model.ListIds
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(listId: String): Flow<List<Task>> {
        return when (listId) {
            ListIds.ALL -> repository.observeAllTasksSortedByDate()
            ListIds.TODAY -> repository.observeTodayTasks()
            else -> repository.observeTasks(listId)
        }
    }
}