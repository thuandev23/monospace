package com.monospace.app.feature.settings

import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.repository.TaskListRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val dataStore: SettingsDataStore = mockk(relaxed = true) {
        every { sidebarItemOrder }   returns MutableStateFlow(emptyList())
        every { sidebarHiddenItems } returns MutableStateFlow(emptySet())
    }
    private val taskListRepo: TaskListRepository = mockk(relaxed = true) {
        every { observeAllLists() } returns MutableStateFlow(emptyList())
    }

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(dataStore, taskListRepo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── saveItemOrder ────────────────────────────────────────────────────────

    @Test
    fun saveItemOrder_callsDataStoreWithOrderedIds() = runTest {
        val order = listOf("today", "upcoming", "search", "settings")
        viewModel.saveItemOrder(order)
        advanceUntilIdle()

        coVerify { dataStore.setSidebarItemOrder(order) }
    }

    @Test
    fun saveItemOrder_isCalledExactlyOnce() = runTest {
        viewModel.saveItemOrder(listOf("a", "b"))
        advanceUntilIdle()

        coVerify(exactly = 1) { dataStore.setSidebarItemOrder(any()) }
    }

    // ─── saveHiddenItems ──────────────────────────────────────────────────────

    @Test
    fun saveHiddenItems_callsDataStoreWithHiddenIds() = runTest {
        val hidden = setOf("search", "settings")
        viewModel.saveHiddenItems(hidden)
        advanceUntilIdle()

        coVerify { dataStore.setSidebarHiddenItems(hidden) }
    }

    @Test
    fun saveHiddenItems_withEmptySet_callsDataStore() = runTest {
        viewModel.saveHiddenItems(emptySet())
        advanceUntilIdle()

        coVerify { dataStore.setSidebarHiddenItems(emptySet()) }
    }

    // ─── toggleItemVisibility ─────────────────────────────────────────────────

    @Test
    fun toggleItemVisibility_addsIdWhenNotHidden() = runTest {
        val currentHidden = setOf("today")
        viewModel.toggleItemVisibility("search", currentHidden)
        advanceUntilIdle()

        coVerify { dataStore.setSidebarHiddenItems(setOf("today", "search")) }
    }

    @Test
    fun toggleItemVisibility_removesIdWhenAlreadyHidden() = runTest {
        val currentHidden = setOf("today", "search")
        viewModel.toggleItemVisibility("search", currentHidden)
        advanceUntilIdle()

        coVerify { dataStore.setSidebarHiddenItems(setOf("today")) }
    }

    @Test
    fun toggleItemVisibility_withEmptyHiddenSet_addsId() = runTest {
        viewModel.toggleItemVisibility("upcoming", emptySet())
        advanceUntilIdle()

        coVerify { dataStore.setSidebarHiddenItems(setOf("upcoming")) }
    }
}
