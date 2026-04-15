package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue,
    private val expandRepeatTaskUseCase: ExpandRepeatTaskUseCase
) {

    suspend operator fun invoke(taskId: String, isCompleted: Boolean) {
        // 1. Cập nhật local (sync_status tự động đổi thành pending_update trong DAO)
        repository.markTaskCompleted(taskId, isCompleted)

        // 2. Enqueue để SyncWorker push lên server
        syncQueue.enqueue(
            taskId = taskId,
            operation = SyncOperationType.UPDATE,
            payload = taskId
        )

        // 3. Nếu vừa DONE và task có repeat → tạo instance tiếp theo
        if (isCompleted) {
            val task = repository.getTaskById(taskId)
            if (task?.repeat != null && task.status == com.monospace.app.core.domain.model.TaskStatus.DONE) {
                expandRepeatTaskUseCase(task)
            }
        }
    }
}
