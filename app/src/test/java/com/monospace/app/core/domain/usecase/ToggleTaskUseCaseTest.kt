package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.SyncOperationType
import com.monospace.app.core.domain.repository.SyncQueue
import com.monospace.app.core.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleTaskUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val syncQueue: SyncQueue = mockk(relaxed = true)
    private val expandRepeatUseCase: ExpandRepeatTaskUseCase = mockk(relaxed = true)
    private val useCase = ToggleTaskUseCase(repository, syncQueue, expandRepeatUseCase)

    private fun makeTask(
        id: String = "task-1",
        status: TaskStatus = TaskStatus.DONE,
        repeat: RepeatConfig? = null,
    ) = Task(
        id = id,
        title = "Test",
        listId = "list-1",
        status = status,
        syncStatus = SyncStatus.SYNCED,
        priority = Priority.NONE,
        startDateTime = null,
        endDateTime = null,
        repeat = repeat,
    )

    // ─── markTaskCompleted ────────────────────────────────────────────────────

    @Test
    fun markTaskCompleted_calledWithTrueWhenCompleting() = runTest {
        useCase("task-1", isCompleted = true)
        coVerify { repository.markTaskCompleted("task-1", true) }
    }

    @Test
    fun markTaskCompleted_calledWithFalseWhenUncompleting() = runTest {
        useCase("task-1", isCompleted = false)
        coVerify { repository.markTaskCompleted("task-1", false) }
    }

    @Test
    fun markTaskCompleted_isCalledExactlyOnce() = runTest {
        useCase("task-1", isCompleted = true)
        coVerify(exactly = 1) { repository.markTaskCompleted(any(), any()) }
    }

    // ─── syncQueue ────────────────────────────────────────────────────────────

    @Test
    fun syncQueue_enqueuesUpdateOperationWhenCompleting() = runTest {
        useCase("task-sync", isCompleted = true)
        coVerify {
            syncQueue.enqueue(
                taskId = "task-sync",
                operation = SyncOperationType.UPDATE,
                payload = "task-sync"
            )
        }
    }

    @Test
    fun syncQueue_enqueuesUpdateOperationWhenUncompleting() = runTest {
        useCase("task-sync", isCompleted = false)
        coVerify {
            syncQueue.enqueue(
                taskId = "task-sync",
                operation = SyncOperationType.UPDATE,
                payload = "task-sync"
            )
        }
    }

    // ─── expandRepeatTaskUseCase ──────────────────────────────────────────────

    @Test
    fun expandRepeat_isCalledWhenTaskIsDoneAndHasRepeat() = runTest {
        val taskWithRepeat = makeTask(
            id = "task-repeat",
            status = TaskStatus.DONE,
            repeat = RepeatConfig(1, RepeatUnit.DAY),
        )
        coEvery { repository.getTaskById("task-repeat") } returns taskWithRepeat

        useCase("task-repeat", isCompleted = true)

        coVerify { expandRepeatUseCase(taskWithRepeat) }
    }

    @Test
    fun expandRepeat_isNotCalledWhenMarkingIncomplete() = runTest {
        useCase("task-1", isCompleted = false)
        coVerify(exactly = 0) { expandRepeatUseCase(any()) }
        // getTaskById should not be called either
        coVerify(exactly = 0) { repository.getTaskById(any()) }
    }

    @Test
    fun expandRepeat_isNotCalledWhenTaskHasNoRepeat() = runTest {
        val taskNoRepeat = makeTask(
            id = "task-no-rep",
            status = TaskStatus.DONE,
            repeat = null,
        )
        coEvery { repository.getTaskById("task-no-rep") } returns taskNoRepeat

        useCase("task-no-rep", isCompleted = true)

        coVerify(exactly = 0) { expandRepeatUseCase(any()) }
    }

    @Test
    fun expandRepeat_isNotCalledWhenGetTaskByIdReturnsNull() = runTest {
        coEvery { repository.getTaskById("task-missing") } returns null

        useCase("task-missing", isCompleted = true)

        coVerify(exactly = 0) { expandRepeatUseCase(any()) }
    }

    @Test
    fun expandRepeat_isNotCalledWhenTaskStatusIsNotDone() = runTest {
        // markTaskCompleted is mocked (relaxed) so status in local mock stays as original
        val taskInProgress = makeTask(
            id = "task-ip",
            status = TaskStatus.IN_PROGRESS,
            repeat = RepeatConfig(1, RepeatUnit.WEEK),
        )
        coEvery { repository.getTaskById("task-ip") } returns taskInProgress

        useCase("task-ip", isCompleted = true)

        // status is IN_PROGRESS (not DONE), so expand should NOT be triggered
        coVerify(exactly = 0) { expandRepeatUseCase(any()) }
    }

    // ─── ordering ─────────────────────────────────────────────────────────────

    @Test
    fun markTaskCompleted_isCalledBeforeSyncQueueEnqueue() = runTest {
        // Use isCompleted=false to avoid getTaskById being called after enqueue,
        // which would break SEQUENCE ordering (no extra calls allowed between verified steps).
        useCase("task-order", isCompleted = false)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            repository.markTaskCompleted("task-order", false)
            syncQueue.enqueue("task-order", SyncOperationType.UPDATE, "task-order")
        }
    }
}
