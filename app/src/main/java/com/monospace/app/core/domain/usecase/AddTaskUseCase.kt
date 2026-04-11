package com.monospace.app.core.domain.usecase


import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue
) {
    suspend operator fun invoke(title: String, listId: String) {
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            notes = null,
            isCompleted = false,
            dueDate = null,
            priority = Priority.NONE,
            listId = listId,
            syncStatus = SyncStatus.PENDING_CREATE
        )

        // 1. Lưu vào Local DB ngay lập tức (Optimistic UI)
        repository.saveTask(task)

        // 2. Đưa vào hàng đợi để đồng bộ sau
        syncQueue.enqueue(
            taskId = task.id,
            operation = SyncOperationType.CREATE,
            payload = title // Tạm thời để title, sau này sẽ là JSON
        )
    }
}