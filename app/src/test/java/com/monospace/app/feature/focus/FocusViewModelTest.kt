package com.monospace.app.feature.focus

import android.content.Context
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.AppRepository
import com.monospace.app.core.domain.repository.FocusProfileRepository
import com.monospace.app.core.domain.repository.FocusSessionRepository
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.sync.FocusScheduleEnforcer
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
import kotlinx.coroutines.test.advanceTimeBy
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
class FocusViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val profilesFlow = MutableStateFlow<List<FocusProfile>>(emptyList())
    private val activeProfileFlow = MutableStateFlow<FocusProfile?>(null)
    private val listsFlow = MutableStateFlow<List<TaskList>>(emptyList())

    private val focusRepo: FocusProfileRepository = mockk(relaxed = true) {
        every { observeAll() }    returns profilesFlow
        every { observeActive() } returns activeProfileFlow
    }
    private val taskListRepo: TaskListRepository = mockk(relaxed = true) {
        every { observeAllLists() } returns listsFlow
    }
    private val sessionRepo: FocusSessionRepository = mockk(relaxed = true) {
        every { observeStats() } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.monospace.app.core.domain.model.DetoxStats()
        )
    }
    private val scheduleEnforcer: FocusScheduleEnforcer = mockk(relaxed = true)
    private val appRepo: AppRepository = mockk(relaxed = true) {
        every { getInstalledApps() } returns emptyList()
    }
    private val context: Context = mockk(relaxed = true)

    private lateinit var viewModel: FocusViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FocusViewModel(context, focusRepo, taskListRepo, sessionRepo, scheduleEnforcer, appRepo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun profile(id: String = "fp-1", name: String = "Work") =
        FocusProfile(id = id, name = name)

    // ─── showCreateSheet ──────────────────────────────────────────────────────

    @Test
    fun showCreateSheet_setsShowCreateSheetTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showCreateSheet()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showCreateSheet)
    }

    @Test
    fun showCreateSheet_clearsEditingProfile() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showEditSheet(profile())
        viewModel.showCreateSheet()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingProfile)
    }

    // ─── showEditSheet ────────────────────────────────────────────────────────

    @Test
    fun showEditSheet_setsEditingProfile() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        val p = profile("fp-1", "Work")
        viewModel.showEditSheet(p)
        advanceUntilIdle()

        assertEquals(p, viewModel.uiState.value.editingProfile)
    }

    @Test
    fun showEditSheet_setsShowCreateSheetTrue() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showEditSheet(profile())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showCreateSheet)
    }

    // ─── dismissSheet ─────────────────────────────────────────────────────────

    @Test
    fun dismissSheet_setsShowCreateSheetFalse() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showCreateSheet()
        viewModel.dismissSheet()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCreateSheet)
    }

    @Test
    fun dismissSheet_clearsEditingProfile() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showEditSheet(profile())
        viewModel.dismissSheet()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingProfile)
    }

    // ─── saveProfile ──────────────────────────────────────────────────────────

    @Test
    fun saveProfile_doesNothingWhenNameIsBlank() = runTest {
        viewModel.saveProfile("  ", linkedListId = null)
        advanceUntilIdle()

        coVerify(exactly = 0) { focusRepo.save(any()) }
    }

    @Test
    fun saveProfile_new_callsRepoSaveWithTrimmedName() = runTest {
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        viewModel.saveProfile("  Deep Work  ", linkedListId = null)
        advanceUntilIdle()

        assertEquals("Deep Work", slot.captured.name)
    }

    @Test
    fun saveProfile_new_setsLinkedListId() = runTest {
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        viewModel.saveProfile("Work", linkedListId = "list-2")
        advanceUntilIdle()

        assertEquals("list-2", slot.captured.linkedListId)
    }

    @Test
    fun saveProfile_edit_updatesExistingProfile() = runTest {
        val existing = profile("fp-existing", "Old Name")
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        viewModel.showEditSheet(existing)
        viewModel.saveProfile("New Name", linkedListId = "list-3")
        advanceUntilIdle()

        assertEquals("fp-existing", slot.captured.id)
        assertEquals("New Name", slot.captured.name)
        assertEquals("list-3", slot.captured.linkedListId)
    }

    @Test
    fun saveProfile_dismissesSheetAfterSuccess() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.showCreateSheet()
        viewModel.saveProfile("Work", null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showCreateSheet)
    }

    @Test
    fun saveProfile_emitsErrorEventOnException() = runTest {
        coEvery { focusRepo.save(any()) } throws RuntimeException("save error")

        val errors = mutableListOf<FocusEvent>()
        val job = launch { viewModel.events.collect { errors.add(it) } }

        viewModel.saveProfile("Work", null)
        advanceUntilIdle()

        assertTrue(errors.any { it is FocusEvent.Error })
        job.cancel()
    }

    @Test
    fun saveProfile_new_savesAllowedAppIds() = runTest {
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        val apps = setOf("com.instagram.android", "com.tiktok.app")
        viewModel.saveProfile("Deep Work", linkedListId = null, allowedAppIds = apps)
        advanceUntilIdle()

        assertEquals(apps, slot.captured.allowedAppIds)
    }

    @Test
    fun saveProfile_new_savesSchedule() = runTest {
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        val schedule = FocusSchedule(startHour = 9, startMinute = 0, endHour = 17, endMinute = 0, daysOfWeek = setOf(1, 2, 3, 4, 5))
        viewModel.saveProfile("Work", linkedListId = null, schedule = schedule)
        advanceUntilIdle()

        assertEquals(schedule, slot.captured.schedule)
    }

    @Test
    fun saveProfile_new_defaultsToEmptyAllowedAppsAndNullSchedule() = runTest {
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        viewModel.saveProfile("Work", linkedListId = null)
        advanceUntilIdle()

        assertTrue(slot.captured.allowedAppIds.isEmpty())
        assertNull(slot.captured.schedule)
    }

    @Test
    fun saveProfile_edit_preservesAllowedAppIdsAndSchedule() = runTest {
        val apps = setOf("com.instagram.android")
        val schedule = FocusSchedule(9, 0, 17, 0, setOf(1, 2, 3, 4, 5))
        val existing = profile("fp-1").copy(allowedAppIds = apps, schedule = schedule)
        val slot = slot<FocusProfile>()
        coEvery { focusRepo.save(capture(slot)) } returns Unit

        viewModel.showEditSheet(existing)
        viewModel.saveProfile("Renamed", linkedListId = null, allowedAppIds = apps, schedule = schedule)
        advanceUntilIdle()

        assertEquals(apps, slot.captured.allowedAppIds)
        assertEquals(schedule, slot.captured.schedule)
    }

    // ─── activateProfile ──────────────────────────────────────────────────────

    @Test
    fun activateProfile_callsRepoActivateWithId() = runTest {
        viewModel.activateProfile("fp-1")
        advanceUntilIdle()

        coVerify { focusRepo.activate("fp-1") }
    }

    @Test
    fun activateProfile_emitsErrorOnException() = runTest {
        coEvery { focusRepo.activate(any()) } throws RuntimeException("activate failed")

        val errors = mutableListOf<FocusEvent>()
        val job = launch { viewModel.events.collect { errors.add(it) } }

        viewModel.activateProfile("fp-1")
        advanceUntilIdle()

        assertTrue(errors.any { it is FocusEvent.Error })
        job.cancel()
    }

    // ─── deactivate ───────────────────────────────────────────────────────────

    @Test
    fun deactivate_callsRepoDeactivate() = runTest {
        viewModel.deactivate()
        advanceUntilIdle()

        coVerify { focusRepo.deactivate() }
    }

    // ─── deleteProfile ────────────────────────────────────────────────────────

    @Test
    fun deleteProfile_callsRepoDeleteWithId() = runTest {
        viewModel.deleteProfile("fp-1")
        advanceUntilIdle()

        coVerify { focusRepo.delete("fp-1") }
    }

    @Test
    fun deleteProfile_emitsErrorOnException() = runTest {
        coEvery { focusRepo.delete(any()) } throws RuntimeException("delete failed")

        val errors = mutableListOf<FocusEvent>()
        val job = launch { viewModel.events.collect { errors.add(it) } }

        viewModel.deleteProfile("fp-1")
        advanceUntilIdle()

        assertTrue(errors.any { it is FocusEvent.Error })
        job.cancel()
    }

    // ─── setFocusMode ─────────────────────────────────────────────────────────

    @Test
    fun setFocusMode_updatesMode() = runTest {
        viewModel.setFocusMode(FocusMode.STOPWATCH)
        advanceUntilIdle()

        assertEquals(FocusMode.STOPWATCH, viewModel.timerState.value.mode)
    }

    @Test
    fun setFocusMode_doesNothingWhenTimerIsRunning() = runTest {
        viewModel.startFocus() // sets isRunning = true synchronously
        viewModel.setFocusMode(FocusMode.MINIMAL) // should be no-op

        // Mode should still be default TIMER, not MINIMAL
        assertEquals(FocusMode.TIMER, viewModel.timerState.value.mode)
    }

    // ─── adjustDuration ──────────────────────────────────────────────────────

    @Test
    fun adjustDuration_increasesByDelta() = runTest {
        viewModel.adjustDuration(5)
        advanceUntilIdle()

        assertEquals(30, viewModel.timerState.value.durationMinutes)
    }

    @Test
    fun adjustDuration_decreasesByDelta() = runTest {
        viewModel.adjustDuration(-10)
        advanceUntilIdle()

        assertEquals(15, viewModel.timerState.value.durationMinutes)
    }

    @Test
    fun adjustDuration_clampsToMinimum1() = runTest {
        viewModel.adjustDuration(-200)
        advanceUntilIdle()

        assertEquals(1, viewModel.timerState.value.durationMinutes)
    }

    @Test
    fun adjustDuration_clampsToMaximum120() = runTest {
        viewModel.adjustDuration(200)
        advanceUntilIdle()

        assertEquals(120, viewModel.timerState.value.durationMinutes)
    }

    @Test
    fun adjustDuration_updatesRemainingSeconds() = runTest {
        viewModel.adjustDuration(5)
        advanceUntilIdle()

        assertEquals(30 * 60L, viewModel.timerState.value.remainingSeconds)
    }

    @Test
    fun adjustDuration_doesNothingWhenTimerIsRunning() = runTest {
        viewModel.startFocus() // sets isRunning = true synchronously
        val durationBefore = viewModel.timerState.value.durationMinutes
        viewModel.adjustDuration(10) // should be no-op

        assertEquals(durationBefore, viewModel.timerState.value.durationMinutes)
    }

    // ─── startFocus / stopFocus ───────────────────────────────────────────────

    @Test
    fun startFocus_setsIsRunningTrue() = runTest {
        // isRunning is set synchronously before the timer coroutine starts
        viewModel.startFocus()

        assertTrue(viewModel.timerState.value.isRunning)
    }

    @Test
    fun startFocus_doesNothingWhenAlreadyRunning() = runTest {
        viewModel.startFocus()
        // Don't advanceUntilIdle — keep timer running
        val remainingBefore = viewModel.timerState.value.remainingSeconds
        viewModel.startFocus()    // second call should be no-op

        assertEquals(remainingBefore, viewModel.timerState.value.remainingSeconds)
    }

    @Test
    fun stopFocus_setsIsRunningFalse() = runTest {
        viewModel.startFocus()
        viewModel.stopFocus()

        assertFalse(viewModel.timerState.value.isRunning)
    }

    @Test
    fun stopFocus_resetsRemainingSecondsToFullDuration() = runTest {
        viewModel.startFocus()
        advanceTimeBy(10_000L) // advance 10 seconds
        viewModel.stopFocus()
        advanceUntilIdle()

        val expected = viewModel.timerState.value.durationMinutes * 60L
        assertEquals(expected, viewModel.timerState.value.remainingSeconds)
    }

    @Test
    fun timer_decrementsRemainingSecondsOverTime() = runTest {
        viewModel.startFocus()
        val initial = viewModel.timerState.value.remainingSeconds
        advanceTimeBy(3_000L) // 3 seconds
        advanceUntilIdle()

        assertTrue(viewModel.timerState.value.remainingSeconds < initial)
    }
}
