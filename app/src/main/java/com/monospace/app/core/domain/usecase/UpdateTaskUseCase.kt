package com.monospace.app.core.domain.usecase

import android.os.Build
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.sync.ReminderScheduler
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val syncQueue: SyncQueue,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(task: Task) {
        val updatedTask = task.copy(syncStatus = SyncStatus.PENDING_UPDATE)

        repository.saveTask(updatedTask)

        syncQueue.enqueue(
            taskId = updatedTask.id,
            operation = SyncOperationType.UPDATE,
            payload = updatedTask.id
        )

        // Re-schedule reminder nếu startDateTime hoặc reminderConfig thay đổi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            reminderScheduler.cancelReminder(updatedTask.id)
            reminderScheduler.scheduleReminder(updatedTask)
        }
    }
}
