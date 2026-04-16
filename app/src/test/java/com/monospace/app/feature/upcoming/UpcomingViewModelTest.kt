package com.monospace.app.feature.upcoming

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val allTasksFlow = MutableStateFlow<List<Task>>(emptyList())

    private val taskRepository: TaskRepository = mockk(relaxed = true) {
        every { observeAllTasksSortedByDate() } returns allTasksFlow
    }
    private val toggleTaskUseCase: ToggleTaskUseCase = mockk(relaxed = true)
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk(relaxed = true)

    private lateinit var viewModel: UpcomingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UpcomingViewModel(taskRepository, toggleTaskUseCase, deleteTaskUseCase)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun task(
        id: String,
        title: String = "Task $id",
        status: TaskStatus = TaskStatus.NOT_DONE,
        startDate: LocalDate? = null
    ) = Task(
        id = id,
        title = title,
        listId = "list-1",
        status = status,
        syncStatus = SyncStatus.SYNCED,
        priority = Priority.NONE,
        startDateTime = startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant(),
        endDateTime = null,
    )

    private fun today() = LocalDate.now(ZoneId.systemDefault())
    private fun successState() = viewModel.uiState.value as? UpcomingUiState.Success

    // ─── initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        // Before any collection, state should be Loading
        assertTrue(viewModel.uiState.value is UpcomingUiState.Loading)
    }

    @Test
    fun uiState_becomesSuccessOnTaskEmission() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is UpcomingUiState.Success)
    }

    @Test
    fun uiState_showCompleted_isFalseByDefault() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = emptyList()
        advanceUntilIdle()

        assertFalse(successState()!!.showCompleted)
    }

    // ─── toggleShowCompleted ──────────────────────────────────────────────────

    @Test
    fun toggleShowCompleted_setsShowCompletedTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = emptyList()
        advanceUntilIdle()

        viewModel.toggleShowCompleted()
        advanceUntilIdle()

        assertTrue(successState()!!.showCompleted)
    }

    @Test
    fun toggleShowCompleted_twice_returnsToFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = emptyList()
        advanceUntilIdle()

        viewModel.toggleShowCompleted()
        viewModel.toggleShowCompleted()
        advanceUntilIdle()

        assertFalse(successState()!!.showCompleted)
    }

    @Test
    fun toggleShowCompleted_completedTasksAreAlwaysInState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(
            task("t1", status = TaskStatus.DONE),
            task("t2", status = TaskStatus.NOT_DONE)
        )
        advanceUntilIdle()

        // completedTasks list is always populated regardless of showCompleted flag
        assertEquals(1, successState()!!.completedTasks.size)
        assertEquals("t1", successState()!!.completedTasks[0].id)
    }

    // ─── toggleTask ───────────────────────────────────────────────────────────

    @Test
    fun toggleTask_callsUseCaseWithCorrectArgs_complete() = runTest {
        viewModel.toggleTask("task-1", isCompleted = true)
        advanceUntilIdle()

        coVerify { toggleTaskUseCase("task-1", true) }
    }

    @Test
    fun toggleTask_callsUseCaseWithCorrectArgs_uncomplete() = runTest {
        viewModel.toggleTask("task-1", isCompleted = false)
        advanceUntilIdle()

        coVerify { toggleTaskUseCase("task-1", false) }
    }

    @Test
    fun toggleTask_emitsErrorEventOnException() = runTest {
        coEvery { toggleTaskUseCase(any(), any()) } throws RuntimeException("toggle failed")

        val errors = mutableListOf<String>()
        val job = launch { viewModel.errorEvent.collect { errors.add(it) } }

        viewModel.toggleTask("task-1", true)
        advanceUntilIdle()

        assertTrue(errors.isNotEmpty())
        job.cancel()
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Test
    fun deleteTask_callsDeleteUseCaseWithId() = runTest {
        viewModel.deleteTask("task-1")
        advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-1") }
    }

    @Test
    fun deleteTask_emitsErrorEventOnException() = runTest {
        coEvery { deleteTaskUseCase(any()) } throws RuntimeException("delete failed")

        val errors = mutableListOf<String>()
        val job = launch { viewModel.errorEvent.collect { errors.add(it) } }

        viewModel.deleteTask("task-1")
        advanceUntilIdle()

        assertTrue(errors.isNotEmpty())
        job.cancel()
    }

    // ─── task grouping ─────────────────────────────────────────────────────────

    @Test
    fun grouping_taskWithNullDate_goesToNoDateGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = null))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val noDateGroup = groups.find { it.type == UpcomingGroupType.NO_DATE }
        assertTrue(noDateGroup != null)
        assertEquals("t1", noDateGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_taskWithTodayDate_goesToTodayGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today()))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val todayGroup = groups.find { it.type == UpcomingGroupType.TODAY }
        assertTrue(todayGroup != null)
        assertEquals("t1", todayGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_taskWithTomorrowDate_goesToTomorrowGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today().plusDays(1)))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val tomorrowGroup = groups.find { it.type == UpcomingGroupType.TOMORROW }
        assertTrue(tomorrowGroup != null)
        assertEquals("t1", tomorrowGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_taskWithPastDate_goesToOverdueGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today().minusDays(1)))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val overdueGroup = groups.find { it.type == UpcomingGroupType.OVERDUE }
        assertTrue(overdueGroup != null)
        assertEquals("t1", overdueGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_taskWithin7Days_goesToThisWeekGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today().plusDays(3)))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val thisWeekGroup = groups.find { it.type == UpcomingGroupType.THIS_WEEK }
        assertTrue(thisWeekGroup != null)
        assertEquals("t1", thisWeekGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_taskBeyond7Days_goesToLaterGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today().plusDays(10)))
        advanceUntilIdle()

        val groups = successState()!!.groups
        val laterGroup = groups.find { it.type == UpcomingGroupType.LATER }
        assertTrue(laterGroup != null)
        assertEquals("t1", laterGroup!!.tasks[0].id)
    }

    @Test
    fun grouping_completedTasksExcludedFromGroups() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(
            task("t1", status = TaskStatus.DONE, startDate = today()),
            task("t2", status = TaskStatus.NOT_DONE, startDate = today())
        )
        advanceUntilIdle()

        val groups = successState()!!.groups
        val todayGroup = groups.find { it.type == UpcomingGroupType.TODAY }
        assertEquals(1, todayGroup?.tasks?.size)
        assertEquals("t2", todayGroup?.tasks?.get(0)?.id)
    }

    @Test
    fun grouping_emptyGroupsNotIncluded() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today()))
        advanceUntilIdle()

        val groups = successState()!!.groups
        // Only TODAY group should be present, no empty groups
        assertEquals(1, groups.size)
        assertEquals(UpcomingGroupType.TODAY, groups[0].type)
    }

    @Test
    fun grouping_multipleTasksInSameGroup() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(
            task("t1", startDate = today()),
            task("t2", startDate = today()),
            task("t3", startDate = today())
        )
        advanceUntilIdle()

        val groups = successState()!!.groups
        val todayGroup = groups.find { it.type == UpcomingGroupType.TODAY }
        assertEquals(3, todayGroup?.tasks?.size)
    }

    @Test
    fun grouping_updatesWhenTaskListChanges() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", startDate = today()))
        advanceUntilIdle()
        assertEquals(1, successState()!!.groups.sumOf { it.tasks.size })

        allTasksFlow.value = listOf(
            task("t1", startDate = today()),
            task("t2", startDate = today())
        )
        advanceUntilIdle()
        assertEquals(2, successState()!!.groups.sumOf { it.tasks.size })
    }
}
