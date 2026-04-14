package com.monospace.app.core.domain.usecase

import android.os.Build
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.sync.ReminderScheduler
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(task: Task) {
        // 1. Lưu vào Local DB (Optimistic UI)
        repository.saveTask(task)

        // 2. Enqueue sync
        syncQueue.enqueue(
            taskId = task.id,
            operation = SyncOperationType.CREATE,
            payload = task.id
        )

        // 3. Schedule reminder nếu task có cấu hình reminder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            reminderScheduler.scheduleReminder(task)
        }
    }
}
