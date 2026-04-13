package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue
) {
    suspend operator fun invoke(task: Task) {
        // 1. Lưu vào Local DB (Optimistic UI)
        repository.saveTask(task)

        // 2. Enqueue — SyncWorker sẽ tự đọc TaskEntity từ DB để gửi lên server
        syncQueue.enqueue(
            taskId = task.id,
            operation = SyncOperationType.CREATE,
            payload = task.id // SyncWorker query task từ DB theo ID, không cần serialize ở đây
        )
    }
}
