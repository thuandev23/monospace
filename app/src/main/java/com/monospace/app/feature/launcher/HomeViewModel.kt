//package com.monospace.app.feature.launcher
//
//import androidx.compose.runtime.mutableStateOf
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.monospace.app.core.database.entity.TaskEntity
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//class HomeViewModel @Inject constructor(
//    private val taskRepo: TaskRepository
//) : ViewModel() {
//    // CreateTaskSheet state — không mất khi sheet đóng/mở
//    var newTaskTitle by mutableStateOf("")
//        private set
//
//    fun onNewTaskTitleChange(value: String) {
//        newTaskTitle = value
//    }
//
//    val uiState: StateFlow<HomeUiState> = taskRepo
//        .observeTasks(listId = "inbox")  // Tạm dùng inbox list
//        .map { tasks ->
//            HomeUiState.Success(
//                activeTasks = tasks.filter { !it.isCompleted },
//                completedTasks = tasks.filter { it.isCompleted }
//            )
//        }
//        .catch { emit(HomeUiState.Error(it.message ?: "Unknown error")) }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)
//
//    fun onTaskToggle(taskId: String, completed: Boolean) {
//        viewModelScope.launch {
//            taskRepo.completeTask(taskId)
//        }
//    }
//
//    fun onCreateTask() {
//        if (newTaskTitle.isBlank()) return
//        viewModelScope.launch {
//            taskRepo.createTask(TaskEntity(title = newTaskTitle, listId = "inbox"))
//            newTaskTitle = ""  // Clear sau khi save
//        }
//    }
//}