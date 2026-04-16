package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.GroupOption
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.SortOption
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.model.ViewSettings
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.AddTaskUseCase
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import com.monospace.app.core.domain.usecase.UpdateTaskUseCase
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        val priorityFilter: Priority? = null,
        val viewSettings: ViewSettings = ViewSettings(),
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
    savedStateHandle: SavedStateHandle,
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val taskRepository: TaskRepository,
    private val taskListRepository: TaskListRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    // One-shot events → hiển thị Snackbar ở UI
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _successEvent = MutableSharedFlow<String>()
    val successEvent: SharedFlow<String> = _successEvent.asSharedFlow()

    private val _currentListId = MutableStateFlow(
        savedStateHandle.get<String>("listId") ?: "default"
    )
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")

    private val _priorityFilter = MutableStateFlow<Priority?>(null)
    private val _isMenuExpanded = MutableStateFlow(false)
    private val _showCreateSheet = MutableStateFlow(false)
    private val _showDatePicker = MutableStateFlow(false)

    // Draft State — mặc định theo list đang xem (virtual listId → về default)
    private val _draftListId = MutableStateFlow(
        savedStateHandle.get<String>("listId")
            ?.takeIf { it != "all" && it != "today" }
            ?: "default"
    )
    private val _draftStartDateTime = MutableStateFlow<Instant?>(null)
    private val _draftEndDateTime = MutableStateFlow<Instant?>(null)
    private val _draftIsAllDay = MutableStateFlow(true)
    private val _draftReminder = MutableStateFlow<ReminderConfig?>(null)
    private val _draftRepeat = MutableStateFlow<RepeatConfig?>(null)

    // View Settings (persisted via DataStore)
    private val _viewSettings = settingsDataStore.viewSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ViewSettings())

    // Intermediate data classes to avoid type-unsafe 14-arg combine
    private data class TaskListState(
        val tasks: List<Task>,
        val availableLists: List<TaskList>,
        val isSelectionMode: Boolean,
        val selectedTaskIds: Set<String>,
        val searchQuery: String
    )

    private data class UiSheetState(
        val isMenuExpanded: Boolean,
        val showCreateSheet: Boolean,
        val showDatePicker: Boolean
    )

    private data class DraftState(
        val listId: String,
        val startDateTime: Instant?,
        val endDateTime: Instant?,
        val isAllDay: Boolean,
        val reminder: ReminderConfig?,
        val repeat: RepeatConfig?
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _taskListState = combine(
        _currentListId.flatMapLatest { id -> getTasksUseCase(id) },
        taskListRepository.observeAllLists(),
        _isSelectionMode,
        _selectedTaskIds,
        _searchQuery
    ) { allTasks, lists, selection, selectedIds, query ->
        val filtered = if (query.isBlank()) allTasks
            else allTasks.filter { it.title.contains(query, true) || it.notes?.contains(query, true) == true }
        TaskListState(filtered, lists, selection, selectedIds, query)
    }

    private val _sheetState = combine(
        _isMenuExpanded, _showCreateSheet, _showDatePicker
    ) { menu, create, date -> UiSheetState(menu, create, date) }

    // 6 flows → split into 2 nested combines (max 5 args each)
    private val _draftDateTime = combine(
        _draftStartDateTime, _draftEndDateTime, _draftIsAllDay
    ) { start, end, allDay -> Triple(start, end, allDay) }

    private val _draftState = combine(
        _draftListId, _draftDateTime, _draftReminder, _draftRepeat
    ) { listId, dt, reminder, repeat ->
        DraftState(listId, dt.first, dt.second, dt.third, reminder, repeat)
    }

    private val _baseState = combine(
        _taskListState, _sheetState, _draftState
    ) { tl, sheet, draft ->
        HomeUiState.Success(
            tasks = tl.tasks,
            availableLists = tl.availableLists,
            isSelectionMode = tl.isSelectionMode,
            selectedTaskIds = tl.selectedTaskIds,
            searchQuery = tl.searchQuery,
            isMenuExpanded = sheet.isMenuExpanded,
            showCreateSheet = sheet.showCreateSheet,
            showDatePicker = sheet.showDatePicker,
            draftListId = draft.listId,
            draftStartDateTime = draft.startDateTime,
            draftEndDateTime = draft.endDateTime,
            draftIsAllDay = draft.isAllDay,
            draftReminder = draft.reminder,
            draftRepeat = draft.repeat
        )
    }

    private val _withPriority = combine(_baseState, _priorityFilter) { state, filter ->
        if (state is HomeUiState.Success) {
            val filtered = if (filter != null) state.tasks.filter { it.priority == filter }
                           else state.tasks
            state.copy(tasks = filtered, priorityFilter = filter)
        } else state
    }

    val uiState: StateFlow<HomeUiState> = combine(_withPriority, _viewSettings) { state, settings ->
        if (state is HomeUiState.Success) {
            var tasks = state.tasks

            // Apply visibility filters
            if (!settings.showCompleted) tasks = tasks.filter { it.status != TaskStatus.DONE }
            if (!settings.showInProgress) tasks = tasks.filter { it.status != TaskStatus.IN_PROGRESS }
            if (!settings.showOverdue) {
                val nowMs = System.currentTimeMillis()
                tasks = tasks.filter { task ->
                    task.startDateTime == null || task.startDateTime.toEpochMilli() >= nowMs
                        || task.status == TaskStatus.DONE
                }
            }

            // Apply sort
            tasks = when (settings.sortBy) {
                SortOption.NAME -> tasks.sortedBy { it.title.lowercase() }
                SortOption.DATE -> tasks.sortedWith(
                    compareBy(nullsLast()) { it.startDateTime }
                )
                SortOption.FOLDER -> tasks.sortedBy { it.listId }
                SortOption.MANUAL -> tasks // DB order
            }

            state.copy(tasks = tasks, viewSettings = settings)
        } else state
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
                    status = TaskStatus.NOT_DONE,
                    listId = _draftListId.value,
                    syncStatus = SyncStatus.PENDING_CREATE,
                    priority = Priority.NONE,
                    startDateTime = _draftStartDateTime.value
                    ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDateTime = _draftEndDateTime.value,
                    isAllDay = _draftIsAllDay.value,
                    reminder = _draftReminder.value,
                    repeat = _draftRepeat.value
                )
                addTaskUseCase(task)
                resetDraft()
                setShowCreateSheet(false)
                _successEvent.emit("Task đã được tạo")
            } catch (e: Exception) {
                _errorEvent.emit("Không thể thêm task: ${e.message}")
            }
        }
    }

    private fun resetDraft() {
        // Reset về list hiện tại đang xem (virtual listId → về default)
        val currentList = _currentListId.value
        _draftListId.value = if (currentList == "all" || currentList == "today") "default" else currentList
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
            try {
                toggleTaskUseCase(taskId, isCompleted)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể cập nhật task: ${e.message}")
            }
        }
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch {
            try {
                _selectedTaskIds.value.forEach { id -> deleteTaskUseCase(id) }
                setSelectionMode(false)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể xóa task: ${e.message}")
            }
        }
    }

    fun selectAll() {
        val currentState = uiState.value
        if (currentState is HomeUiState.Success) {
            _selectedTaskIds.value = currentState.tasks.map { it.id }.toSet()
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

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể xóa task: ${e.message}")
            }
        }
    }

    fun setPriorityFilter(priority: Priority?) {
        _priorityFilter.value = if (_priorityFilter.value == priority) null else priority
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun markSelectedTasksDone() {
        viewModelScope.launch {
            try {
                _selectedTaskIds.value.forEach { id -> toggleTaskUseCase(id, true) }
                setSelectionMode(false)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể cập nhật task: ${e.message}")
            }
        }
    }

    fun moveSelectedTasksToList(listId: String) {
        viewModelScope.launch {
            try {
                _selectedTaskIds.value.forEach { id ->
                    val task = taskRepository.getTaskById(id) ?: return@forEach
                    updateTaskUseCase(task.copy(listId = listId))
                }
                setSelectionMode(false)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể di chuyển task: ${e.message}")
            }
        }
    }

    fun rescheduleSelectedTasks(
        start: Instant?,
        end: Instant?,
        isAllDay: Boolean,
        reminder: ReminderConfig?,
        repeat: RepeatConfig?
    ) {
        viewModelScope.launch {
            try {
                _selectedTaskIds.value.forEach { id ->
                    val task = taskRepository.getTaskById(id) ?: return@forEach
                    updateTaskUseCase(
                        task.copy(
                            startDateTime = start,
                            endDateTime = end,
                            isAllDay = isAllDay,
                            reminder = reminder,
                            repeat = repeat
                        )
                    )
                }
                setSelectionMode(false)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể reschedule task: ${e.message}")
            }
        }
    }

    fun setViewSettings(settings: ViewSettings) {
        viewModelScope.launch {
            settingsDataStore.setViewSettings(settings)
        }
    }
}
