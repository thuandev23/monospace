package com.monospace.app.core.domain.usecase


import com.monospace.app.core.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, currentStatus: Boolean) {
        repository.markTaskCompleted(taskId, !currentStatus)
    }
}