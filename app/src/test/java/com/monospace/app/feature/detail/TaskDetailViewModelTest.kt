package com.monospace.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.UpdateTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val listsFlow = MutableStateFlow<List<TaskList>>(emptyList())

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val taskListRepository: TaskListRepository = mockk(relaxed = true) {
        every { observeAllLists() } returns listsFlow
    }
    private val updateTaskUseCase: UpdateTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)

    private fun baseTask(id: String = "task-1") = Task(
        id = id, title = "Buy groceries", notes = "Milk, eggs",
        listId = "list-1", status = TaskStatus.NOT_DONE,
        syncStatus = SyncStatus.SYNCED, priority = Priority.NONE,
        startDateTime = null, endDateTime = null,
    )

    private fun makeViewModel(taskId: String = "task-1"): TaskDetailViewModel {
        val handle = SavedStateHandle(mapOf("taskId" to taskId))
        return TaskDetailViewModel(handle, taskRepository, taskListRepository, updateTaskUseCase, deleteTaskUseCase)
    }

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── Loading ──────────────────────────────────────────────────────────────

    @Test
    fun init_setsEditingStateWhenTaskExists() = runTest {
        coEvery { taskRepository.getTaskById("task-1") } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is TaskDetailUiState.Editing)
        assertEquals("Buy groceries", (vm.uiState.value as TaskDetailUiState.Editing).title)
    }

    @Test
    fun init_setsNotFoundWhenTaskIsNull() = runTest {
        coEvery { taskRepository.getTaskById("task-1") } returns null
        val vm = makeViewModel()
        advanceUntilIdle()

        assertEquals(TaskDetailUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun init_setsNotFoundWhenTaskIdIsEmpty() = runTest {
        val vm = makeViewModel(taskId = "")
        advanceUntilIdle()

        assertEquals(TaskDetailUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun init_populatesAvailableListsFromRepository() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        listsFlow.value = listOf(TaskList(id = "l1", name = "Work"))
        val vm = makeViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as TaskDetailUiState.Editing
        assertEquals(1, state.availableLists.size)
        assertEquals("l1", state.availableLists[0].id)
    }

    // ─── Field edits ──────────────────────────────────────────────────────────

    @Test
    fun onTitleChange_updatesTitle() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onTitleChange("New title")
        assertEquals("New title", (vm.uiState.value as TaskDetailUiState.Editing).title)
    }

    @Test
    fun onNotesChange_updatesNotes() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onNotesChange("Some notes here")
        assertEquals("Some notes here", (vm.uiState.value as TaskDetailUiState.Editing).notes)
    }

    @Test
    fun onListIdChange_updatesListId() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onListIdChange("list-2")
        assertEquals("list-2", (vm.uiState.value as TaskDetailUiState.Editing).listId)
    }

    @Test
    fun onPriorityChange_updatesPriority() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onPriorityChange(Priority.HIGH)
        assertEquals(Priority.HIGH, (vm.uiState.value as TaskDetailUiState.Editing).priority)
    }

    // ─── Date picker ──────────────────────────────────────────────────────────

    @Test
    fun onShowDatePicker_true_setsShowDatePickerTrue() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onShowDatePicker(true)
        assertTrue((vm.uiState.value as TaskDetailUiState.Editing).showDatePicker)
    }

    @Test
    fun onShowDatePicker_false_setsShowDatePickerFalse() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onShowDatePicker(true)
        vm.onShowDatePicker(false)
        assertFalse((vm.uiState.value as TaskDetailUiState.Editing).showDatePicker)
    }

    // ─── onScheduleChange ─────────────────────────────────────────────────────

    @Test
    fun onScheduleChange_updatesDateTimeFields() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        val start = Instant.ofEpochMilli(1_700_000_000_000L)
        vm.onScheduleChange(start = start, end = null, isAllDay = true, reminder = null, repeat = null)

        val state = vm.uiState.value as TaskDetailUiState.Editing
        assertEquals(start, state.startDateTime)
        assertNull(state.endDateTime)
    }

    @Test
    fun onScheduleChange_closesDatePicker() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onShowDatePicker(true)
        vm.onScheduleChange(null, null, true, null, null)

        assertFalse((vm.uiState.value as TaskDetailUiState.Editing).showDatePicker)
    }

    // ─── saveTask ─────────────────────────────────────────────────────────────

    @Test
    fun saveTask_callsUpdateTaskUseCaseWithCurrentState() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onTitleChange("Updated title")
        vm.saveTask()
        advanceUntilIdle()

        val slot = slot<Task>()
        coVerify { updateTaskUseCase(capture(slot)) }
        assertEquals("Updated title", slot.captured.title)
    }

    @Test
    fun saveTask_emitsSavedAndNavigateBackEvent() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<TaskDetailEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.saveTask()
        advanceUntilIdle()

        assertTrue(events.any { it is TaskDetailEvent.SavedAndNavigateBack })
        job.cancel()
    }

    @Test
    fun saveTask_emitsErrorEventWhenTitleIsBlank() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<TaskDetailEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.onTitleChange("   ")
        vm.saveTask()
        advanceUntilIdle()

        assertTrue(events.any { it is TaskDetailEvent.Error })
        coVerify(exactly = 0) { updateTaskUseCase(any()) }
        job.cancel()
    }

    @Test
    fun saveTask_trimsTitleBeforeSaving() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onTitleChange("  Buy milk  ")
        vm.saveTask()
        advanceUntilIdle()

        val slot = slot<Task>()
        coVerify { updateTaskUseCase(capture(slot)) }
        assertEquals("Buy milk", slot.captured.title)
    }

    @Test
    fun saveTask_emitsErrorEventOnException() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        coEvery { updateTaskUseCase(any()) } throws RuntimeException("save failed")
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<TaskDetailEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.saveTask()
        advanceUntilIdle()

        assertTrue(events.any { it is TaskDetailEvent.Error })
        job.cancel()
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Test
    fun deleteTask_callsDeleteTaskUseCaseWithTaskId() = runTest {
        coEvery { taskRepository.getTaskById("task-1") } returns baseTask()
        val vm = makeViewModel("task-1")
        advanceUntilIdle()

        vm.deleteTask()
        advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-1") }
    }

    @Test
    fun deleteTask_emitsDeletedAndNavigateBackEvent() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<TaskDetailEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.deleteTask()
        advanceUntilIdle()

        assertTrue(events.any { it is TaskDetailEvent.DeletedAndNavigateBack })
        job.cancel()
    }

    @Test
    fun deleteTask_emitsErrorEventOnException() = runTest {
        coEvery { taskRepository.getTaskById(any()) } returns baseTask()
        coEvery { deleteTaskUseCase(any()) } throws RuntimeException("delete failed")
        val vm = makeViewModel()
        advanceUntilIdle()

        val events = mutableListOf<TaskDetailEvent>()
        val job = launch { vm.events.collect { events.add(it) } }

        vm.deleteTask()
        advanceUntilIdle()

        assertTrue(events.any { it is TaskDetailEvent.Error })
        job.cancel()
    }
}
