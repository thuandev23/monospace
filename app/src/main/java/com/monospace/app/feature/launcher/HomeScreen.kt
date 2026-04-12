package com.monospace.app.feature.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.R
import com.monospace.app.feature.launcher.components.CreateTaskSheet
import com.monospace.app.feature.launcher.components.EmptyStateTask
import com.monospace.app.feature.launcher.components.HomeTopBar
import com.monospace.app.feature.launcher.components.MinimalCalendarDialog
import com.monospace.app.feature.launcher.components.TaskList
import com.monospace.app.feature.launcher.state.HomeUiState
import com.monospace.app.feature.launcher.state.HomeViewModel
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onToggleTask = viewModel::toggleTask,
        onAddTask = viewModel::addTask,
        onDeleteSelected = viewModel::deleteSelectedTasks,
        onSetSelectionMode = viewModel::setSelectionMode,
        onToggleTaskSelection = viewModel::toggleTaskSelection,
        onMenuToggle = viewModel::setMenuExpanded,
        onShowCreateSheet = viewModel::setShowCreateSheet,
        onShowDatePicker = viewModel::setShowDatePicker
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onToggleTask: (String, Boolean) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onShowCreateSheet: (Boolean) -> Unit,
    onShowDatePicker: (Boolean) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FocusTheme.colors.background,
        floatingActionButton = {
            if (uiState is HomeUiState.Success && !uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { onShowCreateSheet(true) },
                    containerColor = FocusTheme.colors.primary,
                    contentColor = FocusTheme.colors.background,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.content_desc_add_task),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = FocusTheme.colors.primary
                    )
                }

                is HomeUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = FocusTheme.colors.destructive
                    )
                }

                is HomeUiState.Success -> {
                    SuccessContent(
                        state = uiState,
                        onToggleTask = onToggleTask,
                        onDeleteSelected = onDeleteSelected,
                        onSetSelectionMode = onSetSelectionMode,
                        onToggleTaskSelection = onToggleTaskSelection,
                        onMenuToggle = onMenuToggle,
                        onShowCreateSheet = onShowCreateSheet
                    )

                    if (uiState.showCreateSheet) {
                        CreateTaskSheet(
                            onDismiss = { onShowCreateSheet(false) },
                            onSave = onAddTask,
                            onTodayClick = { onShowDatePicker(true) }
                        )
                    }

                    if (uiState.showDatePicker) {
                        MinimalCalendarDialog(
                            onDismiss = { onShowDatePicker(false) },
                            onDateSelected = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: HomeUiState.Success,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onShowCreateSheet: (Boolean) -> Unit
) {
    val activeTasks = remember(state.tasks) { state.tasks.filter { !it.isCompleted } }
    val completedTasks = remember(state.tasks) { state.tasks.filter { it.isCompleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        HomeTopBar(
            isSelectionMode = state.isSelectionMode,
            selectedCount = state.selectedTaskIds.size,
            onExitSelection = { onSetSelectionMode(false) },
            onDeleteSelected = onDeleteSelected,
            isMenuExpanded = state.isMenuExpanded,
            onMenuToggle = onMenuToggle,
            onSelectedTasks = {
                onSetSelectionMode(true)
                onMenuToggle(false)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.tasks.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            EmptyStateTask(onCreateClick = { onShowCreateSheet(true) })
            Spacer(modifier = Modifier.weight(1.5f))
        } else {
            TaskList(
                activeTasks = activeTasks,
                completedTasks = completedTasks,
                isSelectionMode = state.isSelectionMode,
                selectedTaskIds = state.selectedTaskIds,
                onTaskToggle = onToggleTask,
                onTaskClick = { task ->
                    if (state.isSelectionMode) {
                        onToggleTaskSelection(task.id)
                    }
                },
                onTaskLongClick = { task ->
                    if (!state.isSelectionMode) {
                        onSetSelectionMode(true)
                        onToggleTaskSelection(task.id)
                    }
                }
            )
        }
    }
}
