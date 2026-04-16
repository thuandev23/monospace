package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val syncQueue: SyncQueue = mockk(relaxed = true)
    private val useCase = DeleteTaskUseCase(repository, syncQueue)

    // ─── repository ───────────────────────────────────────────────────────────

    @Test
    fun deleteTask_isCalledWithCorrectTaskId() = runTest {
        useCase("task-42")
        coVerify { repository.deleteTask("task-42") }
    }

    @Test
    fun deleteTask_isCalledExactlyOnce() = runTest {
        useCase("task-1")
        coVerify(exactly = 1) { repository.deleteTask(any()) }
    }

    // ─── syncQueue ────────────────────────────────────────────────────────────

    @Test
    fun syncQueue_enqueuesDeleteOperationWithTaskId() = runTest {
        useCase("task-99")
        coVerify {
            syncQueue.enqueue(
                taskId = "task-99",
                operation = SyncOperationType.DELETE,
                payload = "task-99"
            )
        }
    }

    @Test
    fun syncQueue_isCalledExactlyOnce() = runTest {
        useCase("task-1")
        coVerify(exactly = 1) { syncQueue.enqueue(any(), any(), any()) }
    }

    // ─── ordering ─────────────────────────────────────────────────────────────

    @Test
    fun deleteTask_isCalledBeforeSyncQueueEnqueue() = runTest {
        val id = "task-order"
        useCase(id)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            repository.deleteTask(id)
            syncQueue.enqueue(id, SyncOperationType.DELETE, id)
        }
    }
}
