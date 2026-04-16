package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.sync.ReminderScheduler
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val syncQueue: SyncQueue = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val useCase = UpdateTaskUseCase(repository, syncQueue, reminderScheduler)

    private fun makeTask(
        id: String = "task-1",
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = Task(
        id = id,
        title = "Updated Task",
        listId = "list-1",
        status = TaskStatus.IN_PROGRESS,
        syncStatus = syncStatus,
        priority = Priority.HIGH,
        startDateTime = null,
        endDateTime = null,
    )

    // ─── repository ───────────────────────────────────────────────────────────

    @Test
    fun saveTask_isCalledWithPendingUpdateSyncStatus() = runTest {
        val task = makeTask(syncStatus = SyncStatus.SYNCED)
        useCase(task)
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(SyncStatus.PENDING_UPDATE, slot.captured.syncStatus)
    }

    @Test
    fun saveTask_overridesSyncStatusRegardlessOfOriginalValue() = runTest {
        // Even if task already has PENDING_CREATE, it should be overridden to PENDING_UPDATE
        val task = makeTask(syncStatus = SyncStatus.PENDING_CREATE)
        useCase(task)
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(SyncStatus.PENDING_UPDATE, slot.captured.syncStatus)
    }

    @Test
    fun saveTask_preservesAllOtherTaskFields() = runTest {
        val task = makeTask(id = "task-preserve")
        useCase(task)
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(task.id,       slot.captured.id)
        assertEquals(task.title,    slot.captured.title)
        assertEquals(task.listId,   slot.captured.listId)
        assertEquals(task.priority, slot.captured.priority)
        assertEquals(task.status,   slot.captured.status)
    }

    @Test
    fun saveTask_isCalledExactlyOnce() = runTest {
        useCase(makeTask())
        coVerify(exactly = 1) { repository.saveTask(any()) }
    }

    // ─── syncQueue ────────────────────────────────────────────────────────────

    @Test
    fun syncQueue_enqueuesUpdateOperationWithTaskId() = runTest {
        val task = makeTask(id = "task-upd")
        useCase(task)
        coVerify {
            syncQueue.enqueue(
                taskId = "task-upd",
                operation = SyncOperationType.UPDATE,
                payload = "task-upd"
            )
        }
    }

    @Test
    fun syncQueue_isCalledExactlyOnce() = runTest {
        useCase(makeTask())
        coVerify(exactly = 1) { syncQueue.enqueue(any(), any(), any()) }
    }

    // ─── ordering ─────────────────────────────────────────────────────────────

    @Test
    fun saveTask_isCalledBeforeSyncQueueEnqueue() = runTest {
        val task = makeTask(id = "task-order")
        useCase(task)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            repository.saveTask(any())
            syncQueue.enqueue("task-order", SyncOperationType.UPDATE, "task-order")
        }
    }
}
