package com.monospace.app.feature.search

import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val allTasksFlow = MutableStateFlow<List<Task>>(emptyList())

    private val taskRepository: TaskRepository = mockk(relaxed = true) {
        every { observeAllTasksSortedByDate() } returns allTasksFlow
    }

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(taskRepository)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun task(id: String, title: String, notes: String? = null) = Task(
        id = id, title = title, notes = notes,
        listId = "list-1", status = TaskStatus.NOT_DONE,
        syncStatus = SyncStatus.SYNCED, priority = Priority.NONE,
        startDateTime = null, endDateTime = null,
    )

    // ─── setQuery ─────────────────────────────────────────────────────────────

    @Test
    fun setQuery_updatesQueryInState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setQuery("meeting")
        advanceUntilIdle()

        assertEquals("meeting", viewModel.uiState.value.query)
    }

    @Test
    fun setQuery_setsIsSearchingTrueForNonBlankQuery() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", "anything"))
        viewModel.setQuery("a")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSearching)
    }

    @Test
    fun setQuery_blank_setsIsSearchingFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setQuery("   ")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
    }

    // ─── clearQuery ───────────────────────────────────────────────────────────

    @Test
    fun clearQuery_resetsQueryToEmpty() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setQuery("work")
        viewModel.clearQuery()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.query)
    }

    @Test
    fun clearQuery_setsIsSearchingFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", "work"))
        viewModel.setQuery("work")
        viewModel.clearQuery()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
    }

    @Test
    fun clearQuery_returnsEmptyResults() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", "work"))
        viewModel.setQuery("work")
        viewModel.clearQuery()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    // ─── filtering ────────────────────────────────────────────────────────────

    @Test
    fun results_matchesByTitle_caseInsensitive() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(
            task("t1", "Buy Milk"),
            task("t2", "Go gym"),
        )
        viewModel.setQuery("milk")
        advanceUntilIdle()

        val ids = viewModel.uiState.value.results.map { it.id }
        assertTrue("t1" in ids)
        assertFalse("t2" in ids)
    }

    @Test
    fun results_matchesByNotes_caseInsensitive() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(
            task("t1", "Task A", notes = "Important meeting"),
            task("t2", "Task B", notes = null),
        )
        viewModel.setQuery("meeting")
        advanceUntilIdle()

        val ids = viewModel.uiState.value.results.map { it.id }
        assertTrue("t1" in ids)
        assertFalse("t2" in ids)
    }

    @Test
    fun results_emptyWhenQueryIsBlank() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        allTasksFlow.value = listOf(task("t1", "Buy Milk"))
        viewModel.setQuery("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun results_updatesWhenTaskListChanges() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.setQuery("work")
        allTasksFlow.value = listOf(task("t1", "work item"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.results.size)
    }
}
