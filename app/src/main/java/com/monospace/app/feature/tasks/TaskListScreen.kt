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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun TaskListScreen(
    onListClick: (listId: String) -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = FocusTheme.colors.primary,
                contentColor = FocusTheme.colors.background,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo danh sách", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Danh sách",
                style = FocusTheme.typography.displayLarge.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.lists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FocusTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Chưa có danh sách nào",
                            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.lists, key = { it.id }) { list ->
                        TaskListItem(
                            list = list,
                            isDefault = list.id == "default",
                            onClick = { onListClick(list.id) },
                            onEdit = { viewModel.startEdit(list) },
                            onDelete = { viewModel.deleteList(list.id) }
                        )
                    }
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

@Composable
private fun TaskListItem(
    list: TaskList,
    isDefault: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(FocusTheme.colors.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = null,
                tint = FocusTheme.colors.background,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = list.name,
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            )
            if (isDefault) {
                Text(
                    text = "Danh sách mặc định",
                    style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary)
                )
            }
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Tuỳ chọn",
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(FocusTheme.colors.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Đổi tên", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(18.dp)) },
                    onClick = { showMenu = false; onEdit() }
                )
                if (!isDefault) {
                    DropdownMenuItem(
                        text = { Text("Xoá", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.destructive)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = FocusTheme.colors.destructive, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
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
        title = {
            Text("Danh sách mới", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Tên danh sách", color = FocusTheme.colors.secondary) },
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
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tạo", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Huỷ", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary))
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
        title = {
            Text("Đổi tên", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
        },
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
                Text("Lưu", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Huỷ", style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary))
            }
        }
    )
}
