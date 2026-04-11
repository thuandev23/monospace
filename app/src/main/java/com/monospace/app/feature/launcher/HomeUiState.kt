//package com.monospace.app.feature.launcher
//
//sealed interface HomeUiState {
//    object Loading : HomeUiState
//    data class Error(val message: String) : HomeUiState
//    data class Success(
//        val activeTasks: List<Task>,
//        val completedTasks: List<Task>,
//        val selectedListName: String = "Today",
//        val taskDate: LocalDate = LocalDate.now()
//    ) : HomeUiState
//}