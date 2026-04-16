package com.monospace.app.feature.settings

import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.TaskAlignment
import com.monospace.app.core.domain.model.TaskDisplaySettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDefaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsFlow = MutableStateFlow(TaskDisplaySettings())
    private val dataStore: SettingsDataStore = mockk(relaxed = true) {
        every { taskDisplaySettings } returns settingsFlow
    }

    private lateinit var viewModel: TaskDefaultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskDefaultViewModel(dataStore)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    fun update_callsDataStoreSetTaskDisplaySettings() = runTest {
        val newSettings = TaskDisplaySettings(showStatusCircle = false)
        viewModel.update(newSettings)
        advanceUntilIdle()

        coVerify { dataStore.setTaskDisplaySettings(newSettings) }
    }

    @Test
    fun update_withShowStatusCircleFalse_passesCorrectValue() = runTest {
        val slot = slot<TaskDisplaySettings>()
        viewModel.update(TaskDisplaySettings(showStatusCircle = false))
        advanceUntilIdle()

        coVerify { dataStore.setTaskDisplaySettings(capture(slot)) }
        assertEquals(false, slot.captured.showStatusCircle)
    }

    @Test
    fun update_withLowercaseTrue_passesCorrectValue() = runTest {
        val slot = slot<TaskDisplaySettings>()
        viewModel.update(TaskDisplaySettings(lowercase = true))
        advanceUntilIdle()

        coVerify { dataStore.setTaskDisplaySettings(capture(slot)) }
        assertEquals(true, slot.captured.lowercase)
    }

    @Test
    fun update_withCustomFontSize_passesCorrectValue() = runTest {
        val slot = slot<TaskDisplaySettings>()
        viewModel.update(TaskDisplaySettings(fontSize = 20))
        advanceUntilIdle()

        coVerify { dataStore.setTaskDisplaySettings(capture(slot)) }
        assertEquals(20, slot.captured.fontSize)
    }

    @Test
    fun update_withAlignmentCenter_passesCorrectValue() = runTest {
        val slot = slot<TaskDisplaySettings>()
        viewModel.update(TaskDisplaySettings(alignment = TaskAlignment.CENTER))
        advanceUntilIdle()

        coVerify { dataStore.setTaskDisplaySettings(capture(slot)) }
        assertEquals(TaskAlignment.CENTER, slot.captured.alignment)
    }

    @Test
    fun update_isCalledExactlyOnce() = runTest {
        viewModel.update(TaskDisplaySettings())
        advanceUntilIdle()

        coVerify(exactly = 1) { dataStore.setTaskDisplaySettings(any()) }
    }
}
