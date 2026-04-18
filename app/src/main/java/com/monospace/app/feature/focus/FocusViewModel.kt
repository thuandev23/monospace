package com.monospace.app.feature.focus

import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.DetoxStats
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.FocusProfileRepository
import com.monospace.app.core.domain.repository.FocusSessionRepository
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.service.AppBlockingService
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.core.sync.FocusScheduleEnforcer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class FocusMode { MINIMAL, DISPLAY_CLOCK, STOPWATCH, TIMER }

data class FocusTimerState(
    val mode: FocusMode = FocusMode.TIMER,
    val durationMinutes: Int = 25,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

data class FocusUiState(
    val profiles: List<FocusProfile> = emptyList(),
    val activeProfile: FocusProfile? = null,
    val availableLists: List<TaskList> = emptyList(),
    val isLoading: Boolean = true,
    // Sheet state
    val showCreateSheet: Boolean = false,
    val editingProfile: FocusProfile? = null
)

sealed interface FocusEvent {
    data class Error(val message: String) : FocusEvent
}

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusRepo: FocusProfileRepository,
    private val taskListRepo: TaskListRepository,
    private val sessionRepo: FocusSessionRepository,
    private val scheduleEnforcer: FocusScheduleEnforcer
) : ViewModel() {

    private val _showCreateSheet = MutableStateFlow(false)
    private val _editingProfile = MutableStateFlow<FocusProfile?>(null)

    private val _events = MutableSharedFlow<FocusEvent>()
    val events: SharedFlow<FocusEvent> = _events.asSharedFlow()

    // ── Timer state ──────────────────────────────────────────────────────────
    private val _timerState = MutableStateFlow(FocusTimerState())
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    val blockedPackage: StateFlow<String?> = AppBlockingState.blockedPackage

    val detoxStats: StateFlow<DetoxStats> = sessionRepo.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetoxStats())

    private val _hasUsagePermission = MutableStateFlow(checkUsagePermission())
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    fun refreshUsagePermission() {
        _hasUsagePermission.value = checkUsagePermission()
    }

    private fun checkUsagePermission(): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10_000, now)
        return stats != null && stats.isNotEmpty()
    }

    fun openUsageSettings() {
        val intent = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val uiState: StateFlow<FocusUiState> = combine(
        focusRepo.observeAll(),
        focusRepo.observeActive(),
        taskListRepo.observeAllLists(),
        _showCreateSheet,
        _editingProfile
    ) { profiles, active, lists, showSheet, editing ->
        FocusUiState(
            profiles = profiles,
            activeProfile = active,
            availableLists = lists,
            isLoading = false,
            showCreateSheet = showSheet,
            editingProfile = editing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusUiState()
    )

    fun showCreateSheet() {
        _editingProfile.value = null
        _showCreateSheet.value = true
    }

    fun showEditSheet(profile: FocusProfile) {
        _editingProfile.value = profile
        _showCreateSheet.value = true
    }

    fun dismissSheet() {
        _showCreateSheet.value = false
        _editingProfile.value = null
    }

    fun saveProfile(name: String, linkedListId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val existing = _editingProfile.value
                val profile = if (existing != null) {
                    existing.copy(name = name.trim(), linkedListId = linkedListId)
                } else {
                    FocusProfile(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        linkedListId = linkedListId
                    )
                }
                focusRepo.save(profile)
                scheduleEnforcer.scheduleProfile(profile.id, profile.schedule)
                dismissSheet()
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể lưu: ${e.message}"))
            }
        }
    }

    fun setProfileSchedule(profileId: String, schedule: FocusSchedule?) {
        viewModelScope.launch {
            try {
                val profile = focusRepo.getById(profileId) ?: return@launch
                val updated = profile.copy(schedule = schedule)
                focusRepo.save(updated)
                scheduleEnforcer.scheduleProfile(profileId, schedule)
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể cập nhật lịch: ${e.message}"))
            }
        }
    }

    fun activateProfile(id: String) {
        viewModelScope.launch {
            try {
                focusRepo.activate(id)
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể kích hoạt: ${e.message}"))
            }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            try {
                focusRepo.deactivate()
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể tắt: ${e.message}"))
            }
        }
    }

    // ── Timer functions ───────────────────────────────────────────────────────

    fun setFocusMode(mode: FocusMode) {
        if (_timerState.value.isRunning) return
        _timerState.update { it.copy(mode = mode) }
    }

    fun adjustDuration(deltaMinutes: Int) {
        if (_timerState.value.isRunning) return
        val newDuration = (_timerState.value.durationMinutes + deltaMinutes).coerceIn(1, 120)
        _timerState.update { it.copy(durationMinutes = newDuration, remainingSeconds = newDuration * 60L) }
    }

    fun startFocus() {
        val state = _timerState.value
        if (state.isRunning) return
        _timerState.update { it.copy(isRunning = true, isFinished = false, remainingSeconds = it.durationMinutes * 60L) }
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                _timerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            _timerState.update { it.copy(isRunning = false, isFinished = true) }
            AppBlockingService.stop(context)
            val durationMinutes = _timerState.value.durationMinutes
            val profileId = uiState.value.activeProfile?.id
            sessionRepo.recordSession(durationMinutes, profileId)
        }
        val activeProfile = uiState.value.activeProfile
        if (activeProfile != null && activeProfile.allowedAppIds.isNotEmpty() && checkUsagePermission()) {
            AppBlockingService.start(context)
        }
    }

    fun stopFocus() {
        timerJob?.cancel()
        timerJob = null
        _timerState.update {
            it.copy(isRunning = false, isFinished = false, remainingSeconds = it.durationMinutes * 60L)
        }
        AppBlockingService.stop(context)
    }

    fun stopFocusAndDeactivate() {
        stopFocus()
        deactivate()
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            try {
                scheduleEnforcer.cancelProfile(id)
                focusRepo.delete(id)
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể xóa: ${e.message}"))
            }
        }
    }
}
