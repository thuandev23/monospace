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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalFocusManager
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
import com.monospace.app.feature.focus.FocusMode
import com.monospace.app.feature.focus.FocusTimerState
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
    onListSelected: (String) -> Unit = {},
    draftStartDate: Instant? = null,
    draftIsAllDay: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState()
    var taskTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var showListMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            if (showListMenu) {
                // ── Inline folder picker (thay DropdownMenu để tránh lỗi vị trí popup) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_choose_folder),
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showListMenu = false }) {
                        Icon(Icons.Default.Close, contentDescription = null,
                            tint = FocusTheme.colors.secondary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                availableLists.forEach { list ->
                    val isSelected = list.id == currentListId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) FocusTheme.colors.surface else Color.Transparent
                            )
                            .clickable {
                                onListSelected(list.id)
                                showListMenu = false
                                focusRequester.requestFocus()
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = FocusTheme.colors.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = list.name,
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // ── Normal create task UI ──
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
                        val currentList = availableLists.find { it.id == currentListId }
                        TaskOptionChip(
                            icon = Icons.Outlined.MailOutline,
                            label = currentList?.name ?: stringResource(R.string.label_inbox),
                            onClick = {
                                focusManager.clearFocus()
                                showListMenu = true
                            }
                        )
                        TaskOptionChip(
                            icon = Icons.Default.DateRange,
                            label = draftStartDate?.let { instant ->
                                val zone = ZoneId.systemDefault()
                                val date = instant.atZone(zone).toLocalDate()
                                val today = LocalDate.now()
                                when (date) {
                                    today -> stringResource(R.string.label_today)
                                    today.plusDays(1) -> stringResource(R.string.label_tomorrow)
                                    else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
                                }
                            } ?: stringResource(R.string.label_date),
                            onClick = onTodayClick
                        )
                        TaskOptionChip(
                            icon = Icons.Default.Notifications,
                            label = stringResource(R.string.label_reminder),
                            onClick = {}
                        )
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
    onConfigSave: (startDate: LocalDate, startTime: LocalTime?, endDate: LocalDate?, endTime: LocalTime?, reminder: ReminderConfig?, repeat: RepeatConfig?) -> Unit,
    onNoDate: () -> Unit = {},
    initialStart: Instant? = null,
    initialEnd: Instant? = null,
    initialIsAllDay: Boolean = true,
    initialReminder: ReminderConfig? = null,
    initialRepeat: RepeatConfig? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = ZoneId.systemDefault()

    // Pre-populate from existing draft when provided
    var startDate by remember {
        mutableStateOf(initialStart?.atZone(zone)?.toLocalDate() ?: LocalDate.now())
    }
    var startTime by remember {
        mutableStateOf(initialStart?.atZone(zone)?.toLocalTime()?.withSecond(0)?.withNano(0)
            ?: LocalTime.of(9, 0))
    }
    var endDate by remember {
        mutableStateOf(initialEnd?.atZone(zone)?.toLocalDate() ?: LocalDate.now())
    }
    var endTime by remember {
        mutableStateOf(initialEnd?.atZone(zone)?.toLocalTime()?.withSecond(0)?.withNano(0)
            ?: LocalTime.of(10, 0))
    }

    var isTimeEnabled by remember { mutableStateOf(!initialIsAllDay) }
    var isDurationEnabled by remember { mutableStateOf(initialEnd != null) }
    var reminderConfig by remember { mutableStateOf<ReminderConfig?>(initialReminder) }
    var repeatConfig by remember { mutableStateOf<RepeatConfig?>(initialRepeat) }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            { onNoDate(); onDismiss() },
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
                                label = if (isDurationEnabled) stringResource(R.string.label_start) else stringResource(R.string.label_date),
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
                                value = formatReminder(reminderConfig),
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
                                value = formatRepeat(repeatConfig),
                                showArrow = true,
                                onClick = { showRepeatMenu = true }
                            )
                        }
                    }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Sub-Menus (Dialog/ModalBottomSheet composables are independent overlays)
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
                            .toLocalDate(); if (pickingTarget == "START") {
                                startDate = s
                                if (endDate.isBefore(startDate)) endDate = startDate
                            } else {
                                endDate = s
                                if (startDate.isAfter(endDate)) startDate = endDate
                            }
                    }; showDatePicker = false
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
                    onClick = { onReminderSelected(ReminderConfig(0, ReminderUnit.DAY, LocalTime.of(9, 0))) }
                )
                ReminderOptionItem(
                    "1 day before (09:00)",
                    onClick = { onReminderSelected(ReminderConfig(1, ReminderUnit.DAY, LocalTime.of(9, 0))) }
                )
                ReminderOptionItem(
                    "2 days before (09:00)",
                    onClick = { onReminderSelected(ReminderConfig(2, ReminderUnit.DAY, LocalTime.of(9, 0))) }
                )
                ReminderOptionItem(
                    "1 week before (09:00)",
                    onClick = { onReminderSelected(ReminderConfig(1, ReminderUnit.WEEK, LocalTime.of(9, 0))) }
                )
                ReminderOptionItem(
                    "1 month before (09:00)",
                    onClick = { onReminderSelected(ReminderConfig(1, ReminderUnit.MONTH, LocalTime.of(9, 0))) }
                )
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
                ReminderOptionItem("Daily", onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.DAY)) })
                ReminderOptionItem("Weekly", onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.WEEK)) })
                ReminderOptionItem("Monthly", onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.MONTH)) })
                ReminderOptionItem("Yearly", onClick = { onRepeatSelected(RepeatConfig(1, RepeatUnit.YEAR)) })
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
    var isBefore by remember { mutableStateOf(true) }
    var showTP by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
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
                TextButton(onClick = { onDone(ReminderConfig(count, unit, time, isBefore)) }) {
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
                            items = (0..60).map { it.toString() },
                            initialIndex = 1,
                            onItemSelected = { count = it.toInt() })
                        Picker(
                            items = ReminderUnit.entries.map {
                                it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
                            },
                            initialIndex = 2,
                            onItemSelected = { unit = ReminderUnit.valueOf(it.uppercase()) })
                        
                        Picker(
                            items = listOf("Before", "After"),
                            initialIndex = if (isBefore) 0 else 1,
                            onItemSelected = { isBefore = it == "Before" }
                        )
                    }
                    HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTP = true }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
                        Surface(
                            color = FocusTheme.colors.surfaceAlt,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = FocusTheme.typography.label
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
    var selectedDays by remember { mutableStateOf(setOf<Int>()) } // Mon=1, Sun=7

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surfaceAlt,
        dragHandle = null,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())) {
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
                            style = FocusTheme.typography.body.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.primary)
                        )
                        Picker(
                            items = (1..30).map { it.toString() },
                            initialIndex = interval - 1,
                            onItemSelected = { interval = it.toInt() })
                        Picker(
                            items = RepeatUnit.entries.map {
                                it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
                            },
                            initialIndex = unit.ordinal,
                            onItemSelected = { unit = RepeatUnit.valueOf(it.uppercase()) })
                    }
                }
            }
            if (unit == RepeatUnit.WEEK) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "On days",
                    style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary),
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    color = FocusTheme.colors.background,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        val dayNames = listOf(
                            "Sunday",
                            "Monday",
                            "Tuesday",
                            "Wednesday",
                            "Thursday",
                            "Friday",
                            "Saturday"
                        )
                        dayNames.forEachIndexed { index, name ->
                            val dayIdx = if (index == 0) 7 else index // Sunday = 7, Monday = 1...
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (selectedDays.contains(dayIdx)) selectedDays -= dayIdx else selectedDays += dayIdx }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
                                if (selectedDays.contains(dayIdx)) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = FocusTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
            Spacer(modifier = Modifier.height(32.dp))
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
            .collect { 
                if (it in items.indices) {
                    onItemSelected(items[it])
                }
            }
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
            Text(text, style = FocusTheme.typography.body.copy(fontSize = 15.sp, color = FocusTheme.colors.primary))
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
                    style = FocusTheme.typography.headline.copy(fontWeight = FontWeight.Bold, color = FocusTheme.colors.primary),
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

private fun formatReminder(reminder: ReminderConfig?): String {
    if (reminder == null) return "None"
    val unitStr = when (reminder.unit) {
        ReminderUnit.MINUTE -> "min"
        ReminderUnit.HOUR -> "hour"
        ReminderUnit.DAY -> "day"
        ReminderUnit.WEEK -> "week"
        ReminderUnit.MONTH -> "month"
    }
    val plural = if (reminder.value > 1) "s" else ""
    val relation = if (reminder.isBefore) "before" else "after"
    return if (reminder.value == 0) "On the day" else "${reminder.value} $unitStr$plural $relation"
}

private fun formatRepeat(repeat: RepeatConfig?): String {
    if (repeat == null) return "None"
    val unitStr = when (repeat.unit) {
        RepeatUnit.DAY -> "day"
        RepeatUnit.WEEK -> "week"
        RepeatUnit.MONTH -> "month"
        RepeatUnit.YEAR -> "year"
    }
    val plural = if (repeat.interval > 1) "s" else ""
    return "Every ${repeat.interval} $unitStr$plural"
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
                        style = FocusTheme.typography.label.copy(color = FocusTheme.colors.primary),
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

// ─── Selection Mode Action Bar ────────────────────────────────────────────────

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    modifier: Modifier = Modifier,
    onMoveToFolder: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit,
    onMarkDone: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FocusTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionActionButton(
                icon = Icons.Default.Folder,
                label = "Move",
                onClick = onMoveToFolder,
                enabled = selectedCount > 0
            )
            SelectionActionButton(
                icon = Icons.Default.DateRange,
                label = "Reschedule",
                onClick = onReschedule,
                enabled = selectedCount > 0
            )
            SelectionActionButton(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
                enabled = selectedCount > 0,
                tint = FocusTheme.colors.destructive
            )
            SelectionActionButton(
                icon = Icons.Default.Check,
                label = "Done",
                onClick = onMarkDone,
                enabled = selectedCount > 0
            )
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = FocusTheme.colors.primary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else FocusTheme.colors.divider,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = FocusTheme.typography.caption.copy(
                color = if (enabled) tint else FocusTheme.colors.divider,
                fontSize = 10.sp
            )
        )
    }
}

// ─── Confirm Dialogs ──────────────────────────────────────────────────────────

@Composable
fun ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = FocusTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Are you sure? This action cannot be undone.",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary),
                    textAlign = TextAlign.Center
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = FocusTheme.colors.destructive
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, FocusTheme.colors.destructive.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        "Confirm Delete",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.destructive)
                    )
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = FocusTheme.colors.primary
                    )
                ) {
                    Text("Cancel", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary))
                }
            }
        }
    }
}

@Composable
fun ConfirmMarkDoneDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = FocusTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Are you sure?",
                    style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary))
                    }
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = FocusTheme.colors.primary
                        )
                    ) {
                        Text("Mark as Done", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.background))
                    }
                }
            }
        }
    }
}

// ─── Move to Folder Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    lists: List<TaskList>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.secondary))
            }
            Text("Move to", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
            TextButton(onClick = onDismiss) {
                Text("Done", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary))
            }
        }
        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)

        Text(
            "My Folders",
            style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        lists.forEach { list ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(list.id) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Folder,
                    null,
                    modifier = Modifier.size(22.dp),
                    tint = FocusTheme.colors.primary
                )
                Text(
                    list.name,
                    modifier = Modifier.weight(1f),
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Reschedule Sheet ─────────────────────────────────────────────────────────

@Composable
fun RescheduleSheet(
    onDismiss: () -> Unit,
    onConfirm: (
        start: java.time.Instant?,
        end: java.time.Instant?,
        isAllDay: Boolean,
        reminder: ReminderConfig?,
        repeat: RepeatConfig?
    ) -> Unit
) {
    MinimalCalendarDialog(
        onDismiss = onDismiss,
        onConfigSave = { startDate, startTime, endDate, endTime, reminder, repeat ->
            val zone = java.time.ZoneId.systemDefault()
            val start = if (startTime != null)
                startDate.atTime(startTime).atZone(zone).toInstant()
            else
                startDate.atStartOfDay(zone).toInstant()
            val end = endDate?.let {
                if (endTime != null) it.atTime(endTime).atZone(zone).toInstant()
                else it.atStartOfDay(zone).toInstant()
            }
            val isAllDay = startTime == null
            onConfirm(start, end, isAllDay, reminder, repeat)
        }
    )
}

// ─── Focus Session Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionSheet(
    timerState: FocusTimerState,
    hasUsagePermission: Boolean = true,
    onDismiss: () -> Unit,
    onSetMode: (FocusMode) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onStartFocus: () -> Unit,
    onStopFocus: () -> Unit,
    onOpenUsageSettings: () -> Unit = {},
    onRefreshUsagePermission: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Handle + title
            Text(
                "Focus",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)

            // Mode selector
            FocusModeSelector(
                selectedMode = timerState.mode,
                enabled = !timerState.isRunning,
                onModeSelected = onSetMode
            )

            // Timer display / controls
            when (timerState.mode) {
                FocusMode.TIMER -> {
                    FocusTimerDisplay(
                        timerState = timerState,
                        onAdjustDuration = onAdjustDuration
                    )
                }
                FocusMode.STOPWATCH -> {
                    val elapsed = timerState.durationMinutes * 60L - timerState.remainingSeconds
                    Text(
                        formatSeconds(elapsed),
                        style = FocusTheme.typography.title.copy(fontSize = 56.sp,
                            color = FocusTheme.colors.primary
                        )
                    )
                }
                FocusMode.DISPLAY_CLOCK -> {
                    val now = java.time.LocalTime.now()
                    Text(
                        now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        style = FocusTheme.typography.title.copy(fontSize = 56.sp,
                            color = FocusTheme.colors.primary
                        )
                    )
                }
                FocusMode.MINIMAL -> {
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Finished label
            if (timerState.isFinished) {
                Text(
                    "Session complete!",
                    style = FocusTheme.typography.headline.copy(
                        color = FocusTheme.colors.success
                    )
                )
            }

            // Usage permission banner
            if (!hasUsagePermission) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cấp quyền Usage Access\nđể chặn ứng dụng",
                        style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary),
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onOpenUsageSettings()
                            onRefreshUsagePermission()
                        }
                    ) {
                        Text(
                            "Cấp quyền",
                            style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.primary)
                        )
                    }
                }
            }

            // Start / Stop button
            androidx.compose.material3.Button(
                onClick = if (timerState.isRunning) onStopFocus else onStartFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (timerState.isRunning) FocusTheme.colors.destructive
                                     else FocusTheme.colors.primary
                )
            ) {
                Text(
                    if (timerState.isRunning) "Stop Focus" else "Start Focus",
                    style = FocusTheme.typography.headline.copy(
                        color = FocusTheme.colors.background
                    )
                )
            }
        }
    }
}

@Composable
private fun FocusModeSelector(
    selectedMode: FocusMode,
    enabled: Boolean,
    onModeSelected: (FocusMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modeLabels = mapOf(
        FocusMode.MINIMAL to "Minimal",
        FocusMode.DISPLAY_CLOCK to "Display Clock",
        FocusMode.STOPWATCH to "Stopwatch",
        FocusMode.TIMER to "Timer"
    )

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(FocusTheme.colors.background)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modeLabels[selectedMode] ?: "Timer",
                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
            )
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = FocusTheme.colors.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FocusTheme.colors.surface)
        ) {
            FocusMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            modeLabels[mode] ?: mode.name,
                            color = if (mode == selectedMode) FocusTheme.colors.primary
                                    else FocusTheme.colors.secondary
                        )
                    },
                    trailingIcon = {
                        if (mode == selectedMode) {
                            Icon(Icons.Default.Check, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = { onModeSelected(mode); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FocusTimerDisplay(
    timerState: FocusTimerState,
    onAdjustDuration: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            formatSeconds(timerState.remainingSeconds),
            style = FocusTheme.typography.title.copy(fontSize = 56.sp,
                color = FocusTheme.colors.primary
            )
        )
        if (!timerState.isRunning) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onAdjustDuration(-5) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FocusTheme.colors.background)
                ) {
                    Text("−", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
                }
                Text(
                    "${timerState.durationMinutes} min",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                )
                IconButton(
                    onClick = { onAdjustDuration(5) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FocusTheme.colors.background)
                ) {
                    Text("+", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
                }
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
