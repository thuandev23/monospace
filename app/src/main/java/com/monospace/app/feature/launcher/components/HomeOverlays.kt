package com.monospace.app.feature.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

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
                        icon = Icons.Default.Notifications,
                        label = "Reminder",
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
            Text(label, style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary))
        }
    }
}

@Composable
fun MinimalCalendarDialog(onDismiss: () -> Unit, onDateSelected: (LocalDate) -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = FocusTheme.colors.background,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val today = LocalDate.now()
                var currentMonth by remember { mutableStateOf(YearMonth.now()) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)} ${currentMonth.year}",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                    Row {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = FocusTheme.colors.primary)
                        }
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = FocusTheme.colors.primary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val daysInMonth = currentMonth.lengthOfMonth()
                val firstDayOfMonth = currentMonth.atDay(1)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
                
                val days = (1..daysInMonth).toList()
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(dayOfWeekOffset) { Spacer(modifier = Modifier.size(40.dp)) }
                    
                    items(days) { day ->
                        val date = currentMonth.atDay(day)
                        val isToday = date == today
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isToday) FocusTheme.colors.primary else Color.Transparent)
                                .clickable { 
                                    onDateSelected(date)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = FocusTheme.typography.label.copy(
                                    color = if (isToday) FocusTheme.colors.background else FocusTheme.colors.primary
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary))
                    }
                }
            }
        }
    }
}
