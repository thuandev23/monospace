package com.monospace.app.feature.upcoming

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.domain.usecase.DeleteTaskUseCase
import com.monospace.app.core.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class UpcomingGroupType { OVERDUE, TODAY, TOMORROW, THIS_WEEK, LATER, NO_DATE }

data class UpcomingGroup(
    val type: UpcomingGroupType,
    val tasks: List<Task>
)

sealed interface UpcomingUiState {
    object Loading : UpcomingUiState
    data class Success(
        val groups: List<UpcomingGroup>,
        val completedTasks: List<Task>,
        val showCompleted: Boolean = false
    ) : UpcomingUiState
}

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    val uiState: StateFlow<UpcomingUiState> = combine(
        taskRepository.observeAllTasksSortedByDate(),
        _showCompleted
    ) { tasks, showCompleted ->
        val today = LocalDate.now(ZoneId.systemDefault())
        val activeTasks = tasks.filter { !it.isCompleted }
        val completedTasks = tasks.filter { it.isCompleted }

        val groups = buildGroups(activeTasks, today)

        UpcomingUiState.Success(
            groups = groups,
            completedTasks = completedTasks,
            showCompleted = showCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UpcomingUiState.Loading
    )

    fun toggleTask(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskUseCase(taskId, isCompleted)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể cập nhật: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(taskId)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể xóa task: ${e.message}")
            }
        }
    }

    fun toggleShowCompleted() {
        _showCompleted.value = !_showCompleted.value
    }

    private fun buildGroups(tasks: List<Task>, today: LocalDate): List<UpcomingGroup> {
        val tomorrow = today.plusDays(1)
        val endOfWeek = today.plusDays(7)

        val overdue = mutableListOf<Task>()
        val todayTasks = mutableListOf<Task>()
        val tomorrowTasks = mutableListOf<Task>()
        val thisWeekTasks = mutableListOf<Task>()
        val laterTasks = mutableListOf<Task>()
        val noDatTasks = mutableListOf<Task>()

        for (task in tasks) {
            val date = task.startDateTime?.toLocalDate()
            when {
                date == null -> noDatTasks.add(task)
                date.isBefore(today) -> overdue.add(task)
                date == today -> todayTasks.add(task)
                date == tomorrow -> tomorrowTasks.add(task)
                date.isBefore(endOfWeek) -> thisWeekTasks.add(task)
                else -> laterTasks.add(task)
            }
        }

        return buildList {
            if (overdue.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.OVERDUE, overdue))
            if (todayTasks.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.TODAY, todayTasks))
            if (tomorrowTasks.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.TOMORROW, tomorrowTasks))
            if (thisWeekTasks.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.THIS_WEEK, thisWeekTasks))
            if (laterTasks.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.LATER, laterTasks))
            if (noDatTasks.isNotEmpty()) add(UpcomingGroup(UpcomingGroupType.NO_DATE, noDatTasks))
        }
    }
}

private fun Instant.toLocalDate(): LocalDate =
    atZone(ZoneId.systemDefault()).toLocalDate()
