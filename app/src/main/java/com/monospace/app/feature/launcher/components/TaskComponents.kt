package com.monospace.app.feature.launcher.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.monospace.app.R
import com.monospace.app.core.domain.model.Task
import com.monospace.app.ui.theme.FocusTheme

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
                    color = FocusTheme.colors.divider
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
        CircularCheckbox(
            checked = if (isSelectionMode) isSelected else task.isCompleted,
            onCheckedChange = { 
                if (isSelectionMode) onClick() else onToggle(it)
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = task.title,
            style = FocusTheme.typography.headline.copy(
                color = if (task.isCompleted) FocusTheme.colors.secondary else FocusTheme.colors.primary,
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
            .background(if (checked) FocusTheme.colors.primary else Color.Transparent)
            .border(2.dp, if (checked) FocusTheme.colors.primary else FocusTheme.colors.secondary, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Default.Check, null, tint = FocusTheme.colors.background, modifier = Modifier.size(16.dp))
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
