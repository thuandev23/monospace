package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.AppInfo
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.AppRepository
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.AddTaskUseCase
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LauncherUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val repository: TaskRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _currentListId = MutableStateFlow("default")
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LauncherUiState> = combine(
        _currentListId.flatMapLatest { id -> getTasksUseCase(id) },
        _isSelectionMode,
        _selectedTaskIds,
        _searchQuery,
        _isLoading
    ) { tasks, isSelectionMode, selectedIds, query, loading ->
        LauncherUiState(
            tasks = tasks,
            isSelectionMode = isSelectionMode,
            selectedTaskIds = selectedIds,
            searchQuery = query,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState(isLoading = true)
    )

    val filteredApps = combine(
        _searchQuery,
        MutableStateFlow(appRepository.getInstalledApps())
    ) { query, allApps ->
        if (query.isBlank()) allApps else allApps.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                addTaskUseCase(title, _currentListId.value)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    /**
     * Cập nhật trạng thái hoàn thành của task.
     * @param taskId ID của task.
     * @param isCompleted Trạng thái hoàn thành mới.
     */
    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskUseCase(taskId, isCompleted)
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
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
    
    fun deleteSelectedTasks() {
        viewModelScope.launch {
            _selectedTaskIds.value.forEach { id ->
                repository.deleteTask(id)
            }
            setSelectionMode(false)
        }
    }
}
