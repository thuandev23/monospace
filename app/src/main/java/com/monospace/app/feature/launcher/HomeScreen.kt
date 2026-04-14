package com.monospace.app.feature.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.R
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.feature.launcher.components.CreateTaskSheet
import com.monospace.app.feature.launcher.components.EmptyStateTask
import com.monospace.app.feature.launcher.components.HomeTopBar
import com.monospace.app.feature.launcher.components.MinimalCalendarDialog
import com.monospace.app.feature.launcher.components.TaskList
import com.monospace.app.feature.launcher.state.HomeUiState
import com.monospace.app.feature.launcher.state.HomeViewModel
import com.monospace.app.ui.theme.FocusTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun HomeScreen(
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onToggleTask = viewModel::toggleTask,
        onAddTask = viewModel::addTask,
        onDeleteSelected = viewModel::deleteSelectedTasks,
        onDeleteTask = viewModel::deleteTask,
        onSelectAll = viewModel::selectAll,
        onSetSelectionMode = viewModel::setSelectionMode,
        onToggleTaskSelection = viewModel::toggleTaskSelection,
        onMenuToggle = viewModel::setMenuExpanded,
        onShowCreateSheet = viewModel::setShowCreateSheet,
        onShowDatePicker = viewModel::setShowDatePicker,
        onUpdateDraftSchedule = viewModel::updateDraftSchedule,
        onUpdateDraftListId = viewModel::setDraftListId,
        onSearchQueryChange = viewModel::setSearchQuery,
        onClearSearch = viewModel::clearSearch,
        onPriorityFilterChange = viewModel::setPriorityFilter,
        onNavigateToTask = onNavigateToTask,
        onNavigateToLists = onNavigateToLists,
        initialShowSearch = initialShowSearch
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onToggleTask: (String, Boolean) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteTask: (String) -> Unit = {},
    onSelectAll: () -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onShowCreateSheet: (Boolean) -> Unit,
    onShowDatePicker: (Boolean) -> Unit,
    onUpdateDraftSchedule: (startDate: java.time.Instant?, endDate: java.time.Instant?, isAllDay: Boolean, reminder: ReminderConfig?, repeat: RepeatConfig?) -> Unit,
    onUpdateDraftListId: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onPriorityFilterChange: (Priority?) -> Unit = {},
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FocusTheme.colors.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = FocusTheme.colors.primary,
                    contentColor = FocusTheme.colors.background
                )
            }
        },
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
                        onDeleteTask = onDeleteTask,
                        onSelectAll = onSelectAll,
                        onSetSelectionMode = onSetSelectionMode,
                        onToggleTaskSelection = onToggleTaskSelection,
                        onMenuToggle = onMenuToggle,
                        onShowCreateSheet = onShowCreateSheet,
                        onSearchQueryChange = onSearchQueryChange,
                        onClearSearch = onClearSearch,
                        onPriorityFilterChange = onPriorityFilterChange,
                        onNavigateToTask = onNavigateToTask,
                        onNavigateToLists = onNavigateToLists,
                        initialShowSearch = initialShowSearch
                    )

                    if (uiState.showCreateSheet) {
                        CreateTaskSheet(
                            onDismiss = { onShowCreateSheet(false) },
                            onSave = onAddTask,
                            onTodayClick = { onShowDatePicker(true) },
                            availableLists = uiState.availableLists,
                            currentListId = uiState.draftListId,
                            onListSelected = onUpdateDraftListId
                        )
                    }

                    if (uiState.showDatePicker) {
                        MinimalCalendarDialog(
                            onDismiss = { onShowDatePicker(false) },
                            onConfigSave = { startD, startT, endD, endT, rem, rep ->
                                val startInstant = startD.atTime(startT ?: LocalTime.MIDNIGHT)
                                    .atZone(ZoneId.systemDefault()).toInstant()
                                val endInstant = endD?.atTime(endT ?: LocalTime.MAX)
                                    ?.atZone(ZoneId.systemDefault())?.toInstant()
                                
                                onUpdateDraftSchedule(
                                    startInstant,
                                    endInstant,
                                    startT == null,
                                    rem,
                                    rep
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Tìm task...", color = FocusTheme.colors.secondary) },
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
            }
        },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FocusTheme.colors.primary,
            unfocusedBorderColor = FocusTheme.colors.divider,
            focusedTextColor = FocusTheme.colors.primary,
            unfocusedTextColor = FocusTheme.colors.primary
        )
    )
}

@Composable
private fun SuccessContent(
    state: HomeUiState.Success,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteTask: (String) -> Unit = {},
    onSelectAll: () -> Unit,
    onSetSelectionMode: (Boolean) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onMenuToggle: (Boolean) -> Unit,
    onShowCreateSheet: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onPriorityFilterChange: (Priority?) -> Unit = {},
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false
) {
    val activeTasks = remember(state.tasks) { state.tasks.filter { !it.isCompleted } }
    val completedTasks = remember(state.tasks) { state.tasks.filter { it.isCompleted } }
    var showSearchBar by remember { mutableStateOf(initialShowSearch || state.searchQuery.isNotBlank()) }

    val priorityChips = remember {
        listOf(Priority.HIGH to "Cao", Priority.MEDIUM to "Trung bình", Priority.LOW to "Thấp")
    }

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
            onSelectAll = onSelectAll,
            isMenuExpanded = state.isMenuExpanded,
            onMenuToggle = onMenuToggle,
            onSelectedTasks = {
                onSetSelectionMode(true)
                onMenuToggle(false)
            },
            onNavigateToLists = {
                onMenuToggle(false)
                onNavigateToLists()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar — hiện khi tap icon tìm kiếm hoặc đang có query
        if (showSearchBar) {
            TaskSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClose = {
                    showSearchBar = false
                    onClearSearch()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Priority filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(priorityChips) { (priority, label) ->
                val selected = state.priorityFilter == priority
                FilterChip(
                    selected = selected,
                    onClick = { onPriorityFilterChange(priority) },
                    label = { Text(label, style = FocusTheme.typography.caption) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FocusTheme.colors.primary,
                        selectedLabelColor = FocusTheme.colors.background,
                        containerColor = FocusTheme.colors.background,
                        labelColor = FocusTheme.colors.secondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = FocusTheme.colors.divider,
                        selectedBorderColor = FocusTheme.colors.primary
                    )
                )
            }
        }

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
                    } else {
                        onNavigateToTask(task.id)
                    }
                },
                onTaskLongClick = { task ->
                    if (!state.isSelectionMode) {
                        onSetSelectionMode(true)
                        onToggleTaskSelection(task.id)
                    }
                },
                onTaskSwipeDelete = onDeleteTask
            )
        }
    }
}
