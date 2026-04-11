package com.monospace.app.feature.launcher.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.usecase.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LauncherViewModel @Inject constructor(
    getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    // Lấy danh sách task từ list mặc định (ví dụ ID là "default")
    val uiState: StateFlow<List<Task>> = getTasksUseCase("default")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}