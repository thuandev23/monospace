package com.monospace.app.feature.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monospace.app.R
import com.monospace.app.core.domain.model.ReminderConfig
import com.monospace.app.core.domain.model.ReminderUnit
import com.monospace.app.core.domain.model.RepeatConfig
import com.monospace.app.core.domain.model.RepeatUnit
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.ui.theme.FocusTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTodayClick: () -> Unit,
    availableLists: List<TaskList> = emptyList(),
    currentListId: String = "default",
    onListSelected: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()
    var taskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showListMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = FocusTheme.colors.divider) }
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
                textStyle = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary),
                decorationBox = { innerTextField ->
                    if (taskTitle.isEmpty()) {
                        Text(
                            stringResource(R.string.hint_task_title),
                            style = FocusTheme.typography.title.copy(color = FocusTheme.colors.divider)
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
                    Box {
                        val currentList = availableLists.find { it.id == currentListId }
                        TaskOptionChip(
                            icon = Icons.Outlined.MailOutline,
                            label = currentList?.name ?: stringResource(R.string.label_inbox),
                            onClick = { showListMenu = true }
                        )

                        DropdownMenu(
                            expanded = showListMenu,
                            onDismissRequest = { showListMenu = false },
                            modifier = Modifier.background(
                                FocusTheme.colors.surface,
                                RoundedCornerShape(16.dp)
                            )
                        ) {
                            availableLists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name, style = FocusTheme.typography.body) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Folder,
                                            null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        onListSelected(list.id)
                                        showListMenu = false
                                    }
                                )
                            }
                        }
                    }

                    TaskOptionChip(
                        icon = Icons.Default.DateRange,
                        label = stringResource(R.string.label_today),
                        onClick = onTodayClick
                    )
                    TaskOptionChip(
                        icon = Icons.Default.Notifications,
                        label = stringResource(R.string.label_reminder),
                        onClick = {})
                }

                IconButton(
                    onClick = { if (taskTitle.isNotBlank()) onSave(taskTitle) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(FocusTheme.colors.primary, CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null, tint = FocusTheme.colors.background)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun TaskOptionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = FocusTheme.colors.surfaceAlt,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = FocusTheme.colors.secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalCalendarDialog(
    onDismiss: () -> Unit,
    onConfigSave: (startDate: LocalDate, startTime: LocalTime?, endDate: LocalDate?, endTime: LocalTime?, reminder: ReminderConfig?, repeat: RepeatConfig?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Logic States
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var endTime by remember {
        mutableStateOf(
            LocalTime.now().plusHours(1).withSecond(0).withNano(0)
        )
    }

    var isTimeEnabled by remember { mutableStateOf(false) }
    var isDurationEnabled by remember { mutableStateOf(false) }
    var reminderConfig by remember { mutableStateOf<ReminderConfig?>(null) }
    var repeatConfig by remember { mutableStateOf<RepeatConfig?>(null) }

    // Dialog Controls
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    var showCustomRepeat by remember { mutableStateOf(false) }
    var pickingTarget by remember { mutableStateOf("START") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .background(FocusTheme.colors.surfaceAlt)
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.action_cancel),
                            color = FocusTheme.colors.primary
                        )
                    }
                    Text(
                        stringResource(R.string.label_date),
                        style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = {
                        onConfigSave(
                            startDate,
                            if (isTimeEnabled) startTime else null,
                            if (isDurationEnabled) endDate else null,
                            if (isDurationEnabled && isTimeEnabled) endTime else null,
                            reminderConfig,
                            repeatConfig
                        )
                        onDismiss()
                    }) {
                        Text(
                            stringResource(R.string.action_done),
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickDateOption(
                            Modifier.weight(1f),
                            Icons.Default.LightMode,
                            stringResource(R.string.label_tomorrow),
                            { startDate = LocalDate.now().plusDays(1) })
                        QuickDateOption(
                            Modifier.weight(1f),
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            stringResource(R.string.label_next_week),
                            { startDate = LocalDate.now().plusWeeks(1) })
                        QuickDateOption(
                            Modifier.weight(1f),
                            Icons.Default.Close,
                            stringResource(R.string.label_no_date),
                            {},
                            FocusTheme.colors.destructive
                        )
                    }

                    // Date Settings Section
                    Surface(
                        color = FocusTheme.colors.background,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column {
                            DateSettingsItem(
                                icon = Icons.Default.DateRange,
                                label = stringResource(R.string.label_start),
                                value = formatDateTime(
                                    startDate,
                                    if (isTimeEnabled) startTime else null
                                ),
                                onClick = { pickingTarget = "START"; showDatePicker = true }
                            )

                            if (isDurationEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                                )
                                DateSettingsItem(
                                    icon = Icons.Default.DateRange,
                                    label = stringResource(R.string.label_end),
                                    value = formatDateTime(
                                        endDate,
                                        if (isTimeEnabled) endTime else null
                                    ),
                                    onClick = { pickingTarget = "END"; showDatePicker = true }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                            )
                            DateSettingsItem(
                                icon = Icons.Default.AccessTime,
                                label = stringResource(R.string.label_time),
                                showSwitch = true,
                                switchState = isTimeEnabled,
                                value = if (isTimeEnabled) startTime.format(
                                    DateTimeFormatter.ofPattern(
                                        "HH:mm"
                                    )
                                ) else null,
                                onSwitchChange = { isTimeEnabled = it },
                                onClick = {
                                    if (isTimeEnabled) {
                                        pickingTarget = "START"; showTimePicker = true
                                    }
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                            )
                            DateSettingsItem(
                                icon = Icons.Default.Sync,
                                label = stringResource(R.string.label_duration),
                                showSwitch = true,
                                switchState = isDurationEnabled,
                                onSwitchChange = { isDurationEnabled = it }
                            )
                        }
                    }

                    // Reminder and Repeat Section
                    Surface(
                        color = FocusTheme.colors.background,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column {
                            DateSettingsItem(
                                icon = Icons.Default.Notifications,
                                label = stringResource(R.string.label_reminder),
                                value = reminderConfig?.let { "${it.value} ${it.unit.name.lowercase()}" }
                                    ?: "None",
                                showArrow = true,
                                onClick = { showReminderMenu = true }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                            )
                            DateSettingsItem(
                                icon = Icons.Default.Sync,
                                label = stringResource(R.string.label_repeat),
                                value = repeatConfig?.let { "Every ${it.interval} ${it.unit.name.lowercase()}" }
                                    ?: "None",
                                showArrow = true,
                                onClick = { showRepeatMenu = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Sub-Menus
            if (showReminderMenu) {
                ReminderSelectionDialog(
                    onReminderSelected = {
                        reminderConfig = it
                        showReminderMenu = false
                    },
                    onCustomClick = { showReminderMenu = false; showCustomReminder = true },
                    onDismiss = { showReminderMenu = false }
                )
            }

            if (showCustomReminder) {
                CustomReminderBottomSheet(
                    onDismiss = { showCustomReminder = false },
                    onDone = { config ->
                        reminderConfig = config
                        showCustomReminder = false
                    }
                )
            }

            if (showRepeatMenu) {
                RepeatSelectionDialog(
                    onRepeatSelected = { repeatConfig = it; showRepeatMenu = false },
                    onCustomClick = { showRepeatMenu = false; showCustomRepeat = true },
                    onDismiss = { showRepeatMenu = false }
                )
            }

            if (showCustomRepeat) {
                CustomRepeatBottomSheet(
                    onDismiss = { showCustomRepeat = false },
                    onDone = { config ->
                        repeatConfig = config
                        showCustomRepeat = false
                    }
                )
            }
        }
    }

    // Pickers
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (if (pickingTarget == "START") startDate else endDate).atStartOfDay(
                ZoneId.systemDefault()
            ).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val s = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .toLocalDate(); if (pickingTarget == "START") startDate =
                        s else endDate = s
                    }; showDatePicker = false; if (isTimeEnabled) showTimePicker = true
                }) { Text("OK") }
            }) { DatePicker(state = state) }
    }
    if (showTimePicker) {
        val initialTime = if (pickingTarget == "START") startTime else endTime
        val state = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(
                    24.dp
                ), color = FocusTheme.colors.background
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    TimePicker(state = state); Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        showTimePicker = false
                    }) { Text("Cancel") }; TextButton(onClick = {
                    val s = LocalTime.of(
                        state.hour,
                        state.minute
                    ); if (pickingTarget == "START") startTime = s else endTime =
                    s; showTimePicker = false
                }) { Text("OK") }
                }
                }
            }
        }
    }
}

@Composable
fun ReminderSelectionDialog(
    onReminderSelected: (ReminderConfig?) -> Unit,
    onCustomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = FocusTheme.colors.background
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                ReminderOptionItem("None", onClick = { onReminderSelected(null) })
                ReminderOptionItem(
                    "On the day (09:00)",
                    onClick = {
                        onReminderSelected(
                            ReminderConfig(
                                0,
                                ReminderUnit.DAY,
                                LocalTime.of(9, 0)
                            )
                        )
                    })
                ReminderOptionItem(
                    "1 day before (09:00)",
                    onClick = {
                        onReminderSelected(
                            ReminderConfig(
                                1,
                                ReminderUnit.DAY,
                                LocalTime.of(9, 0)
                            )
                        )
                    })
                ReminderOptionItem(
                    "1 week before (09:00)",
                    onClick = {
                        onReminderSelected(
                            ReminderConfig(
                                1,
                                ReminderUnit.WEEK,
                                LocalTime.of(9, 0)
                            )
                        )
                    })
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = FocusTheme.colors.divider
                )
                ReminderOptionItem(
                    stringResource(R.string.reminder_custom),
                    showArrow = true,
                    onClick = onCustomClick
                )
            }
        }
    }
}

@Composable
fun RepeatSelectionDialog(
    onRepeatSelected: (RepeatConfig?) -> Unit,
    onCustomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = FocusTheme.colors.background
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                ReminderOptionItem("None", onClick = { onRepeatSelected(null) })
                ReminderOptionItem(
                    "Daily",
                    onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.DAY)) })
                ReminderOptionItem(
                    "Weekly",
                    onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.WEEK)) })
                ReminderOptionItem(
                    "Monthly",
                    onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.MONTH)) })
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = FocusTheme.colors.divider
                )
                ReminderOptionItem("Custom", showArrow = true, onClick = onCustomClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderBottomSheet(onDismiss: () -> Unit, onDone: (ReminderConfig) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var count by remember { mutableStateOf(1) }
    var unit by remember { mutableStateOf(ReminderUnit.DAY) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var showTP by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        color = FocusTheme.colors.primary
                    )
                }
                Text(
                    "Custom Reminder",
                    style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onDone(ReminderConfig(count, unit, time)) }) {
                    Text(
                        "Done",
                        color = FocusTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                color = FocusTheme.colors.background,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Picker(
                            items = (0..30).map { it.toString() },
                            initialIndex = 1,
                            onItemSelected = { count = it.toInt() })
                        Picker(
                            items = ReminderUnit.entries.map {
                                it.name.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            initialIndex = 2,
                            onItemSelected = { unit = ReminderUnit.valueOf(it.uppercase()) })
                        Text(
                            "Before",
                            style = FocusTheme.typography.body.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTP = true }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Time"); Surface(
                        color = FocusTheme.colors.surfaceAlt,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            time.format(DateTimeFormatter.ofPattern("HH:mm")),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    }
                }
            }
        }
        if (showTP) {
            val state = rememberTimePickerState(
                time.hour,
                time.minute
            ); Dialog(onDismissRequest = {
                showTP = false
            }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = FocusTheme.colors.background
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        TimePicker(state = state); Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showTP = false
                        }) { Text("Cancel") }; TextButton(onClick = {
                        time = LocalTime.of(state.hour, state.minute); showTP = false
                    }) { Text("OK") }
                    }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRepeatBottomSheet(onDismiss: () -> Unit, onDone: (RepeatConfig) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var interval by remember { mutableStateOf(1) }
    var unit by remember { mutableStateOf(RepeatUnit.WEEK) }
    var selectedDays by remember { mutableStateOf(setOf(1)) } // Mon=1, Sun=7

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        color = FocusTheme.colors.primary
                    )
                }
                Text(
                    "Custom Repeat",
                    style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = {
                    onDone(
                        RepeatConfig(
                            interval,
                            unit,
                            if (unit == RepeatUnit.WEEK) selectedDays else null
                        )
                    )
                }) { Text("Done", color = FocusTheme.colors.primary, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                color = FocusTheme.colors.background,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Every",
                            style = FocusTheme.typography.body.copy(fontWeight = FontWeight.Bold)
                        )
                        Picker(
                            items = (1..30).map { it.toString() },
                            initialIndex = 0,
                            onItemSelected = { interval = it.toInt() })
                        Picker(
                            items = RepeatUnit.entries.map {
                                it.name.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            initialIndex = 1,
                            onItemSelected = { unit = RepeatUnit.valueOf(it.uppercase()) })
                    }
                }
            }
            if (unit == RepeatUnit.WEEK) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    color = FocusTheme.colors.background,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        val dayNames = listOf(
                            "Monday",
                            "Tuesday",
                            "Wednesday",
                            "Thursday",
                            "Friday",
                            "Saturday",
                            "Sunday"
                        )
                        dayNames.forEachIndexed { index, name ->
                            val dayIdx = index + 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (selectedDays.contains(dayIdx)) selectedDays -= dayIdx else selectedDays += dayIdx }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name); if (selectedDays.contains(dayIdx)) Icon(
                                Icons.Default.Check,
                                null,
                                tint = FocusTheme.colors.primary
                            )
                            }
                            if (index < dayNames.size - 1) HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp
                                ), color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Picker(items: List<String>, initialIndex: Int, onItemSelected: (String) -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { onItemSelected(items[it]) }
    }

    Box(modifier = Modifier
        .height(120.dp)
        .width(80.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = FocusTheme.colors.surfaceAlt,
            shape = RoundedCornerShape(8.dp)
        ) {}
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(40.dp)) }
            itemsIndexed(items) { index, item ->
                val isSelected =
                    remember { derivedStateOf { listState.firstVisibleItemIndex == index } }
                Text(
                    text = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = FocusTheme.typography.body.copy(
                        fontWeight = if (isSelected.value) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected.value) FocusTheme.colors.primary else FocusTheme.colors.divider
                    )
                )
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ReminderOptionItem(
    text: String,
    isSelected: Boolean = false,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = FocusTheme.colors.primary
                ); Spacer(modifier = Modifier.width(12.dp))
            } else if (!showArrow) Spacer(modifier = Modifier.width(30.dp))
            if (showArrow) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    modifier = Modifier.size(18.dp)
                ); Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text, style = FocusTheme.typography.body.copy(fontSize = 15.sp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOptionsBottomSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showOverdue by remember { mutableStateOf(true) }
    var showInProgress by remember { mutableStateOf(true) }
    var showCompleted by remember { mutableStateOf(true) }
    var showTime by remember { mutableStateOf(true) }
    var showFolder by remember { mutableStateOf(true) }

    var currentSort by remember { mutableStateOf("Manual") }
    var currentGroup by remember { mutableStateOf("None") }

    var showSortDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(FocusTheme.colors.surfaceAlt)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = FocusTheme.colors.primary)
                }
                Text(
                    "View",
                    style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.action_done),
                        color = FocusTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Shown in list",
                    style = FocusTheme.typography.headline.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Surface(color = FocusTheme.colors.background, shape = RoundedCornerShape(24.dp)) {
                    Column {
                        DateSettingsItem(
                            icon = Icons.Default.History,
                            label = "Overdue tasks",
                            showSwitch = true,
                            switchState = showOverdue,
                            onSwitchChange = { showOverdue = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                        )
                        DateSettingsItem(
                            icon = Icons.Default.PieChart,
                            label = "In progress tasks",
                            showSwitch = true,
                            switchState = showInProgress,
                            onSwitchChange = { showInProgress = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                        )
                        DateSettingsItem(
                            icon = Icons.Default.Check,
                            label = "Completed tasks",
                            showSwitch = true,
                            switchState = showCompleted,
                            onSwitchChange = { showCompleted = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                        )
                        DateSettingsItem(
                            icon = Icons.Default.DateRange,
                            label = "Time",
                            showSwitch = true,
                            switchState = showTime,
                            onSwitchChange = { showTime = it }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                        )
                        DateSettingsItem(
                            icon = Icons.Default.Folder,
                            label = "Folder",
                            showSwitch = true,
                            switchState = showFolder,
                            onSwitchChange = { showFolder = it }
                        )
                    }
                }

                Text(
                    "Sort",
                    style = FocusTheme.typography.headline.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Surface(color = FocusTheme.colors.background, shape = RoundedCornerShape(24.dp)) {
                    DateSettingsItem(
                        icon = Icons.Default.SwapVert,
                        label = "Sort",
                        value = currentSort,
                        showArrow = true,
                        onClick = { showSortDialog = true }
                    )
                }

                Text(
                    "Group",
                    style = FocusTheme.typography.headline.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Surface(color = FocusTheme.colors.background, shape = RoundedCornerShape(24.dp)) {
                    DateSettingsItem(
                        icon = Icons.Default.ViewModule,
                        label = "Group",
                        value = currentGroup,
                        showArrow = true,
                        onClick = { showGroupDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showSortDialog) {
            SelectionDialog(
                title = "Sort",
                options = listOf("Manual", "Name", "Date", "Folder"),
                selectedOption = currentSort,
                onOptionSelected = { currentSort = it; showSortDialog = false },
                onDismiss = { showSortDialog = false }
            )
        }

        if (showGroupDialog) {
            SelectionDialog(
                title = "Group",
                options = listOf("None", "Default", "Folder"),
                selectedOption = currentGroup,
                onOptionSelected = { currentGroup = it; showGroupDialog = false },
                onDismiss = { showGroupDialog = false }
            )
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = FocusTheme.colors.background
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = title,
                    style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                options.forEach { option ->
                    ReminderOptionItem(
                        text = option,
                        isSelected = option == selectedOption,
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        }
    }
}

private fun formatDateTime(date: LocalDate, time: LocalTime?): String {
    val d = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    return if (time != null) "$d, ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}" else d
}

@Composable
private fun QuickDateOption(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color = FocusTheme.colors.primary
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = FocusTheme.colors.background,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = FocusTheme.typography.label.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun DateSettingsItem(
    icon: ImageVector,
    label: String,
    value: String? = null,
    showSwitch: Boolean = false,
    switchState: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    showArrow: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (value != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = FocusTheme.colors.surfaceAlt,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = value,
                        style = FocusTheme.typography.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showArrow) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.SwapVert, // Using a representative icon for selection
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = FocusTheme.colors.secondary
                        )
                    }
                }
            }
        }
        if (showSwitch) Switch(
            checked = switchState,
            onCheckedChange = onSwitchChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FocusTheme.colors.success,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = FocusTheme.colors.divider
            )
        )
    }
}
