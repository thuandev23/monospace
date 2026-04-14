package com.monospace.app.feature.launcher.components

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.monospace.app.R
import com.monospace.app.core.domain.model.GroupOption
import com.monospace.app.core.domain.model.SortOption
import com.monospace.app.core.domain.model.ViewSettings
import com.monospace.app.ui.theme.FocusTheme
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSelectAll: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSelectedTasks: () -> Unit,
    onNavigateToLists: () -> Unit = {},
    viewSettings: ViewSettings = ViewSettings(),
    onViewSettingsChange: (ViewSettings) -> Unit = {}
) {
    var showViewOptions by remember { mutableStateOf(false) }

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
                            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                        )
                    }
                    if (selectedCount > 0) {
                        Text(
                            "$selectedCount selected",
                            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary),
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
                            modifier = Modifier.size(28.dp),
                            tint = FocusTheme.colors.primary
                        )
                    }
                    MonospaceDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismiss = { onMenuToggle(false) },
                        onSelectedTasks = onSelectedTasks,
                        onViewOptionsClick = {
                            onMenuToggle(false)
                            showViewOptions = true
                        },
                        onManageListsClick = onNavigateToLists
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCount > 0) {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = FocusTheme.colors.destructive
                                )
                            }
                        }
                        TextButton(onClick = onSelectAll) {
                            Text(
                                stringResource(R.string.action_select_all),
                                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
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
                style = FocusTheme.typography.displayLarge.copy(color = FocusTheme.colors.primary)
            )

            val calendar = Calendar.getInstance()
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = calendar.get(Calendar.DAY_OF_MONTH).toString(),
                    style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
                )
                Text(
                    text = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH)
                        ?: "",
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.secondary)
                )
            }
        }
    }

    if (showViewOptions) {
        ViewOptionsBottomSheet(
            settings = viewSettings,
            onDismiss = { showViewOptions = false },
            onSettingsChange = onViewSettingsChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOptionsBottomSheet(
    settings: ViewSettings,
    onDismiss: () -> Unit,
    onSettingsChange: (ViewSettings) -> Unit
) {
    var draft by remember { mutableStateOf(settings) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var showGroupDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        // Header
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
            Text("View", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
            TextButton(onClick = { onSettingsChange(draft); onDismiss() }) {
                Text("Done", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary))
            }
        }

        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Shown in list section
        Text(
            "Shown in list",
            style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        ViewToggleRow(
            icon = Icons.Default.History,
            label = "Overdue tasks",
            checked = draft.showOverdue,
            onToggle = { draft = draft.copy(showOverdue = it) }
        )
        ViewToggleRow(
            icon = Icons.Default.Sync,
            label = "In progress tasks",
            checked = draft.showInProgress,
            onToggle = { draft = draft.copy(showInProgress = it) }
        )
        ViewToggleRow(
            icon = Icons.Default.Check,
            label = "Completed tasks",
            checked = draft.showCompleted,
            onToggle = { draft = draft.copy(showCompleted = it) }
        )
        ViewToggleRow(
            icon = Icons.Default.Schedule,
            label = "Time",
            checked = draft.showTime,
            onToggle = { draft = draft.copy(showTime = it) }
        )
        ViewToggleRow(
            icon = Icons.Default.Folder,
            label = "Folder",
            checked = draft.showFolder,
            onToggle = { draft = draft.copy(showFolder = it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)

        // Sort section
        Text(
            "Sort",
            style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Box {
            ViewOptionRow(
                icon = Icons.Default.SwapVert,
                label = "Sort",
                value = draft.sortBy.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showSortDropdown = true }
            )
            DropdownMenu(
                expanded = showSortDropdown,
                onDismissRequest = { showSortDropdown = false },
                modifier = Modifier.background(FocusTheme.colors.surface)
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (draft.sortBy == option) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = FocusTheme.colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    option.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                                )
                            }
                        },
                        onClick = { draft = draft.copy(sortBy = option); showSortDropdown = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)

        // Group section
        Text(
            "Group",
            style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Box {
            ViewOptionRow(
                icon = Icons.Default.ViewModule,
                label = "Group",
                value = draft.groupBy.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showGroupDropdown = true }
            )
            DropdownMenu(
                expanded = showGroupDropdown,
                onDismissRequest = { showGroupDropdown = false },
                modifier = Modifier.background(FocusTheme.colors.surface)
            ) {
                GroupOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (draft.groupBy == option) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = FocusTheme.colors.primary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    option.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                                )
                            }
                        },
                        onClick = { draft = draft.copy(groupBy = option); showGroupDropdown = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ViewToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = FocusTheme.colors.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusTheme.colors.background,
                checkedTrackColor = FocusTheme.colors.success
            )
        )
    }
}

@Composable
private fun ViewOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = FocusTheme.colors.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
        )
        TextButton(onClick = onClick) {
            Text(value, style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.secondary))
            Icon(
                Icons.Default.Menu,
                null,
                modifier = Modifier.size(16.dp),
                tint = FocusTheme.colors.secondary
            )
        }
    }
}

@Composable
fun MonospaceDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectedTasks: () -> Unit,
    onViewOptionsClick: () -> Unit,
    onManageListsClick: () -> Unit = {}
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .width(200.dp)
                .border(0.5.dp, FocusTheme.colors.divider, RoundedCornerShape(20.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "View",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Menu,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                },
                onClick = onViewOptionsClick
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = FocusTheme.colors.divider
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Select tasks",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy_24dp),
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                },
                onClick = onSelectedTasks
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = FocusTheme.colors.divider
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Manage lists",
                        style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Menu,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                },
                onClick = { onDismiss(); onManageListsClick() }
            )
        }
    }
}
