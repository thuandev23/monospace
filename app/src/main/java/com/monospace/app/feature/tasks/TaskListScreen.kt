package com.monospace.app.feature.tasks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun TaskListScreen(
    onListClick: (listId: String) -> Unit = {},
    onTodayClick: () -> Unit = {},
    onUpcomingClick: () -> Unit = {},
    onAllClick: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = FocusTheme.colors.primary,
                    contentColor = FocusTheme.colors.background
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Lists",
                        style = FocusTheme.typography.displayLarge.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = FocusTheme.colors.primary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(FocusTheme.colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit lists", color = FocusTheme.colors.primary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    if (uiState.isEditMode) viewModel.exitEditMode() else viewModel.enterEditMode()
                                }
                            )
                            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))
                            DropdownMenuItem(
                                text = { Text("New folder", color = FocusTheme.colors.primary) },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; viewModel.showCreateDialog() }
                            )
                            DropdownMenuItem(
                                text = { Text("New list", color = FocusTheme.colors.primary) },
                                leadingIcon = { Icon(Icons.Default.List, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; viewModel.showCreateDialog() }
                            )
                            DropdownMenuItem(
                                text = { Text("New workspace", color = FocusTheme.colors.primary) },
                                leadingIcon = { Icon(Icons.Default.Add, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Smart Lists ──────────────────────────────────────────────────
            item {
                SectionHeader("Smart Lists")
                Spacer(Modifier.height(8.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                ) {
                    SmartListRow(
                        icon = { Icon(Icons.Default.List, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "All",
                        count = uiState.allTaskCount.takeIf { it > 0 },
                        onClick = onAllClick
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    SmartListRow(
                        icon = { Icon(Icons.Default.Today, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Today",
                        count = uiState.todayTaskCount.takeIf { it > 0 },
                        onClick = onTodayClick
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    SmartListRow(
                        icon = { Icon(Icons.Default.DateRange, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Upcoming",
                        count = null,
                        onClick = onUpcomingClick
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── My Folders ────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("My Folders")
                    if (uiState.isEditMode) {
                        TextButton(onClick = viewModel::exitEditMode) {
                            Text("Done", style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary))
                        }
                    } else {
                        IconButton(onClick = viewModel::showCreateDialog, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Add, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            val userLists = uiState.lists.filter { it.id != "default" }

            if (userLists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusTheme.colors.surface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No folders yet. Tap + to create one.",
                            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(FocusTheme.colors.surface)
                    ) {
                        userLists.forEachIndexed { index, list ->
                            FolderRow(
                                list = list,
                                isEditMode = uiState.isEditMode,
                                canMoveUp = index > 0,
                                canMoveDown = index < userLists.size - 1,
                                onClick = { onListClick(list.id) },
                                onEdit = { viewModel.startEdit(list) },
                                onDelete = { viewModel.deleteList(list.id) },
                                onMoveUp = { viewModel.moveListUp(list) },
                                onMoveDown = { viewModel.moveListDown(list) }
                            )
                            if (index < userLists.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = FocusTheme.colors.divider.copy(alpha = 0.4f),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ── Reminders Section ──────────────────────────────────────────
            item {
                SectionHeader("Reminders")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                ) {
                    IntegrationRow(
                        icon = { Icon(Icons.Default.Notifications, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Sync with Reminders",
                        onClick = {}
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Notion Section ─────────────────────────────────────────────
            item {
                SectionHeader("Notion")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                ) {
                    IntegrationRow(
                        icon = {
                            Text("N", style = FocusTheme.typography.headline.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ))
                        },
                        label = "Connect to Notion",
                        onClick = {}
                    )
                }
            }
        }
    }

    // Dialog tạo list mới
    if (uiState.showCreateDialog) {
        CreateListDialog(
            onConfirm = viewModel::createList,
            onDismiss = viewModel::hideCreateDialog
        )
    }

    // Dialog đổi tên
    uiState.editingList?.let { list ->
        RenameListDialog(
            list = list,
            onConfirm = { newName -> viewModel.renameList(list, newName) },
            onDismiss = viewModel::cancelEdit
        )
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = FocusTheme.typography.caption.copy(
            color = FocusTheme.colors.secondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    )
}

@Composable
private fun SmartListRow(
    icon: @Composable () -> Unit,
    label: String,
    count: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
        )
        if (count != null && count > 0) {
            Text(
                count.toString(),
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FocusTheme.colors.divider,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FolderRow(
    list: TaskList,
    isEditMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditMode, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Folder, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(22.dp))

        Text(
            list.name,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
        )

        if (isEditMode) {
            // Up/Down reorder buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp),
                    enabled = canMoveUp
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        null,
                        tint = if (canMoveUp) FocusTheme.colors.primary else FocusTheme.colors.divider,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp),
                    enabled = canMoveDown
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null,
                        tint = if (canMoveDown) FocusTheme.colors.primary else FocusTheme.colors.divider,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = FocusTheme.colors.destructive, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = FocusTheme.colors.secondary, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(FocusTheme.colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = FocusTheme.colors.primary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = FocusTheme.colors.destructive) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = FocusTheme.colors.destructive, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = FocusTheme.colors.divider,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IntegrationRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = FocusTheme.colors.divider,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CreateListDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        title = { Text("New list", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("List name", color = FocusTheme.colors.secondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusTheme.colors.primary,
                    unfocusedBorderColor = FocusTheme.colors.divider,
                    focusedTextColor = FocusTheme.colors.primary,
                    unfocusedTextColor = FocusTheme.colors.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Create", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary))
            }
        }
    )
}

@Composable
private fun RenameListDialog(
    list: TaskList,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(list.id) { mutableStateOf(list.name) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        title = { Text("Rename", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusTheme.colors.primary,
                    unfocusedBorderColor = FocusTheme.colors.divider,
                    focusedTextColor = FocusTheme.colors.primary,
                    unfocusedTextColor = FocusTheme.colors.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Save", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary))
            }
        }
    )
}
