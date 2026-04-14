package com.monospace.app.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

sealed interface TaskDetailUiState {
    object Loading : TaskDetailUiState
    object NotFound : TaskDetailUiState
    data class Editing(
        val id: String,
        val title: String,
        val notes: String,
        val listId: String,
        val priority: Priority,
        val isCompleted: Boolean,
        val startDateTime: Instant?,
        val endDateTime: Instant?,
        val isAllDay: Boolean,
        val reminder: ReminderConfig?,
        val repeat: RepeatConfig?,
        val availableLists: List<com.monospace.app.core.domain.model.TaskList> = emptyList(),
        val isSaving: Boolean = false,
        val showDatePicker: Boolean = false
    ) : TaskDetailUiState
}

sealed interface TaskDetailEvent {
    object SavedAndNavigateBack : TaskDetailEvent
    object DeletedAndNavigateBack : TaskDetailEvent
    data class Error(val message: String) : TaskDetailEvent
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val taskListRepository: TaskListRepository,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _uiState = MutableStateFlow<TaskDetailUiState>(TaskDetailUiState.Loading)
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>()
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            if (taskId.isEmpty()) {
                _uiState.value = TaskDetailUiState.NotFound
                return@launch
            }
            val task = taskRepository.getTaskById(taskId)
            val lists = withTimeoutOrNull(5_000L) {
                taskListRepository.observeAllLists().first()
            } ?: emptyList()
            if (task == null) {
                _uiState.value = TaskDetailUiState.NotFound
                return@launch
            }
            _uiState.value = TaskDetailUiState.Editing(
                id = task.id,
                title = task.title,
                notes = task.notes ?: "",
                listId = task.listId,
                priority = task.priority,
                isCompleted = task.isCompleted,
                startDateTime = task.startDateTime,
                endDateTime = task.endDateTime,
                isAllDay = task.isAllDay,
                reminder = task.reminder,
                repeat = task.repeat,
                availableLists = lists
            )
        }
    }

    fun onTitleChange(value: String) = updateEditing { copy(title = value) }
    fun onNotesChange(value: String) = updateEditing { copy(notes = value) }
    fun onListIdChange(value: String) = updateEditing { copy(listId = value) }
    fun onPriorityChange(value: Priority) = updateEditing { copy(priority = value) }
    fun onShowDatePicker(show: Boolean) = updateEditing { copy(showDatePicker = show) }

    fun onScheduleChange(
        start: Instant?,
        end: Instant?,
        isAllDay: Boolean,
        reminder: ReminderConfig?,
        repeat: RepeatConfig?
    ) = updateEditing {
        copy(
            startDateTime = start,
            endDateTime = end,
            isAllDay = isAllDay,
            reminder = reminder,
            repeat = repeat,
            showDatePicker = false
        )
    }

    fun saveTask() {
        val state = _uiState.value as? TaskDetailUiState.Editing ?: return
        if (state.title.isBlank()) {
            viewModelScope.launch { _events.emit(TaskDetailEvent.Error("Tiêu đề không được để trống")) }
            return
        }
        viewModelScope.launch {
            updateEditing { copy(isSaving = true) }
            try {
                val updated = Task(
                    id = state.id,
                    title = state.title.trim(),
                    notes = state.notes.trim().ifBlank { null },
                    listId = state.listId,
                    priority = state.priority,
                    startDateTime = state.startDateTime,
                    endDateTime = state.endDateTime,
                    isAllDay = state.isAllDay,
                    reminder = state.reminder,
                    repeat = state.repeat,
                    isCompleted = state.isCompleted
                )
                updateTaskUseCase(updated)
                _events.emit(TaskDetailEvent.SavedAndNavigateBack)
            } catch (e: Exception) {
                updateEditing { copy(isSaving = false) }
                _events.emit(TaskDetailEvent.Error("Không thể lưu: ${e.message}"))
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
                _events.emit(TaskDetailEvent.DeletedAndNavigateBack)
            } catch (e: Exception) {
                _events.emit(TaskDetailEvent.Error("Không thể xóa: ${e.message}"))
            }
        }
    }

    private inline fun updateEditing(block: TaskDetailUiState.Editing.() -> TaskDetailUiState.Editing) {
        val current = _uiState.value as? TaskDetailUiState.Editing ?: return
        _uiState.value = current.block()
    }
}
