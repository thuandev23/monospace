package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.usecase.AddTaskUseCase
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val tasks: List<Task> = emptyList(),
        val availableLists: List<TaskList> = emptyList(),
        val isSelectionMode: Boolean = false,
        val selectedTaskIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val isMenuExpanded: Boolean = false,
        val showCreateSheet: Boolean = false,
        val showDatePicker: Boolean = false,

        // Task Creation State
        val draftListId: String = "default",
        val draftStartDateTime: Instant? = null,
        val draftEndDateTime: Instant? = null,
        val draftIsAllDay: Boolean = true,
        val draftReminder: ReminderConfig? = null,
        val draftRepeat: RepeatConfig? = null
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val repository: TaskRepository,
    private val taskListRepository: TaskListRepository
) : ViewModel() {

    private val _currentListId = MutableStateFlow("default")
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")

    private val _isMenuExpanded = MutableStateFlow(false)
    private val _showCreateSheet = MutableStateFlow(false)
    private val _showDatePicker = MutableStateFlow(false)

    // Draft State
    private val _draftListId = MutableStateFlow("default")
    private val _draftStartDateTime = MutableStateFlow<Instant?>(null)
    private val _draftEndDateTime = MutableStateFlow<Instant?>(null)
    private val _draftIsAllDay = MutableStateFlow(true)
    private val _draftReminder = MutableStateFlow<ReminderConfig?>(null)
    private val _draftRepeat = MutableStateFlow<RepeatConfig?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        _currentListId.flatMapLatest { id -> getTasksUseCase(id) },
        taskListRepository.observeAllLists(),
        _isSelectionMode,
        _selectedTaskIds,
        _searchQuery,
        _isMenuExpanded,
        _showCreateSheet,
        _showDatePicker,
        _draftListId,
        _draftStartDateTime,
        _draftEndDateTime,
        _draftIsAllDay,
        _draftReminder,
        _draftRepeat
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState.Success(
            tasks = args[0] as List<Task>,
            availableLists = args[1] as List<TaskList>,
            isSelectionMode = args[2] as Boolean,
            selectedTaskIds = args[3] as Set<String>,
            searchQuery = args[4] as String,
            isMenuExpanded = args[5] as Boolean,
            showCreateSheet = args[6] as Boolean,
            showDatePicker = args[7] as Boolean,
            draftListId = args[8] as String,
            draftStartDateTime = args[9] as Instant?,
            draftEndDateTime = args[10] as Instant?,
            draftIsAllDay = args[11] as Boolean,
            draftReminder = args[12] as ReminderConfig?,
            draftRepeat = args[13] as RepeatConfig?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    isCompleted = false,
                    listId = _draftListId.value,
                    syncStatus = SyncStatus.PENDING_CREATE,
                    priority = Priority.NONE,
                    startDateTime = _draftStartDateTime.value,
                    endDateTime = _draftEndDateTime.value,
                    isAllDay = _draftIsAllDay.value,
                    reminder = _draftReminder.value,
                    repeat = _draftRepeat.value
                )
                addTaskUseCase(task)
                resetDraft()
                setShowCreateSheet(false)
            } catch (e: Exception) {
                // TODO: Handle error state
            }
        }
    }

    private fun resetDraft() {
        _draftListId.value = "default"
        _draftStartDateTime.value = null
        _draftEndDateTime.value = null
        _draftIsAllDay.value = true
        _draftReminder.value = null
        _draftRepeat.value = null
    }

    fun updateDraftSchedule(
        start: Instant?,
        end: Instant?,
        isAllDay: Boolean,
        reminder: ReminderConfig?,
        repeat: RepeatConfig?
    ) {
        _draftStartDateTime.value = start
        _draftEndDateTime.value = end
        _draftIsAllDay.value = isAllDay
        _draftReminder.value = reminder
        _draftRepeat.value = repeat
    }

    fun setDraftListId(listId: String) {
        _draftListId.value = listId
    }

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleTaskUseCase(taskId, isCompleted)
        }
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch {
            _selectedTaskIds.value.forEach { id -> repository.deleteTask(id) }
            setSelectionMode(false)
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) _selectedTaskIds.value = emptySet()
    }

    fun toggleTaskSelection(taskId: String) {
        val currentSelected = _selectedTaskIds.value
        _selectedTaskIds.value = if (currentSelected.contains(taskId)) {
            currentSelected - taskId
        } else {
            currentSelected + taskId
        }
    }

    fun setMenuExpanded(expanded: Boolean) = run { _isMenuExpanded.value = expanded }
    fun setShowCreateSheet(show: Boolean) = run { _showCreateSheet.value = show }
    fun setShowDatePicker(show: Boolean) = run { _showDatePicker.value = show }
}
