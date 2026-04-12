package com.monospace.app.feature.launcher

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.R
import com.monospace.app.core.domain.model.Task
import com.monospace.app.feature.launcher.state.LauncherViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var isMenuExpanded by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeTasks = remember(uiState.tasks) { uiState.tasks.filter { !it.isCompleted } }
    val completedTasks = remember(uiState.tasks) { uiState.tasks.filter { it.isCompleted } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            if (!uiState.isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalBottomNav(
                        onSearchClick = { /* Handle Search */ },
                        onListClick = { /* Handle List */ }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = Color.Black,
                    contentColor = Color.White,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                HomeTopBar(
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedTaskIds.size,
                    onExitSelection = { viewModel.setSelectionMode(false) },
                    onDeleteSelected = { viewModel.deleteSelectedTasks() },
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = it },
                    onSelectedTasks = {
                        viewModel.setSelectionMode(true)
                        isMenuExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                    Spacer(modifier = Modifier.weight(1f))
                    EmptyStateTask(onCreateClick = { showCreateSheet = true })
                    Spacer(modifier = Modifier.weight(1.5f))
                } else {
                    TaskList(
                        activeTasks = activeTasks,
                        completedTasks = completedTasks,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedTaskIds = uiState.selectedTaskIds,
                        onTaskToggle = { taskId, completed ->
                            viewModel.toggleTask(taskId, completed)
                        },
                        onTaskClick = { task ->
                            if (uiState.isSelectionMode) {
                                viewModel.toggleTaskSelection(task.id)
                            } else {
                                // Mở chi tiết task
                            }
                        },
                        onTaskLongClick = { task ->
                            if (!uiState.isSelectionMode) {
                                viewModel.setSelectionMode(true)
                                viewModel.toggleTaskSelection(task.id)
                            }
                        }
                    )
                }
            }

            if (showCreateSheet) {
                CreateTaskSheet(
                    onDismiss = { showCreateSheet = false },
                    onSave = { title ->
                        viewModel.addTask(title)
                        showCreateSheet = false
                    },
                    onTodayClick = { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                DatePickerSheet(
                    onDismiss = { showDatePicker = false },
                    onDone = { showDatePicker = false }
                )
            }
        }
    }
}

@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSelectedTasks: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onExitSelection) {
                        Text(
                            stringResource(R.string.action_cancel),
                            style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black)
                        )
                    }
                    if (selectedCount > 0) {
                        Text(
                            "$selectedCount selected",
                            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                if (!isSelectionMode) {
                    IconButton(
                        onClick = { onMenuToggle(true) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.content_desc_menu),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    MonospaceDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismiss = { onMenuToggle(false) },
                        onSelectedTasks = onSelectedTasks
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCount > 0) {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                        TextButton(onClick = { /* Select All logic */ }) {
                            Text(
                                stringResource(R.string.action_select_all),
                                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.label_today),
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            val calendar = Calendar.getInstance()
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
                )
                Text(
                    text = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
                        ?: "",
                    style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                )
            }
        }
    }
}

@Composable
fun MonospaceDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectedTasks: () -> Unit
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(Color.White)
                .width(200.dp)
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            DropdownMenuItem(
                text = { Text("View", fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.size(20.dp)) },
                onClick = { onDismiss() }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp)
            DropdownMenuItem(
                text = { Text("Select tasks", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24dp),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onSelectedTasks
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTodayClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var taskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .imePadding()
        ) {
            BasicTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 20.sp, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text(
                            stringResource(R.string.hint_task_title),
                            color = Color.LightGray,
                            fontSize = 20.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskOptionChip(
                        icon = Icons.Outlined.MailOutline,
                        label = stringResource(R.string.label_inbox),
                        onClick = {})
                    TaskOptionChip(
                        icon = Icons.Default.DateRange,
                        label = stringResource(R.string.label_today),
                        onClick = onTodayClick
                    )
                    TaskOptionChip(
                        icon = Icons.Default.Place,
                        label = stringResource(R.string.label_deadline),
                        onClick = {})
                }

                IconButton(
                    onClick = { if (taskTitle.isNotBlank()) onSave(taskTitle) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black, CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TaskList(
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    isSelectionMode: Boolean,
    selectedTaskIds: Set<String>,
    onTaskToggle: (String, Boolean) -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(items = activeTasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                isSelected = selectedTaskIds.contains(task.id),
                isSelectionMode = isSelectionMode,
                onToggle = { onTaskToggle(task.id, it) },
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) },
                modifier = Modifier.animateItem()
            )
        }

        if (activeTasks.isNotEmpty() && completedTasks.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )
            }
        }

        items(items = completedTasks, key = { it.id }) { task ->
            TaskItem(
                task = task,
                isSelected = selectedTaskIds.contains(task.id),
                isSelectionMode = isSelectionMode,
                onToggle = { onTaskToggle(task.id, it) },
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            CircularCheckbox(
                checked = isSelected,
                onCheckedChange = { _ -> onClick() }
            )
        } else {
            CircularCheckbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(it)
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (task.isCompleted) Color(0xFFBDBDBD) else Color.Black,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) Color.Black else Color.Transparent)
            .border(2.dp, if (checked) Color.Black else Color.Gray, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun EmptyStateTask(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.content_copy_24dp),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.msg_no_tasks),
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.action_create_task), color = Color.White)
        }
    }
}

@Composable
fun MinimalBottomNav(
    onSearchClick: () -> Unit,
    onListClick: () -> Unit
) {
    Surface(
        color = Color(0xFFF9F9F9),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(72.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val calendar = Calendar.getInstance()
            Text(
                calendar.get(Calendar.DAY_OF_MONTH).toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Icon(Icons.Default.DateRange, null, tint = Color.Gray)
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    null,
                    tint = Color.Gray
                )
            }
            IconButton(onClick = onListClick) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    null,
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TaskOptionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(onDismiss: () -> Unit, onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFFF2F2F7)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SheetHeader(onDismiss, onDone)
        }
    }
}

@Composable
fun SheetHeader(onDismiss: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(
                stringResource(R.string.action_cancel),
                color = Color.Gray
            )
        }
        Text("Date", fontWeight = FontWeight.Bold)
        TextButton(onClick = onDone) {
            Text(
                stringResource(R.string.action_done),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
