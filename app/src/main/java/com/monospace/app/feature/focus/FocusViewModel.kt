package com.monospace.app.feature.focus

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.MainActivity
import com.monospace.app.MonospaceApp.Companion.CHANNEL_REMINDER
import com.monospace.app.R
import com.monospace.app.core.domain.model.AppInfo
import com.monospace.app.core.domain.model.DetoxStats
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.AppRepository
import com.monospace.app.core.domain.repository.FocusProfileRepository
import com.monospace.app.core.domain.repository.FocusSessionRepository
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.service.AppBlockingService
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.core.sync.FocusScheduleEnforcer
import com.monospace.app.widget.WidgetUpdater
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
    val installedApps: List<AppInfo> = emptyList(),
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
    private val scheduleEnforcer: FocusScheduleEnforcer,
    private val appRepo: AppRepository
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

    private val _hasOverlayPermission = MutableStateFlow(checkOverlayPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    fun refreshPermissions() {
        _hasUsagePermission.value = checkUsagePermission()
        _hasOverlayPermission.value = checkOverlayPermission()
        android.util.Log.d("BLOCK_DEBUG", "refreshPermissions: usage=${_hasUsagePermission.value}, overlay=${_hasOverlayPermission.value}")
    }

    private fun checkUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = "package:${context.packageName}".toUri()
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
            installedApps = appRepo.getInstalledApps(),
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

    fun saveProfile(
        name: String,
        linkedListId: String?,
        allowedAppIds: Set<String> = emptySet(),
        schedule: FocusSchedule? = null
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val existing = _editingProfile.value
                val profile = if (existing != null) {
                    existing.copy(
                        name = name.trim(),
                        linkedListId = linkedListId,
                        allowedAppIds = allowedAppIds,
                        schedule = schedule
                    )
                } else {
                    FocusProfile(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        linkedListId = linkedListId,
                        allowedAppIds = allowedAppIds,
                        schedule = schedule
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
                AppBlockingState.reset()
                focusRepo.activate(id)
                val profile = focusRepo.getById(id)
                if (profile != null && profile.allowedAppIds.isNotEmpty()) {
                    if (checkUsagePermission()) {
                        AppBlockingService.start(context)
                    } else {
                        _events.emit(FocusEvent.Error(context.getString(R.string.error_usage_permission_required)))
                    }
                }
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể kích hoạt: ${e.message}"))
            }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            try {
                focusRepo.deactivate()
                AppBlockingService.stop(context)
                AppBlockingState.reset()
            } catch (e: Exception) {
                _events.emit(FocusEvent.Error("Không thể tắt: ${e.message}"))
            }
        }
    }

    // ── Timer functions ───────────────────────────────────────────────────────

    fun setFocusMode(focusMode: FocusMode) {
        if (_timerState.value.isRunning) return
        _timerState.update { it.copy(mode = focusMode) }
    }

    fun adjustDuration(deltaMinutes: Int) {
        if (_timerState.value.isRunning) return
        val newDuration = (_timerState.value.durationMinutes + deltaMinutes).coerceIn(1, 120)
        _timerState.update { it.copy(durationMinutes = newDuration, remainingSeconds = newDuration * 60L) }
    }

    fun startFocus() {
        val state = _timerState.value
        if (state.isRunning) return
        AppBlockingState.reset()
        _timerState.update { it.copy(isRunning = true, isFinished = false, remainingSeconds = it.durationMinutes * 60L) }
        timerJob = viewModelScope.launch {
            WidgetUpdater.updateAll(context)
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                _timerState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            _timerState.update { it.copy(isRunning = false, isFinished = true) }
            AppBlockingService.stop(context)
            AppBlockingState.reset()
            val durationMinutes = _timerState.value.durationMinutes
            val profileId = uiState.value.activeProfile?.id
            sessionRepo.recordSession(durationMinutes, profileId)
            sendSessionCompleteNotification()
            WidgetUpdater.updateAll(context)
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
        AppBlockingState.reset()
        viewModelScope.launch { WidgetUpdater.updateAll(context) }
    }

    fun stopFocusAndDeactivate() {
        stopFocus()
        deactivate()
    }

    private fun sendSessionCompleteNotification() {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0xF0C05,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Session hoàn thành! +1 streak 🔥")
            .setContentText("Tốt lắm! Hãy nghỉ ngơi một chút.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(0xF0C05, notification)
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
