package com.monospace.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.Priority
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.feature.launcher.components.MinimalCalendarDialog
import com.monospace.app.ui.theme.FocusTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskDetailEvent.SavedAndNavigateBack -> onNavigateBack()
                is TaskDetailEvent.DeletedAndNavigateBack -> onNavigateBack()
                is TaskDetailEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    when (val state = uiState) {
        is TaskDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FocusTheme.colors.primary)
            }
        }

        is TaskDetailUiState.NotFound -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Task không tồn tại", color = FocusTheme.colors.secondary)
            }
        }

        is TaskDetailUiState.Editing -> {
            TaskDetailContent(
                state = state,
                snackbarHostState = snackbarHostState,
                onNavigateBack = onNavigateBack,
                onTitleChange = viewModel::onTitleChange,
                onNotesChange = viewModel::onNotesChange,
                onListIdChange = viewModel::onListIdChange,
                onPriorityChange = viewModel::onPriorityChange,
                onShowDatePicker = viewModel::onShowDatePicker,
                onScheduleChange = viewModel::onScheduleChange,
                onSave = viewModel::saveTask,
                onDelete = viewModel::deleteTask
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailContent(
    state: TaskDetailUiState.Editing,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onListIdChange: (String) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onShowDatePicker: (Boolean) -> Unit,
    onScheduleChange: (Instant?, Instant?, Boolean, ReminderConfig?, RepeatConfig?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa task?", color = FocusTheme.colors.primary) },
            text = { Text("Hành động này không thể hoàn tác.", color = FocusTheme.colors.secondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Xóa", color = FocusTheme.colors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = FocusTheme.colors.secondary)
                }
            },
            containerColor = FocusTheme.colors.surface
        )
    }

    if (state.showDatePicker) {
        MinimalCalendarDialog(
            onDismiss = { onShowDatePicker(false) },
            onConfigSave = { startD, startT, endD, endT, rem, rep ->
                val startInstant = startD.atTime(startT ?: LocalTime.MIDNIGHT)
                    .atZone(ZoneId.systemDefault()).toInstant()
                val endInstant = endD?.atTime(endT ?: LocalTime.MAX)
                    ?.atZone(ZoneId.systemDefault())?.toInstant()
                onScheduleChange(startInstant, endInstant, startT == null, rem, rep)
                onShowDatePicker(false)
            },
            onNoDate = {
                onScheduleChange(null, null, true, null, null)
                onShowDatePicker(false)
            },
            initialStart = state.startDateTime,
            initialEnd = state.endDateTime,
            initialIsAllDay = state.isAllDay,
            initialReminder = state.reminder,
            initialRepeat = state.repeat
        )
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = FocusTheme.colors.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = FocusTheme.colors.destructive)
                    }
                    IconButton(onClick = onSave, enabled = !state.isSaving) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = FocusTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Lưu", tint = FocusTheme.colors.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FocusTheme.colors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(Modifier.height(8.dp))

            // Title
            androidx.compose.foundation.text.BasicTextField(
                value = state.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                decorationBox = { inner ->
                    if (state.title.isEmpty()) {
                        Text(
                            "Tiêu đề task",
                            style = FocusTheme.typography.title.copy(
                                color = FocusTheme.colors.secondary.copy(alpha = 0.5f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(color = FocusTheme.colors.divider)

            Spacer(Modifier.height(12.dp))

            // Notes
            androidx.compose.foundation.text.BasicTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                textStyle = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary),
                decorationBox = { inner ->
                    if (state.notes.isEmpty()) {
                        Text(
                            "Ghi chú...",
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.secondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = FocusTheme.colors.divider)
            Spacer(Modifier.height(8.dp))

            // Schedule row
            DetailRow(
                icon = Icons.Default.AccessTime,
                label = "Thời gian",
                value = formatSchedule(state.startDateTime, state.endDateTime, state.isAllDay),
                onClick = { onShowDatePicker(true) }
            )

            // Reminder row
            DetailRow(
                icon = Icons.Default.Notifications,
                label = "Nhắc nhở",
                value = formatReminder(state.reminder),
                onClick = { onShowDatePicker(true) }
            )

            // Repeat row
            DetailRow(
                icon = Icons.Default.Repeat,
                label = "Lặp lại",
                value = formatRepeat(state.repeat),
                onClick = { onShowDatePicker(true) }
            )

            // List picker row
            ListPickerRow(
                currentListId = state.listId,
                availableLists = state.availableLists,
                onListSelected = onListIdChange
            )

            // Priority row
            PriorityRow(
                current = state.priority,
                onSelect = onPriorityChange
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary), modifier = Modifier.weight(1f))
        Text(value, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
    }
    HorizontalDivider(color = FocusTheme.colors.divider)
}

@Composable
private fun ListPickerRow(
    currentListId: String,
    availableLists: List<com.monospace.app.core.domain.model.TaskList>,
    onListSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = availableLists.find { it.id == currentListId }?.name ?: currentListId

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("Danh sách", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary), modifier = Modifier.weight(1f))
            Text(currentName, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FocusTheme.colors.surface)
        ) {
            availableLists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.name, color = FocusTheme.colors.primary) },
                    onClick = { onListSelected(list.id); expanded = false }
                )
            }
        }
    }
    HorizontalDivider(color = FocusTheme.colors.divider)
}

@Composable
private fun PriorityRow(
    current: Priority,
    onSelect: (Priority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(Priority.NONE to "Không", Priority.LOW to "Thấp", Priority.MEDIUM to "Trung bình", Priority.HIGH to "Cao")

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(priorityColor(current))
            )
            Spacer(Modifier.width(12.dp))
            Text("Ưu tiên", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary), modifier = Modifier.weight(1f))
            Text(labels[current] ?: "Không", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FocusTheme.colors.surface)
        ) {
            Priority.entries.forEach { priority ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(priorityColor(priority))
                            )
                            Text(labels[priority] ?: "", color = FocusTheme.colors.primary)
                        }
                    },
                    onClick = { onSelect(priority); expanded = false }
                )
            }
        }
    }
    HorizontalDivider(color = FocusTheme.colors.divider)
}

@Composable
private fun priorityColor(priority: Priority) = when (priority) {
    Priority.HIGH -> FocusTheme.colors.destructive
    Priority.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
    Priority.LOW -> FocusTheme.colors.success
    Priority.NONE -> FocusTheme.colors.divider
}

private fun formatSchedule(start: Instant?, end: Instant?, isAllDay: Boolean): String {
    if (start == null) return "Không"
    val zone = ZoneId.systemDefault()
    val startZdt = start.atZone(zone)
    val formatter = if (isAllDay) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    
    val startStr = startZdt.format(formatter)
    if (end == null) return startStr
    
    val endZdt = end.atZone(zone)
    val endStr = endZdt.format(formatter)
    
    return "$startStr - $endStr"
}

private fun formatReminder(reminder: ReminderConfig?): String {
    if (reminder == null) return "Không"
    if (reminder.value == 0) return "Vào lúc bắt đầu"
    
    val unitStr = when (reminder.unit) {
        ReminderUnit.MINUTE -> "phút"
        ReminderUnit.HOUR -> "giờ"
        ReminderUnit.DAY -> "ngày"
        ReminderUnit.WEEK -> "tuần"
        ReminderUnit.MONTH -> "tháng"
    }
    val relation = if (reminder.isBefore) "trước" else "sau"
    return "${reminder.value} $unitStr $relation"
}

private fun formatRepeat(rep: RepeatConfig?): String {
    if (rep == null) return "Không"
    val unitLabel = when (rep.unit) {
        RepeatUnit.DAY -> if (rep.interval == 1) "ngày" else "${rep.interval} ngày"
        RepeatUnit.WEEK -> if (rep.interval == 1) "tuần" else "${rep.interval} tuần"
        RepeatUnit.MONTH -> if (rep.interval == 1) "tháng" else "${rep.interval} tháng"
        RepeatUnit.YEAR -> if (rep.interval == 1) "năm" else "${rep.interval} năm"
    }
    
    var result = "Mỗi $unitLabel"
    if (rep.unit == RepeatUnit.WEEK && !rep.daysOfWeek.isNullOrEmpty()) {
        val days = rep.daysOfWeek.sorted().joinToString(", ") { dayIdx ->
            when (dayIdx) {
                1 -> "Thứ 2"
                2 -> "Thứ 3"
                3 -> "Thứ 4"
                4 -> "Thứ 5"
                5 -> "Thứ 6"
                6 -> "Thứ 7"
                7 -> "Chủ nhật"
                else -> ""
            }
        }
        result += " ($days)"
    }
    return result
}
