package com.monospace.app.feature.launcher

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.monospace.app.R
import com.monospace.app.core.data.Task


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var isSelectedMode by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val tasks = remember {
        mutableStateListOf(
            Task(title = "Thiết kế UI cho Monospace", category = "Design"),
            Task(title = "Kết nối API Notion", category = "Coding"),
            Task(title = "Mua cà phê cho Team", category = "Personal", isCompleted = true),
            Task(title = "Họp review với Senior Architect", category = "Work"),
            Task(title = "Tối ưu hóa Docker Image", category = "DevOps")
        )
    }
    val activeTasks by remember { derivedStateOf { tasks.filter { !it.isCompleted } } }
    val completedTasks by remember { derivedStateOf { tasks.filter { it.isCompleted } } }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                MinimalBottomNav()
            }
        },
        floatingActionButton = {
            if (!isSelectedMode) {
                FloatingActionButton(
                    onClick = {
                        showCreateSheet = true
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
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
                    isSelectionMode = isSelectedMode,
                    onExitSelection = { isSelectedMode = false },
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = it },
                    onSelectedTasks = {
                        isSelectedMode = true
                        isMenuExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                if (tasks.isEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    EmptyStateTask()
                    Spacer(modifier = Modifier.weight(1.5f))
                } else {
                    TaskList(
                        activeTasks = activeTasks,
                        completedTasks = completedTasks,
                        onTaskToggle = { taskId, completed ->
                            val index = tasks.indexOfFirst { it.id == taskId }
                            if (index != -1) {
                                tasks[index] = tasks[index].copy(isCompleted = completed)
                            }
                        }
                    )
                }
            }
            // Sheet 1: Nhập tên Task
            if (showCreateSheet) {
                CreateTaskSheet(
                    onDismiss = { showCreateSheet = false },
                    onSave = { /* save logic */ },
                    onTodayClick = { showDatePicker = true } // Mở sheet chọn ngày
                )
            }

            // Sheet 2: Chọn ngày (Hiện đè lên Sheet 1)
            if (showDatePicker) {
                DatePickerSheet(
                    onDismiss = { showDatePicker = false },
                    onDone = {
                        // Cập nhật ngày vào task đang tạo
                        showDatePicker = false
                    }
                )
            }
        }
    }

}

@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    onExitSelection: () -> Unit,
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
                TextButton(onClick = onExitSelection) {
                    Text("Cancel", style = TextStyle(fontWeight = FontWeight.Bold))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                if (!isSelectionMode) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (!isMenuExpanded) {
                            IconButton(
                                onClick = { onMenuToggle(true) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        MonospaceDropdownMenu(
                            expanded = isMenuExpanded,
                            onDismiss = { onMenuToggle(false) },
                            onSelectedTasks = {
                                onSelectedTasks()
                                onMenuToggle(false)
                            }
                        )
                    }
                } else {
                    TextButton(onClick = { /*Select All*/ }) {
                        Text("Select All", style = TextStyle(fontWeight = FontWeight.Bold))
                    }
                }


            }
        }

        // Lớp dưới: Today và Ngày tháng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Today",
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "7",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp)
                )
                Text(
                    text = "Apr",
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
        // Tùy chỉnh shape cho DropdownMenu bên trong Theme cục bộ
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = androidx.compose.ui.unit.DpOffset(x = (0).dp, y = 0.dp),
            modifier = Modifier
                .background(Color.White)
                .width(180.dp)
                .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            DropdownMenuItem(
                text = { Text("View", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { /* Handle View */ onDismiss() }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = Color.LightGray
            )
            DropdownMenuItem(
                text = { Text("Select tasks", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24dp),
                        contentDescription = null,
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
    onSave: (String) -> Unit, onTodayClick: () -> Unit
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
            // 1. TextField tối giản
            BasicTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 20.sp, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text("Task title", color = Color.LightGray, fontSize = 20.sp)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Row chứa Chips và Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskOptionChip(icon = Icons.Outlined.MailOutline, label = "Inbox", onClick = {})
                    TaskOptionChip(
                        icon = Icons.Default.DateRange,
                        label = "Today",
                        onClick = onTodayClick
                    )
                    TaskOptionChip(icon = Icons.Default.Place, label = "Deadline", onClick = {})
                }

                // Nút Gửi (Submit)
                IconButton(
                    onClick = { if (taskTitle.isNotBlank()) onSave(taskTitle) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Save Task",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Tự động focus và mở bàn phím
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun QuickActionCard(
    onTomorrowClick: () -> Unit,
    onNextWeekClick: () -> Unit,
    onNoDateClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionItem(
                icon = Icons.Default.Menu, // Cần import thêm Icons.Default.WbSunny
                label = "Tomorrow",
                tint = Color.Gray,
                onClick = onTomorrowClick
            )
            QuickActionItem(
                icon = Icons.Default.ArrowForward,
                label = "Next Week",
                tint = Color.Gray,
                onClick = onNextWeekClick
            )
            QuickActionItem(
                icon = Icons.Default.Close,
                label = "No Date",
                tint = Color(0xFFFF453A), // System Red
                onClick = onNoDateClick
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TaskOptionChip(icon: ImageVector, label: String?, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.DarkGray
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun TaskList(
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    onTaskToggle: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(
            items = activeTasks,
            key = { it.id }
        ) { task ->
            TaskItem(
                task = task,
                onToggle = { isChecked -> onTaskToggle(task.id, isChecked) },
                onClick = { /* Mở chi tiết task */ },
                modifier = Modifier.animateItem()
            )
//            // Divider mỏng theo phong cách minimal
//            HorizontalDivider(
//                thickness = 0.5.dp,
//                color = Color.LightGray.copy(alpha = 0.3f)
//            )
        }
        if (activeTasks.isNotEmpty() && completedTasks.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                }
            }
        }
        items(
            items = completedTasks,
            key = { it.id }
        ) { task ->
            TaskItem(
                task = task,
                onToggle = { isChecked -> onTaskToggle(task.id, isChecked) },
                onClick = { /* Mở chi tiết task */ },
                modifier = Modifier.animateItem()
            )
//            // Divider mỏng theo phong cách minimal
//            HorizontalDivider(
//                thickness = 0.5.dp,
//                color = Color.LightGray.copy(alpha = 0.3f)
//            )
        }

    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularCheckbox(
            checked = task.isCompleted,
            onCheckedChange = { isChecked ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(isChecked)
            }
        )

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

        Text(
            text = task.category,
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Normal
            )
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
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyStateTask() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sử dụng icon layers từ resources
        Icon(
            painter = painterResource(id = R.drawable.content_copy_24dp),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No tasks found",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Create task */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                "Create new task",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MinimalBottomNav() {
    Surface(
        color = Color(0xFFF9F9F9),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(72.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "7",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(30.dp)
            )
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initialDate: String = "7 Apr 2026",
    onDismiss: () -> Unit,
    onDone: (/* Pass data here */) -> Unit
) {
    var isTimeEnabled by remember { mutableStateOf(false) }
    var isDurationEnabled by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var selectedRepeatOption by remember { mutableStateOf("None") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF2F2F7), // Màu nền xám nhạt chuẩn iOS
        dragHandle = null // Ẩn drag handle để giống ảnh mẫu
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            // 1. Header Section
            SheetHeader(onDismiss, onDone)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Quick Actions Section
//            QuickActionCard()

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Main Settings Section
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingRow(
                        icon = Icons.Default.AccountBox,
                        label = "Date",
                        onClick = { showCalendarDialog = true },
                        value = {
                            DateChip(initialDate)
                        }
                    )
                    HorizontalDivider(
                        Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE5E5EA)
                    )
                    SettingRow(
                        icon = Icons.Default.Clear,
                        label = "Time",
                        action = {
                            Switch(
                                checked = isTimeEnabled,
                                onCheckedChange = { isTimeEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF34C759)
                                )
                            )
                        }
                    )
                    HorizontalDivider(
                        Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE5E5EA)
                    )
                    SettingRow(
                        icon = Icons.Default.LocationOn,
                        label = "Duration",
                        action = {
                            Switch(
                                checked = isDurationEnabled,
                                onCheckedChange = { isDurationEnabled = it }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (showCalendarDialog) {
                MinimalCalendarDialog(onDismiss = { showCalendarDialog = false })
            }

            if (showRepeatDialog) {
                RepeatSelectionDialog(
                    selectedOption = selectedRepeatOption,
                    onOptionSelected = { selectedRepeatOption = it },
                    onDismiss = { showRepeatDialog = false }
                )
            }
            // 4. Reminder & Repeat Section
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingRow(
                        icon = Icons.Default.Warning,
                        label = "Reminder",
                        valueLabel = "None",
                        hasArrow = true
                    )
                    HorizontalDivider(
                        Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFE5E5EA)
                    )
                    SettingRow(
                        icon = Icons.Default.Send,
                        label = "Repeat",
                        valueLabel = "None",
                        hasArrow = true,
                        onClick = { showRepeatDialog = true }
                    )
                }
            }
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
            Text("Cancel", color = Color.Gray, fontSize = 17.sp)
        }
        Text("Date", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        TextButton(onClick = onDone) {
            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
fun RepeatSelectionDialog(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("None", "Daily", "Weekly", "Monthly", "Yearly", "Custom")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.width(280.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(32.dp)) {
                            if (option == selectedOption) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(text = option, modifier = Modifier.weight(1f), fontSize = 17.sp)
                        if (option == "Custom") {
                            Icon(Icons.Default.Menu, null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalCalendarDialog(onDismiss: () -> Unit) {
    // Sử dụng phiên bản AlertDialog chỉ có tham số content
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(28.dp)),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            // Mọi thứ bên trong này chính là 'content'
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header: Month Year + Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { /* Mở picker tháng/năm */ },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "April 2026",
                                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            )
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(24.dp))
                        }
                        Row {
                            IconButton(onClick = { /* Previous Month */ }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                            }
                            IconButton(onClick = { /* Next Month */ }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekdays Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        days.forEach { day ->
                            Text(
                                text = day,
                                style = TextStyle(
                                    color = Color.LightGray,
                                    fontSize = 11.sp, // Nhỏ lại cho tinh tế
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Days Grid
                    val daysInMonth = (1..30).toList()
                    val startOffset = 2

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .height(260.dp) // Tăng nhẹ để tránh bị cắt content
                            .padding(top = 16.dp),
                        userScrollEnabled = false
                    ) {
                        items(startOffset) { Spacer(Modifier.size(40.dp)) }
                        items(daysInMonth) { day ->
                            val isSelected = day == 7
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f) // Đảm bảo luôn là hình vuông
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.Black else Color.Transparent)
                                    .clickable {
                                        /* logic select date */
                                        onDismiss() // Thường thì chọn xong sẽ đóng dialog
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Nếu bạn muốn có nút "Done" giống thiết kế cũ, hãy đặt nó ở đây:
                    /*
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                */
                }
            }
        })
}

@Composable
fun SettingRow(
    icon: ImageVector,
    label: String,
    valueLabel: String? = null,
    hasArrow: Boolean = false,
    onClick: () -> Unit = {},
    value: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Black)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 17.sp)

        if (value != null) value()
        if (valueLabel != null) {
            Text(valueLabel, color = Color.Gray, fontSize = 17.sp)
            if (hasArrow) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (action != null) action()
    }
}

@Composable
fun DateChip(date: String) {
    Surface(
        color = Color(0xFFE5E5EA),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            date,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 15.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
