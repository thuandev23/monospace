package com.monospace.app.core.domain.usecase

import android.os.Build
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

        // 3. Nếu vừa complete và task có repeat → tạo instance tiếp theo
        if (isCompleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val task = repository.getTaskById(taskId)
            if (task?.repeat != null) {
                expandRepeatTaskUseCase(task)
            }
        }
    }
}
