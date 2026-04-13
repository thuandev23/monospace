package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue
) {
    suspend operator fun invoke(taskId: String) {
        // 1. Soft delete local (đánh dấu pending_delete)
        repository.deleteTask(taskId)

        // 2. Enqueue để SyncWorker xóa trên server
        syncQueue.enqueue(
            taskId = taskId,
            operation = SyncOperationType.DELETE,
            payload = taskId
        )
    }
}
