package com.monospace.app.feature.launcher

import androidx.lifecycle.SavedStateHandle
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.model.ViewSettings
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.AddTaskUseCase
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import com.monospace.app.core.domain.usecase.UpdateTaskUseCase
import com.monospace.app.feature.launcher.state.HomeUiState
import com.monospace.app.feature.launcher.state.HomeViewModel
import android.content.Context
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val tasksFlow    = MutableStateFlow<List<Task>>(emptyList())
    private val listsFlow    = MutableStateFlow<List<TaskList>>(emptyList())
    private val viewSettingsFlow = MutableStateFlow(ViewSettings())

    private val getTasksUseCase: GetTasksUseCase = mockk()
    private val addTaskUseCase: AddTaskUseCase   = mockk(relaxed = true)
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)
    private val updateTaskUseCase: UpdateTaskUseCase = mockk(relaxed = true)

    private val taskRepository: TaskRepository = mockk(relaxed = true) {
        every { observeAllActiveTaskCount() } returns MutableStateFlow(0)
        every { observeTodayTaskCount() }     returns MutableStateFlow(0)
    }
    private val taskListRepository: TaskListRepository = mockk(relaxed = true) {
        every { observeAllLists() } returns listsFlow
    }
    private val settingsDataStore: SettingsDataStore = mockk(relaxed = true) {
        every { viewSettings } returns viewSettingsFlow
    }
    private val context: Context = mockk(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getTasksUseCase(any()) } returns tasksFlow
        viewModel = HomeViewModel(
            savedStateHandle    = SavedStateHandle(),
            context             = context,
            getTasksUseCase     = getTasksUseCase,
            addTaskUseCase      = addTaskUseCase,
            toggleTaskUseCase   = toggleTaskUseCase,
            deleteTaskUseCase   = deleteTaskUseCase,
            updateTaskUseCase   = updateTaskUseCase,
            taskRepository      = taskRepository,
            taskListRepository  = taskListRepository,
            settingsDataStore   = settingsDataStore
        )
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun task(id: String, priority: Priority = Priority.NONE, status: TaskStatus = TaskStatus.NOT_DONE) = Task(
        id = id, title = "Task $id", listId = "default",
        status = status, syncStatus = SyncStatus.SYNCED,
        priority = priority, startDateTime = null, endDateTime = null,
    )

    private fun successState() = viewModel.uiState.value as? HomeUiState.Success

    // ─── addTask ──────────────────────────────────────────────────────────────

    @Test
    fun addTask_callsAddTaskUseCaseWithTitle() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val slot = slot<Task>()
        coEvery { addTaskUseCase(capture(slot)) } returns Unit

        viewModel.addTask("Buy milk")
        advanceUntilIdle()

        assertEquals("Buy milk", slot.captured.title)
    }

    @Test
    fun addTask_doesNothingWhenTitleIsBlank() = runTest {
        viewModel.addTask("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { addTaskUseCase(any()) }
    }

    @Test
    fun addTask_hidesCreateSheetAfterSuccess() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setShowCreateSheet(true)
        viewModel.addTask("Task")
        advanceUntilIdle()

        assertFalse(successState()!!.showCreateSheet)
    }

    @Test
    fun addTask_resetsDraftListIdToDefault() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setDraftListId("list-custom")
        viewModel.addTask("Task")
        advanceUntilIdle()

        assertEquals("default", successState()!!.draftListId)
    }

    @Test
    fun addTask_resetsDraftDateTimeToNull() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.updateDraftSchedule(Instant.now(), null, true, null, null)
        viewModel.addTask("Task")
        advanceUntilIdle()

        assertNull(successState()!!.draftStartDateTime)
    }

    @Test
    fun addTask_usesCurrentDraftListId() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val slot = slot<Task>()
        coEvery { addTaskUseCase(capture(slot)) } returns Unit

        viewModel.setDraftListId("list-work")
        viewModel.addTask("Task")
        advanceUntilIdle()

        assertEquals("list-work", slot.captured.listId)
    }

    // ─── toggleTask ───────────────────────────────────────────────────────────

    @Test
    fun toggleTask_callsToggleTaskUseCaseWithCorrectArgs() = runTest {
        viewModel.toggleTask("task-1", isCompleted = true)
        advanceUntilIdle()

        coVerify { toggleTaskUseCase("task-1", true) }
    }

    @Test
    fun toggleTask_callsWithFalseWhenUncompleting() = runTest {
        viewModel.toggleTask("task-1", isCompleted = false)
        advanceUntilIdle()

        coVerify { toggleTaskUseCase("task-1", false) }
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Test
    fun deleteTask_callsDeleteTaskUseCaseWithId() = runTest {
        viewModel.deleteTask("task-1")
        advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-1") }
    }

    // ─── selection mode ───────────────────────────────────────────────────────

    @Test
    fun setSelectionMode_true_setsIsSelectionModeTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSelectionMode(true)
        advanceUntilIdle()

        assertTrue(successState()!!.isSelectionMode)
    }

    @Test
    fun setSelectionMode_false_clearsSelectedTaskIds() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSelectionMode(true)
        viewModel.toggleTaskSelection("t1")
        viewModel.setSelectionMode(false)
        advanceUntilIdle()

        assertTrue(successState()!!.selectedTaskIds.isEmpty())
    }

    @Test
    fun toggleTaskSelection_addsTaskIdWhenNotSelected() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSelectionMode(true)
        viewModel.toggleTaskSelection("task-1")
        advanceUntilIdle()

        assertTrue("task-1" in successState()!!.selectedTaskIds)
    }

    @Test
    fun toggleTaskSelection_removesTaskIdWhenAlreadySelected() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.toggleTaskSelection("task-1")
        viewModel.toggleTaskSelection("task-1")
        advanceUntilIdle()

        assertFalse("task-1" in successState()!!.selectedTaskIds)
    }

    @Test
    fun selectAll_selectsAllCurrentTaskIds() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        tasksFlow.value = listOf(task("t1"), task("t2"), task("t3"))
        advanceUntilIdle()

        viewModel.selectAll()
        advanceUntilIdle()

        val selected = successState()!!.selectedTaskIds
        assertTrue("t1" in selected)
        assertTrue("t2" in selected)
        assertTrue("t3" in selected)
    }

    // ─── deleteSelectedTasks ──────────────────────────────────────────────────

    @Test
    fun deleteSelectedTasks_callsDeleteUseCaseForEachSelectedTask() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.toggleTaskSelection("t1")
        viewModel.toggleTaskSelection("t2")
        advanceUntilIdle()

        viewModel.deleteSelectedTasks()
        advanceUntilIdle()

        coVerify { deleteTaskUseCase("t1") }
        coVerify { deleteTaskUseCase("t2") }
    }

    @Test
    fun deleteSelectedTasks_exitsSelectionMode() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSelectionMode(true)
        viewModel.toggleTaskSelection("t1")
        viewModel.deleteSelectedTasks()
        advanceUntilIdle()

        assertFalse(successState()!!.isSelectionMode)
    }

    // ─── markSelectedTasksDone ────────────────────────────────────────────────

    @Test
    fun markSelectedTasksDone_callsToggleUseCaseWithTrueForEachSelected() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.toggleTaskSelection("t1")
        viewModel.toggleTaskSelection("t2")
        advanceUntilIdle()

        viewModel.markSelectedTasksDone()
        advanceUntilIdle()

        coVerify { toggleTaskUseCase("t1", true) }
        coVerify { toggleTaskUseCase("t2", true) }
    }

    @Test
    fun markSelectedTasksDone_exitsSelectionMode() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSelectionMode(true)
        viewModel.toggleTaskSelection("t1")
        viewModel.markSelectedTasksDone()
        advanceUntilIdle()

        assertFalse(successState()!!.isSelectionMode)
    }

    // ─── moveSelectedTasksToList ───────────────────────────────────────────────

    @Test
    fun moveSelectedTasksToList_callsUpdateUseCaseWithNewListId() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val t1 = task("t1")
        coEvery { taskRepository.getTaskById("t1") } returns t1

        viewModel.toggleTaskSelection("t1")
        viewModel.moveSelectedTasksToList("list-work")
        advanceUntilIdle()

        val slot = slot<Task>()
        coVerify { updateTaskUseCase(capture(slot)) }
        assertEquals("list-work", slot.captured.listId)
    }

    @Test
    fun moveSelectedTasksToList_exitsSelectionMode() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        coEvery { taskRepository.getTaskById(any()) } returns task("t1")
        viewModel.setSelectionMode(true)
        viewModel.toggleTaskSelection("t1")
        viewModel.moveSelectedTasksToList("list-x")
        advanceUntilIdle()

        assertFalse(successState()!!.isSelectionMode)
    }

    // ─── rescheduleSelectedTasks ──────────────────────────────────────────────

    @Test
    fun rescheduleSelectedTasks_updatesStartDateTimeForSelectedTasks() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val t1 = task("t1")
        coEvery { taskRepository.getTaskById("t1") } returns t1

        val newStart = Instant.ofEpochMilli(1_700_000_000_000L)
        viewModel.toggleTaskSelection("t1")
        viewModel.rescheduleSelectedTasks(start = newStart, end = null, isAllDay = true, reminder = null, repeat = null)
        advanceUntilIdle()

        val slot = slot<Task>()
        coVerify { updateTaskUseCase(capture(slot)) }
        assertEquals(newStart, slot.captured.startDateTime)
    }

    // ─── sheet / dialog state ─────────────────────────────────────────────────

    @Test
    fun setMenuExpanded_true_setsIsMenuExpandedTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setMenuExpanded(true)
        advanceUntilIdle()

        assertTrue(successState()!!.isMenuExpanded)
    }

    @Test
    fun setMenuExpanded_false_setsIsMenuExpandedFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setMenuExpanded(true)
        viewModel.setMenuExpanded(false)
        advanceUntilIdle()

        assertFalse(successState()!!.isMenuExpanded)
    }

    @Test
    fun setShowCreateSheet_true_setsShowCreateSheetTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setShowCreateSheet(true)
        advanceUntilIdle()

        assertTrue(successState()!!.showCreateSheet)
    }

    @Test
    fun setShowDatePicker_true_setsShowDatePickerTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setShowDatePicker(true)
        advanceUntilIdle()

        assertTrue(successState()!!.showDatePicker)
    }

    // ─── draft state ──────────────────────────────────────────────────────────

    @Test
    fun setDraftListId_updatesDraftListId() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setDraftListId("list-work")
        advanceUntilIdle()

        assertEquals("list-work", successState()!!.draftListId)
    }

    @Test
    fun updateDraftSchedule_updatesDraftFields() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val start = Instant.ofEpochMilli(1_700_000_000_000L)
        viewModel.updateDraftSchedule(start = start, end = null, isAllDay = false, reminder = null, repeat = null)
        advanceUntilIdle()

        val state = successState()!!
        assertEquals(start, state.draftStartDateTime)
        assertFalse(state.draftIsAllDay)
    }

    // ─── search ───────────────────────────────────────────────────────────────

    @Test
    fun setSearchQuery_updatesSearchQuery() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSearchQuery("meeting")
        advanceUntilIdle()

        assertEquals("meeting", successState()!!.searchQuery)
    }

    @Test
    fun clearSearch_resetsSearchQueryToEmpty() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setSearchQuery("meeting")
        viewModel.clearSearch()
        advanceUntilIdle()

        assertEquals("", successState()!!.searchQuery)
    }

    // ─── priorityFilter ───────────────────────────────────────────────────────

    @Test
    fun setPriorityFilter_setsPriorityFilter() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setPriorityFilter(Priority.HIGH)
        advanceUntilIdle()

        assertEquals(Priority.HIGH, successState()!!.priorityFilter)
    }

    @Test
    fun setPriorityFilter_togglesOffWhenSamePrioritySet() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setPriorityFilter(Priority.HIGH)
        viewModel.setPriorityFilter(Priority.HIGH)
        advanceUntilIdle()

        assertNull(successState()!!.priorityFilter)
    }

    @Test
    fun setPriorityFilter_switchesToDifferentPriority() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setPriorityFilter(Priority.HIGH)
        viewModel.setPriorityFilter(Priority.LOW)
        advanceUntilIdle()

        assertEquals(Priority.LOW, successState()!!.priorityFilter)
    }

    // ─── viewSettings ─────────────────────────────────────────────────────────

    @Test
    fun setViewSettings_callsDataStoreSetViewSettings() = runTest {
        val newSettings = ViewSettings(showCompleted = false)
        viewModel.setViewSettings(newSettings)
        advanceUntilIdle()

        coVerify { settingsDataStore.setViewSettings(newSettings) }
    }
}
