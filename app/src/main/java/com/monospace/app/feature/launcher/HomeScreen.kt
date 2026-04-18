package com.monospace.app.feature.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.feature.settings.WallpaperCanvas
import com.monospace.app.R
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.feature.focus.FocusViewModel
import com.monospace.app.feature.launcher.components.AddAppShortcutSheet
import com.monospace.app.feature.launcher.components.ConfirmDeleteDialog
import com.monospace.app.feature.launcher.components.ConfirmMarkDoneDialog
import com.monospace.app.feature.launcher.components.CreateTaskSheet
import com.monospace.app.feature.launcher.components.EmptyStateTask
import com.monospace.app.feature.launcher.components.FocusSessionSheet
import com.monospace.app.feature.launcher.components.HomeTopBar
import com.monospace.app.feature.launcher.components.LauncherShortcutsSection
import com.monospace.app.feature.launcher.components.MinimalCalendarDialog
import com.monospace.app.feature.launcher.components.MoveToFolderSheet
import com.monospace.app.feature.launcher.components.RescheduleSheet
import com.monospace.app.feature.launcher.components.SelectionActionBar
import com.monospace.app.feature.launcher.components.TaskList
import com.monospace.app.feature.launcher.state.HomeUiState
import com.monospace.app.feature.launcher.state.HomeViewModel
import com.monospace.app.ui.theme.FocusTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun HomeScreen(
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
    focusViewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timerState by focusViewModel.timerState.collectAsState()
    val hasUsagePermission by focusViewModel.hasUsagePermission.collectAsState()
    val taskDisplaySettings by viewModel.taskDisplaySettings.collectAsState()
    val generalSettings by viewModel.generalSettings.collectAsState()
    val effectiveDisplaySettings = taskDisplaySettings.copy(secondStatus = generalSettings.secondStatus)
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(true) }

    val wallpaperConfig by viewModel.wallpaperConfig.collectAsState()
    val wallpaperTasks by viewModel.wallpaperTasks.collectAsState()
    var showLauncher by remember { mutableStateOf(false) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(wallpaperConfig.showOnHome) {
        if (!wallpaperConfig.showOnHome) showLauncher = true
    }

    BackHandler(enabled = wallpaperConfig.showOnHome && showLauncher) {
        showLauncher = false
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarIsError = true
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.successEvent.collect { message ->
            snackbarIsError = false
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            snackbarIsError = snackbarIsError,
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
            onViewSettingsChange = viewModel::setViewSettings,
            onMarkSelectedDone = viewModel::markSelectedTasksDone,
            onMoveSelectedToList = viewModel::moveSelectedTasksToList,
            onRescheduleSelected = viewModel::rescheduleSelectedTasks,
            onNavigateToTask = onNavigateToTask,
            onNavigateToLists = onNavigateToLists,
            initialShowSearch = initialShowSearch,
            focusTimerState = timerState,
            hasUsagePermission = hasUsagePermission,
            onSetFocusMode = focusViewModel::setFocusMode,
            onAdjustFocusDuration = focusViewModel::adjustDuration,
            onStartFocus = focusViewModel::startFocus,
            onStopFocus = focusViewModel::stopFocus,
            onOpenUsageSettings = {
                focusViewModel.openUsageSettings()
            },
            onRefreshUsagePermission = focusViewModel::refreshPermissions,
            taskDisplaySettings = effectiveDisplaySettings,
            reverseScrollDirection = generalSettings.reverseScrollDirection,
            onSetTaskStatus = viewModel::setTaskStatus
        )

        AnimatedVisibility(
            visible = wallpaperConfig.showOnHome && !showLauncher,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(350)) { -it } + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onVerticalDrag = { _, delta ->
                                dragAccumulator += delta
                                if (dragAccumulator < -120f) showLauncher = true
                            },
                            onDragEnd = { dragAccumulator = 0f },
                            onDragCancel = { dragAccumulator = 0f }
                        )
                    }
            ) {
                WallpaperCanvas(
                    config = wallpaperConfig,
                    tasks = wallpaperTasks,
                    live = true,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = runCatching {
                                    Color(android.graphics.Color.parseColor(wallpaperConfig.textColorHex))
                                }.getOrDefault(Color.White).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarIsError: Boolean = true,
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
    onViewSettingsChange: (com.monospace.app.core.domain.model.ViewSettings) -> Unit = {},
    onMarkSelectedDone: () -> Unit = {},
    onMoveSelectedToList: (String) -> Unit = {},
    onRescheduleSelected: (java.time.Instant?, java.time.Instant?, Boolean, ReminderConfig?, RepeatConfig?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false,
    focusTimerState: com.monospace.app.feature.focus.FocusTimerState = com.monospace.app.feature.focus.FocusTimerState(),
    hasUsagePermission: Boolean = true,
    onSetFocusMode: (com.monospace.app.feature.focus.FocusMode) -> Unit = {},
    onAdjustFocusDuration: (Int) -> Unit = {},
    onStartFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {},
    onOpenUsageSettings: () -> Unit = {},
    onRefreshUsagePermission: () -> Unit = {},
    taskDisplaySettings: com.monospace.app.core.domain.model.TaskDisplaySettings = com.monospace.app.core.domain.model.TaskDisplaySettings(),
    reverseScrollDirection: Boolean = false,
    onSetTaskStatus: ((String, com.monospace.app.core.domain.model.TaskStatus) -> Unit)? = null
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FocusTheme.colors.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (snackbarIsError) FocusTheme.colors.destructive else FocusTheme.colors.success,
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
                        onViewSettingsChange = onViewSettingsChange,
                        onMarkSelectedDone = onMarkSelectedDone,
                        onMoveSelectedToList = onMoveSelectedToList,
                        onRescheduleSelected = onRescheduleSelected,
                        onNavigateToTask = onNavigateToTask,
                        onNavigateToLists = onNavigateToLists,
                        initialShowSearch = initialShowSearch,
                        focusTimerState = focusTimerState,
                        hasUsagePermission = hasUsagePermission,
                        onSetFocusMode = onSetFocusMode,
                        onAdjustFocusDuration = onAdjustFocusDuration,
                        onStartFocus = onStartFocus,
                        onStopFocus = onStopFocus,
                        onOpenUsageSettings = onOpenUsageSettings,
                        onRefreshUsagePermission = onRefreshUsagePermission,
                        taskDisplaySettings = taskDisplaySettings,
                        reverseScrollDirection = reverseScrollDirection,
                        onSetTaskStatus = onSetTaskStatus
                    )

                    if (uiState.showCreateSheet) {
                        CreateTaskSheet(
                            onDismiss = { onShowCreateSheet(false) },
                            onSave = onAddTask,
                            onTodayClick = { onShowDatePicker(true) },
                            availableLists = uiState.availableLists,
                            currentListId = uiState.draftListId,
                            onListSelected = onUpdateDraftListId,
                            draftStartDate = uiState.draftStartDateTime,
                            draftIsAllDay = uiState.draftIsAllDay
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
                                onShowDatePicker(false)
                            },
                            onNoDate = {
                                onUpdateDraftSchedule(null, null, true, null, null)
                            },
                            initialStart = uiState.draftStartDateTime,
                            initialEnd = uiState.draftEndDateTime,
                            initialIsAllDay = uiState.draftIsAllDay,
                            initialReminder = uiState.draftReminder,
                            initialRepeat = uiState.draftRepeat
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
        placeholder = {
            Text(
                stringResource(R.string.hint_search_tasks),
                color = FocusTheme.colors.secondary
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                null,
                tint = FocusTheme.colors.secondary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(20.dp)
                )
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
    onViewSettingsChange: (com.monospace.app.core.domain.model.ViewSettings) -> Unit = {},
    onMarkSelectedDone: () -> Unit = {},
    onMoveSelectedToList: (String) -> Unit = {},
    onRescheduleSelected: (java.time.Instant?, java.time.Instant?, Boolean, ReminderConfig?, RepeatConfig?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToTask: (taskId: String) -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    initialShowSearch: Boolean = false,
    focusTimerState: com.monospace.app.feature.focus.FocusTimerState = com.monospace.app.feature.focus.FocusTimerState(),
    hasUsagePermission: Boolean = true,
    onSetFocusMode: (com.monospace.app.feature.focus.FocusMode) -> Unit = {},
    onAdjustFocusDuration: (Int) -> Unit = {},
    onStartFocus: () -> Unit = {},
    onStopFocus: () -> Unit = {},
    onOpenUsageSettings: () -> Unit = {},
    onRefreshUsagePermission: () -> Unit = {},
    taskDisplaySettings: com.monospace.app.core.domain.model.TaskDisplaySettings = com.monospace.app.core.domain.model.TaskDisplaySettings(),
    reverseScrollDirection: Boolean = false,
    onSetTaskStatus: ((String, com.monospace.app.core.domain.model.TaskStatus) -> Unit)? = null,
    launcherViewModel: LauncherViewModel = hiltViewModel()
) {
    val activeTasks = remember(state.tasks) { state.tasks.filter { it.status != TaskStatus.DONE } }
    val completedTasks =
        remember(state.tasks) { state.tasks.filter { it.status == TaskStatus.DONE } }
    var showSearchBar by remember { mutableStateOf(initialShowSearch || state.searchQuery.isNotBlank()) }
    var showFocusSheet by remember { mutableStateOf(false) }

    val shortcuts by launcherViewModel.shortcuts.collectAsState()
    val isEditMode by launcherViewModel.isEditMode.collectAsState()
    val installedApps by launcherViewModel.installedApps.collectAsState()
    var showAddAppSheet by remember { mutableStateOf(false) }

    // Selection action dialog states
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMarkDoneConfirm by remember { mutableStateOf(false) }
    var showRescheduleSheet by remember { mutableStateOf(false) }
    var showMoveToFolderSheet by remember { mutableStateOf(false) }

    val priorityChips = remember {
        listOf(Priority.HIGH to "Cao", Priority.MEDIUM to "Trung bình", Priority.LOW to "Thấp")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            HomeTopBar(
                isSelectionMode = state.isSelectionMode,
                selectedCount = state.selectedTaskIds.size,
                onExitSelection = { onSetSelectionMode(false) },
                onDeleteSelected = { showDeleteConfirm = true },
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
                },
                onFocusClick = { showFocusSheet = true },
                viewSettings = state.viewSettings,
                onViewSettingsChange = onViewSettingsChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            LauncherShortcutsSection(
                shortcuts = shortcuts,
                isEditMode = isEditMode,
                onLaunch = launcherViewModel::launchApp,
                onRemove = launcherViewModel::removeShortcut,
                onReorder = launcherViewModel::updateShortcutOrder,
                onToggleEditMode = launcherViewModel::toggleEditMode,
                onAddClick = {
                    launcherViewModel.loadInstalledApps()
                    showAddAppSheet = true
                }
            )

            // Search bar
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
                    onTaskSwipeDelete = onDeleteTask,
                    displaySettings = taskDisplaySettings,
                    reverseLayout = reverseScrollDirection,
                    onTaskStatusChange = onSetTaskStatus
                )
            }
        }

        // Bottom action bar khi selection mode
        if (state.isSelectionMode) {
            SelectionActionBar(
                selectedCount = state.selectedTaskIds.size,
                modifier = Modifier.align(Alignment.BottomCenter),
                onMoveToFolder = { showMoveToFolderSheet = true },
                onReschedule = { showRescheduleSheet = true },
                onDelete = { showDeleteConfirm = true },
                onMarkDone = { showMarkDoneConfirm = true }
            )
        }
    }

    // Dialogs & Sheets
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            onConfirm = { onDeleteSelected(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showMarkDoneConfirm) {
        ConfirmMarkDoneDialog(
            onConfirm = { onMarkSelectedDone(); showMarkDoneConfirm = false },
            onDismiss = { showMarkDoneConfirm = false }
        )
    }

    if (showRescheduleSheet) {
        RescheduleSheet(
            onDismiss = { showRescheduleSheet = false },
            onConfirm = { start: Instant?, end: Instant?, isAllDay: Boolean, reminder: ReminderConfig?, repeat: RepeatConfig? ->
                onRescheduleSelected(start, end, isAllDay, reminder, repeat)
                showRescheduleSheet = false
            }
        )
    }

    if (showMoveToFolderSheet) {
        MoveToFolderSheet(
            lists = (state as? HomeUiState.Success)?.availableLists ?: emptyList<TaskList>(),
            onSelect = { listId -> onMoveSelectedToList(listId); showMoveToFolderSheet = false },
            onDismiss = { showMoveToFolderSheet = false }
        )
    }

    if (showFocusSheet) {
        FocusSessionSheet(
            timerState = focusTimerState,
            hasUsagePermission = hasUsagePermission,
            onDismiss = { showFocusSheet = false },
            onSetMode = onSetFocusMode,
            onAdjustDuration = onAdjustFocusDuration,
            onStartFocus = onStartFocus,
            onStopFocus = onStopFocus,
            onOpenUsageSettings = {
                showFocusSheet = false
                onOpenUsageSettings()
            },
            onRefreshUsagePermission = onRefreshUsagePermission
        )
    }

    if (showAddAppSheet) {
        AddAppShortcutSheet(
            installedApps = installedApps,
            pinnedPackages = shortcuts.map { it.packageName }.toSet(),
            onAdd = launcherViewModel::addShortcut,
            onDismiss = { showAddAppSheet = false }
        )
    }
}
