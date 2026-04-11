package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.AddTaskUseCase
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val repository: TaskRepository
) : ViewModel() {

    // Lấy danh sách task từ list mặc định (ví dụ ID là "default")
    val uiState: StateFlow<List<Task>> = getTasksUseCase("default")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                addTaskUseCase(title, "default")
            }
        }
    }

    fun toggleTask(taskId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            toggleTaskUseCase(taskId, currentStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }
}