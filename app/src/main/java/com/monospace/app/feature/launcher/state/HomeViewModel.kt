package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
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
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val tasks: List<Task> = emptyList(),
        val isSelectionMode: Boolean = false,
        val selectedTaskIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val isMenuExpanded: Boolean = false,
        val showCreateSheet: Boolean = false,
        val showDatePicker: Boolean = false
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val repository: TaskRepository
) : ViewModel() {

    private val _currentListId = MutableStateFlow("default")
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    
    private val _isMenuExpanded = MutableStateFlow(false)
    private val _showCreateSheet = MutableStateFlow(false)
    private val _showDatePicker = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        _currentListId.flatMapLatest { id -> getTasksUseCase(id) },
        _isSelectionMode,
        _selectedTaskIds,
        _searchQuery,
        _isMenuExpanded,
        _showCreateSheet,
        _showDatePicker
    ) { args ->
        val tasks = args[0] as List<Task>
        HomeUiState.Success(
            tasks = tasks,
            isSelectionMode = args[1] as Boolean,
            selectedTaskIds = args[2] as Set<String>,
            searchQuery = args[3] as String,
            isMenuExpanded = args[4] as Boolean,
            showCreateSheet = args[5] as Boolean,
            showDatePicker = args[6] as Boolean
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
                addTaskUseCase(title, _currentListId.value)
                setShowCreateSheet(false)
            } catch (e: Exception) {
                // TODO: Handle error state
            }
        }
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
