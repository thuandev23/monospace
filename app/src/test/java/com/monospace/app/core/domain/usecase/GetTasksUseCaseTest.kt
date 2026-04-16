package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTasksUseCaseTest {

    private val repository: TaskRepository = mockk(relaxed = true)
    private val useCase = GetTasksUseCase(repository)

    private fun makeTask(id: String) = Task(
        id = id,
        title = "Task $id",
        listId = "list-1",
        status = TaskStatus.NOT_DONE,
        syncStatus = SyncStatus.SYNCED,
        priority = Priority.NONE,
        startDateTime = null,
        endDateTime = null,
    )

    @Test
    fun invoke_returnsFlowFromRepository() = runTest {
        val tasks = listOf(makeTask("t1"), makeTask("t2"))
        val flow = MutableStateFlow(tasks)
        every { repository.observeTasks("list-1") } returns flow

        val result = useCase("list-1").first()

        assertEquals(tasks, result)
    }

    @Test
    fun invoke_passesListIdToRepository() = runTest {
        every { repository.observeTasks(any()) } returns MutableStateFlow(emptyList())

        useCase("list-42")

        verify { repository.observeTasks("list-42") }
    }

    @Test
    fun invoke_returnsSameFlowInstanceAsRepository() = runTest {
        val flow = MutableStateFlow<List<Task>>(emptyList())
        every { repository.observeTasks("list-1") } returns flow

        val result = useCase("list-1")

        assertEquals(flow, result)
    }

    @Test
    fun invoke_returnsEmptyListWhenRepositoryHasNoTasks() = runTest {
        every { repository.observeTasks("empty-list") } returns MutableStateFlow(emptyList())

        val result = useCase("empty-list").first()

        assertEquals(emptyList<Task>(), result)
    }

    @Test
    fun invoke_reflectsFlowUpdates() = runTest {
        val flow = MutableStateFlow<List<Task>>(emptyList())
        every { repository.observeTasks("list-1") } returns flow

        val resultFlow = useCase("list-1")
        assertEquals(0, resultFlow.first().size)

        flow.value = listOf(makeTask("t1"))
        assertEquals(1, resultFlow.first().size)
    }
}
