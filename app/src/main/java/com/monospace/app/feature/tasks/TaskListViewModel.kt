package com.monospace.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.repository.TaskListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskListUiState(
    val lists: List<TaskList> = emptyList(),
    val showCreateDialog: Boolean = false,
    val editingList: TaskList? = null   // null = không đang edit
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskListRepository
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    private val _editingList = MutableStateFlow<TaskList?>(null)
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    val uiState = combine(
        repository.observeAllLists(),
        _showCreateDialog,
        _editingList
    ) { lists, showDialog, editing ->
        TaskListUiState(
            lists = lists,
            showCreateDialog = showDialog,
            editingList = editing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState()
    )

    fun showCreateDialog() { _showCreateDialog.value = true }
    fun hideCreateDialog() { _showCreateDialog.value = false }

    fun startEdit(list: TaskList) { _editingList.value = list }
    fun cancelEdit() { _editingList.value = null }

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.saveList(
                    TaskList(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        syncStatus = SyncStatus.PENDING_CREATE
                    )
                )
                hideCreateDialog()
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
            } catch (e: Exception) {
                _errorEvent.emit("Không thể đổi tên: ${e.message}")
            }
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteList(id)
            } catch (e: Exception) {
                _errorEvent.emit("Không thể xóa danh sách: ${e.message}")
            }
        }
    }
}
