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

class AddTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val syncQueue: SyncQueue = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val useCase = AddTaskUseCase(repository, syncQueue, reminderScheduler)

    private fun makeTask(id: String = "task-1") = Task(
        id = id,
        title = "Buy groceries",
        listId = "list-1",
        status = TaskStatus.NOT_DONE,
        syncStatus = SyncStatus.PENDING_CREATE,
        priority = Priority.NONE,
        startDateTime = null,
        endDateTime = null,
    )

    // ─── repository ───────────────────────────────────────────────────────────

    @Test
    fun saveTask_isCalledWithExactTaskPassedIn() = runTest {
        val task = makeTask("task-abc")
        useCase(task)
        val slot = slot<Task>()
        coVerify { repository.saveTask(capture(slot)) }
        assertEquals(task, slot.captured)
    }

    @Test
    fun saveTask_isCalledExactlyOnce() = runTest {
        useCase(makeTask())
        coVerify(exactly = 1) { repository.saveTask(any()) }
    }

    // ─── syncQueue ────────────────────────────────────────────────────────────

    @Test
    fun syncQueue_enqueuesCreateOperationWithTaskId() = runTest {
        val task = makeTask("task-xyz")
        useCase(task)
        coVerify {
            syncQueue.enqueue(
                taskId = "task-xyz",
                operation = SyncOperationType.CREATE,
                payload = "task-xyz"
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
        val task = makeTask()
        useCase(task)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            repository.saveTask(task)
            syncQueue.enqueue(task.id, SyncOperationType.CREATE, task.id)
        }
    }
}
