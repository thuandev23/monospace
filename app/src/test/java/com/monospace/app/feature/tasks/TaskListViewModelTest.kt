package com.monospace.app.feature.tasks

import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val listsFlow       = MutableStateFlow<List<TaskList>>(emptyList())
    private val activeCountFlow = MutableStateFlow(0)
    private val todayCountFlow  = MutableStateFlow(0)

    private val repository: TaskListRepository = mockk(relaxed = true) {
        every { observeAllLists() } returns listsFlow
    }
    private val taskRepository: TaskRepository = mockk(relaxed = true) {
        every { observeAllActiveTaskCount() } returns activeCountFlow
        every { observeTodayTaskCount() }     returns todayCountFlow
    }

    private lateinit var viewModel: TaskListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskListViewModel(repository, taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun list(id: String, sortOrder: Int = 0) = TaskList(
        id         = id,
        name       = "List $id",
        syncStatus = SyncStatus.SYNCED,
        sortOrder  = sortOrder,
    )

    // ─── UI state reflects repository ─────────────────────────────────────────

    @Test
    fun uiState_lists_reflectsRepositoryEmission() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        listsFlow.value = listOf(list("l1"), list("l2"))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.lists.size)
    }

    @Test
    fun uiState_allTaskCount_reflectsTaskRepository() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        activeCountFlow.value = 7
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.allTaskCount)
    }

    @Test
    fun uiState_todayTaskCount_reflectsTaskRepository() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        todayCountFlow.value = 3
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.todayTaskCount)
    }

    // ─── editMode ─────────────────────────────────────────────────────────────

    @Test
    fun enterEditMode_setsIsEditModeTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.enterEditMode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun exitEditMode_setsIsEditModeFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.enterEditMode()
        viewModel.exitEditMode()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditMode)
    }

    // ─── createDialog ─────────────────────────────────────────────────────────

    @Test
    fun showCreateDialog_setsShowCreateDialogTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showCreateDialog()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showCreateDialog)
    }

    @Test
    fun hideCreateDialog_setsShowCreateDialogFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showCreateDialog()
        viewModel.hideCreateDialog()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCreateDialog)
    }

    // ─── startEdit / cancelEdit ───────────────────────────────────────────────

    @Test
    fun startEdit_setsEditingList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l = list("l1")
        viewModel.startEdit(l)
        advanceUntilIdle()

        assertEquals(l, viewModel.uiState.value.editingList)
    }

    @Test
    fun cancelEdit_clearsEditingList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.startEdit(list("l1"))
        viewModel.cancelEdit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingList)
    }

    // ─── createList ───────────────────────────────────────────────────────────

    @Test
    fun createList_callsSaveListWithTrimmedName() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val slot = slot<TaskList>()
        coEvery { repository.saveList(capture(slot)) } returns Unit

        viewModel.createList("  Work  ")
        advanceUntilIdle()

        assertEquals("Work", slot.captured.name)
    }

    @Test
    fun createList_setsSyncStatusToPendingCreate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val slot = slot<TaskList>()
        coEvery { repository.saveList(capture(slot)) } returns Unit

        viewModel.createList("Inbox")
        advanceUntilIdle()

        assertEquals(SyncStatus.PENDING_CREATE, slot.captured.syncStatus)
    }

    @Test
    fun createList_sortOrderIsMaxPlusOne() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        listsFlow.value = listOf(list("l1", sortOrder = 5), list("l2", sortOrder = 10))
        advanceUntilIdle()

        val slot = slot<TaskList>()
        coEvery { repository.saveList(capture(slot)) } returns Unit

        viewModel.createList("New")
        advanceUntilIdle()

        assertEquals(11, slot.captured.sortOrder)
    }

    @Test
    fun createList_hidesDialogAfterSuccess() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.showCreateDialog()
        viewModel.createList("Work")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCreateDialog)
    }

    @Test
    fun createList_doesNothingWhenNameIsBlank() = runTest {
        viewModel.createList("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.saveList(any()) }
    }

    @Test
    fun createList_emitsErrorEventOnException() = runTest {
        coEvery { repository.saveList(any()) } throws RuntimeException("DB error")

        val errors = mutableListOf<String>()
        val job = launch { viewModel.errorEvent.collect { errors.add(it) } }

        viewModel.createList("Work")
        advanceUntilIdle()

        assertTrue(errors.any { it.contains("DB error") })
        job.cancel()
    }

    // ─── renameList ───────────────────────────────────────────────────────────

    @Test
    fun renameList_callsSaveListWithNewName() = runTest {
        val original = list("l1")
        val slot = slot<TaskList>()
        coEvery { repository.saveList(capture(slot)) } returns Unit

        viewModel.renameList(original, "Personal")
        advanceUntilIdle()

        assertEquals("Personal", slot.captured.name)
        assertEquals("l1", slot.captured.id)
    }

    @Test
    fun renameList_setsSyncStatusToPendingUpdate() = runTest {
        val original = list("l1")
        val slot = slot<TaskList>()
        coEvery { repository.saveList(capture(slot)) } returns Unit

        viewModel.renameList(original, "Personal")
        advanceUntilIdle()

        assertEquals(SyncStatus.PENDING_UPDATE, slot.captured.syncStatus)
    }

    @Test
    fun renameList_clearsEditingListAfterSuccess() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l = list("l1")
        viewModel.startEdit(l)
        viewModel.renameList(l, "New Name")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingList)
    }

    @Test
    fun renameList_doesNothingWhenNameIsBlank() = runTest {
        viewModel.renameList(list("l1"), "  ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.saveList(any()) }
    }

    // ─── deleteList ───────────────────────────────────────────────────────────

    @Test
    fun deleteList_callsRepositoryDeleteList() = runTest {
        viewModel.deleteList("l1")
        advanceUntilIdle()

        coVerify { repository.deleteList("l1") }
    }

    @Test
    fun deleteList_emitsErrorEventOnException() = runTest {
        coEvery { repository.deleteList(any()) } throws RuntimeException("delete failed")

        val errors = mutableListOf<String>()
        val job = launch { viewModel.errorEvent.collect { errors.add(it) } }

        viewModel.deleteList("l1")
        advanceUntilIdle()

        assertTrue(errors.isNotEmpty())
        job.cancel()
    }

    // ─── moveListUp ───────────────────────────────────────────────────────────

    @Test
    fun moveListUp_swapsSortOrderWithPreviousList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l1 = list("l1", sortOrder = 1)
        val l2 = list("l2", sortOrder = 2)
        listsFlow.value = listOf(l1, l2)
        advanceUntilIdle()

        viewModel.moveListUp(l2)
        advanceUntilIdle()

        coVerify { repository.updateSortOrder("l2", 1) }
        coVerify { repository.updateSortOrder("l1", 2) }
    }

    @Test
    fun moveListUp_doesNothingWhenAlreadyFirst() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l1 = list("l1", sortOrder = 1)
        val l2 = list("l2", sortOrder = 2)
        listsFlow.value = listOf(l1, l2)
        advanceUntilIdle()

        viewModel.moveListUp(l1)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateSortOrder(any(), any()) }
    }

    @Test
    fun moveListUp_skipsDefaultList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val def = TaskList(id = "default", name = "Inbox", sortOrder = 0)
        val l1  = list("l1", sortOrder = 1)
        val l2  = list("l2", sortOrder = 2)
        listsFlow.value = listOf(def, l1, l2)
        advanceUntilIdle()

        // l1 is index 0 in filtered (non-default) list → already first → no-op
        viewModel.moveListUp(l1)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateSortOrder(any(), any()) }
    }

    // ─── moveListDown ─────────────────────────────────────────────────────────

    @Test
    fun moveListDown_swapsSortOrderWithNextList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l1 = list("l1", sortOrder = 1)
        val l2 = list("l2", sortOrder = 2)
        listsFlow.value = listOf(l1, l2)
        advanceUntilIdle()

        viewModel.moveListDown(l1)
        advanceUntilIdle()

        coVerify { repository.updateSortOrder("l1", 2) }
        coVerify { repository.updateSortOrder("l2", 1) }
    }

    @Test
    fun moveListDown_doesNothingWhenAlreadyLast() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val l1 = list("l1", sortOrder = 1)
        val l2 = list("l2", sortOrder = 2)
        listsFlow.value = listOf(l1, l2)
        advanceUntilIdle()

        viewModel.moveListDown(l2)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateSortOrder(any(), any()) }
    }
}
