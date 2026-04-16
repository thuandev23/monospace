package com.monospace.app.feature.settings

import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.AddTaskPosition
import com.monospace.app.core.domain.model.AppTheme
import com.monospace.app.core.domain.model.GeneralSettings
import com.monospace.app.core.domain.model.SecondStatus
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
class GeneralSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsFlow = MutableStateFlow(GeneralSettings())
    private val dataStore: SettingsDataStore = mockk(relaxed = true) {
        every { generalSettings } returns settingsFlow
    }

    private lateinit var viewModel: GeneralSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = GeneralSettingsViewModel(dataStore)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    fun update_callsDataStoreSetGeneralSettings() = runTest {
        val newSettings = GeneralSettings(theme = AppTheme.REMINDERS)
        viewModel.update(newSettings)
        advanceUntilIdle()

        coVerify { dataStore.setGeneralSettings(newSettings) }
    }

    @Test
    fun update_withThemeChange_passesCorrectTheme() = runTest {
        val slot = slot<GeneralSettings>()
        viewModel.update(GeneralSettings(theme = AppTheme.REMINDERS))
        advanceUntilIdle()

        coVerify { dataStore.setGeneralSettings(capture(slot)) }
        assertEquals(AppTheme.REMINDERS, slot.captured.theme)
    }

    @Test
    fun update_withAddTaskPositionTop_passesCorrectPosition() = runTest {
        val slot = slot<GeneralSettings>()
        viewModel.update(GeneralSettings(addTaskPosition = AddTaskPosition.TOP))
        advanceUntilIdle()

        coVerify { dataStore.setGeneralSettings(capture(slot)) }
        assertEquals(AddTaskPosition.TOP, slot.captured.addTaskPosition)
    }

    @Test
    fun update_withSecondStatusInProgress_passesCorrectStatus() = runTest {
        val slot = slot<GeneralSettings>()
        viewModel.update(GeneralSettings(secondStatus = SecondStatus.IN_PROGRESS))
        advanceUntilIdle()

        coVerify { dataStore.setGeneralSettings(capture(slot)) }
        assertEquals(SecondStatus.IN_PROGRESS, slot.captured.secondStatus)
    }

    @Test
    fun update_withReverseScrollTrue_passesCorrectValue() = runTest {
        val slot = slot<GeneralSettings>()
        viewModel.update(GeneralSettings(reverseScrollDirection = true))
        advanceUntilIdle()

        coVerify { dataStore.setGeneralSettings(capture(slot)) }
        assertEquals(true, slot.captured.reverseScrollDirection)
    }

    @Test
    fun update_isCalledExactlyOnce() = runTest {
        viewModel.update(GeneralSettings())
        advanceUntilIdle()

        coVerify(exactly = 1) { dataStore.setGeneralSettings(any()) }
    }
}
