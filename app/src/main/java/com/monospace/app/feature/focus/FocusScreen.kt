package com.monospace.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FocusEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Focus Mode",
                        style = FocusTheme.typography.title.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FocusTheme.colors.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateSheet,
                containerColor = FocusTheme.colors.primary,
                contentColor = FocusTheme.colors.background,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo profile")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FocusTheme.colors.primary)
            }
        } else {
            FocusContent(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onActivate = viewModel::activateProfile,
                onDeactivate = viewModel::deactivate,
                onEdit = viewModel::showEditSheet,
                onDelete = viewModel::deleteProfile
            )
        }
    }

    if (uiState.showCreateSheet) {
        ProfileSheet(
            editingProfile = uiState.editingProfile,
            availableLists = uiState.availableLists,
            onDismiss = viewModel::dismissSheet,
            onSave = viewModel::saveProfile
        )
    }
}

@Composable
private fun FocusContent(
    uiState: FocusUiState,
    modifier: Modifier = Modifier,
    onActivate: (String) -> Unit,
    onDeactivate: () -> Unit,
    onEdit: (FocusProfile) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 8.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active profile banner
        uiState.activeProfile?.let { active ->
            item(key = "active_banner") {
                ActiveProfileBanner(
                    profile = active,
                    onStop = onDeactivate
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (uiState.profiles.isEmpty()) {
            item(key = "empty") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Chưa có Focus Profile",
                            style = FocusTheme.typography.title.copy(
                                color = FocusTheme.colors.secondary
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tạo profile để bắt đầu Focus Mode",
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.secondary.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        } else {
            items(items = uiState.profiles, key = { it.id }) { profile ->
                ProfileCard(
                    profile = profile,
                    linkedListName = uiState.availableLists.find { it.id == profile.linkedListId }?.name,
                    onActivate = { onActivate(profile.id) },
                    onDeactivate = onDeactivate,
                    onEdit = { onEdit(profile) },
                    onDelete = { onDelete(profile.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun ActiveProfileBanner(
    profile: FocusProfile,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.primary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(FocusTheme.colors.success)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Đang hoạt động",
                style = FocusTheme.typography.label.copy(
                    color = FocusTheme.colors.background.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
            Text(
                profile.name,
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.background,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        TextButton(onClick = onStop) {
            Text(
                "Dừng",
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.background
                )
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: FocusProfile,
    linkedListName: String?,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa profile?", color = FocusTheme.colors.primary) },
            text = { Text("Profile \"${profile.name}\" sẽ bị xóa vĩnh viễn.", color = FocusTheme.colors.secondary) },
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .border(
                width = if (profile.isActive) 1.5.dp else 0.dp,
                color = if (profile.isActive) FocusTheme.colors.primary else FocusTheme.colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (profile.isActive) FocusTheme.colors.success
                    else FocusTheme.colors.divider
                )
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.name,
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            )
            if (linkedListName != null) {
                Text(
                    linkedListName,
                    style = FocusTheme.typography.caption.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Activate / Deactivate button
        if (profile.isActive) {
            TextButton(onClick = onDeactivate) {
                Text("Tắt", color = FocusTheme.colors.secondary)
            }
        } else {
            Button(
                onClick = onActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    "Bật",
                    style = FocusTheme.typography.label.copy(color = FocusTheme.colors.background)
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // More actions
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = FocusTheme.colors.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(FocusTheme.colors.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Chỉnh sửa", color = FocusTheme.colors.primary) },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(16.dp))
                    },
                    onClick = { showMenu = false; onEdit() }
                )
                HorizontalDivider(color = FocusTheme.colors.divider)
                DropdownMenuItem(
                    text = { Text("Xóa", color = FocusTheme.colors.destructive) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = FocusTheme.colors.destructive, modifier = Modifier.size(16.dp))
                    },
                    onClick = { showMenu = false; showDeleteDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(
    editingProfile: FocusProfile?,
    availableLists: List<TaskList>,
    onDismiss: () -> Unit,
    onSave: (name: String, linkedListId: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(editingProfile?.id) {
        mutableStateOf(editingProfile?.name ?: "")
    }
    var selectedListId by remember(editingProfile?.id) {
        mutableStateOf(editingProfile?.linkedListId)
    }
    var showListMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (editingProfile != null) "Chỉnh sửa Profile" else "Tạo Focus Profile",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(Modifier.height(24.dp))

            // Name field
            Text(
                "Tên",
                style = FocusTheme.typography.label.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocusTheme.colors.background)
                    .border(1.dp, FocusTheme.colors.divider, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                textStyle = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary),
                singleLine = true,
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(
                            "VD: Deep Work, Study, Meeting...",
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.secondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(20.dp))

            // Linked list picker
            Text(
                "Gắn với danh sách task",
                style = FocusTheme.typography.label.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocusTheme.colors.background)
                        .border(1.dp, FocusTheme.colors.divider, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        availableLists.find { it.id == selectedListId }?.name ?: "Không gắn",
                        style = FocusTheme.typography.body.copy(
                            color = if (selectedListId != null) FocusTheme.colors.primary
                                    else FocusTheme.colors.secondary.copy(alpha = 0.5f)
                        )
                    )
                    TextButton(
                        onClick = { showListMenu = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Chọn",
                            style = FocusTheme.typography.label.copy(color = FocusTheme.colors.primary)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showListMenu,
                    onDismissRequest = { showListMenu = false },
                    modifier = Modifier.background(FocusTheme.colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Không gắn", color = FocusTheme.colors.secondary) },
                        onClick = { selectedListId = null; showListMenu = false }
                    )
                    HorizontalDivider(color = FocusTheme.colors.divider)
                    availableLists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name, color = FocusTheme.colors.primary) },
                            leadingIcon = {
                                if (list.id == selectedListId) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = FocusTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = { selectedListId = list.id; showListMenu = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onSave(name, selectedListId) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusTheme.colors.primary,
                    disabledContainerColor = FocusTheme.colors.divider
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (editingProfile != null) "Lưu thay đổi" else "Tạo Profile",
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.background)
                )
            }
        }
    }
}
