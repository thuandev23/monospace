package com.monospace.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.ListIds
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskListRepository
import com.monospace.app.core.domain.repository.TaskRepository
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
import java.util.UUID
import javax.inject.Inject

data class TaskListUiState(
    val lists: List<TaskList> = emptyList(),
    val allTaskCount: Int = 0,
    val todayTaskCount: Int = 0,
    val isEditMode: Boolean = false,
    val showCreateDialog: Boolean = false,
    val editingList: TaskList? = null
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskListRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    private val _editingList = MutableStateFlow<TaskList?>(null)
    private val _isEditMode = MutableStateFlow(false)

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _successEvent = MutableSharedFlow<String>()
    val successEvent: SharedFlow<String> = _successEvent.asSharedFlow()

    private val _baseListState = combine(
        repository.observeAllLists(),
        taskRepository.observeAllActiveTaskCount(),
        taskRepository.observeTodayTaskCount()
    ) { lists, allCount, todayCount ->
        Triple(lists, allCount, todayCount)
    }

    val uiState: StateFlow<TaskListUiState> = combine(
        _baseListState,
        _isEditMode,
        _showCreateDialog,
        _editingList
    ) { (lists, allCount, todayCount), editMode, showDialog, editing ->
        TaskListUiState(
            lists = lists,
            allTaskCount = allCount,
            todayTaskCount = todayCount,
            isEditMode = editMode,
            showCreateDialog = showDialog,
            editingList = editing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState()
    )

    fun enterEditMode() { _isEditMode.value = true }
    fun exitEditMode() { _isEditMode.value = false }

    fun showCreateDialog() { _showCreateDialog.value = true }
    fun hideCreateDialog() { _showCreateDialog.value = false }

    fun startEdit(list: TaskList) { _editingList.value = list }
    fun cancelEdit() { _editingList.value = null }

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val lists = uiState.value.lists
                val maxOrder = lists.maxOfOrNull { it.sortOrder } ?: 0
                repository.saveList(
                    TaskList(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        syncStatus = SyncStatus.PENDING_CREATE,
                        sortOrder = maxOrder + 1
                    )
                )
                hideCreateDialog()
                _successEvent.emit("Folder \"${name.trim()}\" đã được tạo")
            } catch (e: Exception) {
                _errorEvent.emit("Không thể tạo danh sách: ${e.message}")
            }
        }
    }

    fun renameList(list: TaskList, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                repository.saveList(list.copy(name = newName.trim(), syncStatus = SyncStatus.PENDING_UPDATE))
                cancelEdit()
                _successEvent.emit("Đã đổi tên thành \"${newName.trim()}\"")
            } catch (e: Exception) {
                _errorEvent.emit("Không thể đổi tên: ${e.message}")
            }
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteList(id)
                _successEvent.emit("Đã xóa danh sách")
            } catch (e: Exception) {
                _errorEvent.emit("Không thể xóa danh sách: ${e.message}")
            }
        }
    }

    fun moveListUp(list: TaskList) {
        viewModelScope.launch {
            try {
                val lists = uiState.value.lists.filter { it.id != ListIds.DEFAULT }
                val idx = lists.indexOfFirst { it.id == list.id }
                if (idx <= 0) return@launch
                val prev = lists[idx - 1]
                repository.updateSortOrder(list.id, prev.sortOrder)
                repository.updateSortOrder(prev.id, list.sortOrder)
            } catch (e: Exception) {
                _errorEvent.emit("Lỗi sắp xếp: ${e.message}")
            }
        }
    }

    fun moveListDown(list: TaskList) {
        viewModelScope.launch {
            try {
                val lists = uiState.value.lists.filter { it.id != ListIds.DEFAULT }
                val idx = lists.indexOfFirst { it.id == list.id }
                if (idx < 0 || idx >= lists.size - 1) return@launch
                val next = lists[idx + 1]
                repository.updateSortOrder(list.id, next.sortOrder)
                repository.updateSortOrder(next.id, list.sortOrder)
            } catch (e: Exception) {
                _errorEvent.emit("Lỗi sắp xếp: ${e.message}")
            }
        }
    }
}
