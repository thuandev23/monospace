package com.monospace.app.feature.launcher.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.R
import com.monospace.app.core.domain.model.SecondStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskAlignment
import com.monospace.app.core.domain.model.TaskDisplaySettings
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.ui.theme.FocusTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskList(
    activeTasks: List<Task>,
    completedTasks: List<Task>,
    isSelectionMode: Boolean,
    selectedTaskIds: Set<String>,
    onTaskToggle: (String, Boolean) -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onTaskSwipeDelete: (String) -> Unit = {},
    displaySettings: TaskDisplaySettings = TaskDisplaySettings(),
    reverseLayout: Boolean = false,
    onTaskStatusChange: ((String, TaskStatus) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 100.dp),
        reverseLayout = reverseLayout
    ) {
        items(items = activeTasks, key = { it.id }) { task ->
            SwipeableTaskItem(
                task = task,
                isSelected = selectedTaskIds.contains(task.id),
                isSelectionMode = isSelectionMode,
                onToggle = { onTaskToggle(task.id, it) },
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) },
                onSwipeComplete = { onTaskToggle(task.id, true) },
                onSwipeDelete = { onTaskSwipeDelete(task.id) },
                displaySettings = displaySettings,
                onStatusChange = onTaskStatusChange?.let { cb -> { s -> cb(task.id, s) } },
                modifier = Modifier.animateItem()
            )
        }

        if (activeTasks.isNotEmpty() && completedTasks.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = 0.5.dp,
                    color = FocusTheme.colors.divider
                )
            }
        }

        items(items = completedTasks, key = { it.id }) { task ->
            SwipeableTaskItem(
                task = task,
                isSelected = selectedTaskIds.contains(task.id),
                isSelectionMode = isSelectionMode,
                onToggle = { onTaskToggle(task.id, it) },
                onClick = { onTaskClick(task) },
                onLongClick = { onTaskLongClick(task) },
                onSwipeComplete = { onTaskToggle(task.id, false) },
                onSwipeDelete = { onTaskSwipeDelete(task.id) },
                displaySettings = displaySettings,
                onStatusChange = onTaskStatusChange?.let { cb -> { s -> cb(task.id, s) } },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: Task,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeComplete: () -> Unit,
    onSwipeDelete: () -> Unit,
    displaySettings: TaskDisplaySettings = TaskDisplaySettings(),
    onStatusChange: ((TaskStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Không cho swipe khi đang ở selection mode
    if (isSelectionMode) {
        TaskItem(
            task,
            isSelected,
            isSelectionMode,
            onToggle,
            onClick,
            onLongClick,
            displaySettings,
            onStatusChange,
            modifier
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeComplete(); true
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeDelete(); true
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> FocusTheme.colors.success.copy(alpha = 0.15f)
                    SwipeToDismissBoxValue.EndToStart -> FocusTheme.colors.destructive.copy(alpha = 0.15f)
                    else -> FocusTheme.colors.background
                },
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Hoàn thành",
                        tint = FocusTheme.colors.success,
                        modifier = Modifier.size(22.dp)
                    )

                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = FocusTheme.colors.destructive,
                        modifier = Modifier.size(22.dp)
                    )

                    else -> {}
                }
            }
        }
    ) {
        TaskItem(
            task,
            isSelected,
            isSelectionMode,
            onToggle,
            onClick,
            onLongClick,
            displaySettings,
            onStatusChange
        )
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
    displaySettings: TaskDisplaySettings = TaskDisplaySettings(),
    onStatusChange: ((TaskStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val textAlign = when (displaySettings.alignment) {
        TaskAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
        TaskAlignment.TRAILING -> androidx.compose.ui.text.style.TextAlign.End
        else -> androidx.compose.ui.text.style.TextAlign.Start
    }
    val rowAlignment = when (displaySettings.alignment) {
        TaskAlignment.CENTER -> Arrangement.Center
        TaskAlignment.TRAILING -> Arrangement.End
        else -> Arrangement.Start
    }

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
        verticalAlignment = Alignment.Top
    ) {
        if (displaySettings.showStatusCircle) {
            CircularCheckbox(
                checked = task.status == TaskStatus.DONE,
                onCheckedChange = { if (isSelectionMode) onClick() else onToggle(it) },
                taskStatus = if (isSelectionMode) (if (isSelected) TaskStatus.DONE else TaskStatus.NOT_DONE) else task.status,
                secondStatus = displaySettings.secondStatus,
                onStatusChange = if (isSelectionMode) null else onStatusChange,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = when (displaySettings.alignment) {
                TaskAlignment.CENTER -> Alignment.CenterHorizontally
                TaskAlignment.TRAILING -> Alignment.End
                else -> Alignment.Start
            }
        ) {
            Text(
                text = if (displaySettings.lowercase) task.title.lowercase() else task.title,
                style = FocusTheme.typography.headline.copy(
                    color = if (task.status == TaskStatus.DONE) FocusTheme.colors.secondary else FocusTheme.colors.primary,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else TextDecoration.None,
                    fontSize = displaySettings.fontSize.sp,
                    textAlign = textAlign
                )
            )

            // Dòng thông tin bổ sung: Ngày giờ, Nhắc nhở, Lặp lại
            if (task.status != TaskStatus.DONE) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hiển thị Ngày/Giờ
                    task.startDateTime?.let {
                        InfoTag(
                            icon = Icons.Default.Schedule,
                            text = formatInstant(it, task.isAllDay)
                        )
                    }

                    // Hiển thị Nhắc nhở
                    task.reminder?.let {
                        InfoTag(
                            icon = Icons.Default.Notifications,
                            text = "${it.value} ${it.unit.name.lowercase()}"
                        )
                    }

                    // Hiển thị Lặp lại
                    task.repeat?.let {
                        InfoTag(
                            icon = Icons.Default.Repeat,
                            text = it.unit.name.lowercase()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = FocusTheme.colors.secondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 11.sp
            )
        )
    }
}

private fun formatInstant(instant: Instant, isAllDay: Boolean): String {
    val formatter = if (isAllDay) {
        DateTimeFormatter.ofPattern("d MMM").withZone(ZoneId.systemDefault())
    } else {
        DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())
    }
    return formatter.format(instant)
}

@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    taskStatus: TaskStatus = if (checked) TaskStatus.DONE else TaskStatus.NOT_DONE,
    secondStatus: SecondStatus = SecondStatus.CANCELLED,
    onStatusChange: ((TaskStatus) -> Unit)? = null
) {
    val nextStatus = when (taskStatus) {
        TaskStatus.NOT_DONE -> if (secondStatus == SecondStatus.IN_PROGRESS) TaskStatus.IN_PROGRESS else TaskStatus.CANCELLED
        TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED -> TaskStatus.DONE
        TaskStatus.DONE -> TaskStatus.NOT_DONE
    }

    val bgColor = when (taskStatus) {
        TaskStatus.DONE -> FocusTheme.colors.primary
        TaskStatus.IN_PROGRESS -> FocusTheme.colors.success.copy(alpha = 0.2f)
        TaskStatus.CANCELLED -> FocusTheme.colors.destructive.copy(alpha = 0.15f)
        TaskStatus.NOT_DONE -> Color.Transparent
    }
    val borderColor = when (taskStatus) {
        TaskStatus.DONE -> FocusTheme.colors.primary
        TaskStatus.IN_PROGRESS -> FocusTheme.colors.success
        TaskStatus.CANCELLED -> FocusTheme.colors.destructive
        TaskStatus.NOT_DONE -> FocusTheme.colors.secondary
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable {
                if (onStatusChange != null) {
                    onStatusChange(nextStatus)
                } else {
                    onCheckedChange(!checked)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (taskStatus) {
            TaskStatus.DONE -> Icon(
                Icons.Default.Check,
                null,
                tint = FocusTheme.colors.background,
                modifier = Modifier.size(16.dp)
            )

            TaskStatus.CANCELLED -> Icon(
                Icons.Default.Close,
                null,
                tint = FocusTheme.colors.destructive,
                modifier = Modifier.size(14.dp)
            )

            TaskStatus.IN_PROGRESS -> Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(FocusTheme.colors.success)
            )

            TaskStatus.NOT_DONE -> {}
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
            tint = FocusTheme.colors.divider
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.msg_no_tasks),
            style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = FocusTheme.colors.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.action_create_task), color = FocusTheme.colors.background)
        }
    }
}
