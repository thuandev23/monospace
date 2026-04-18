package com.monospace.app.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.monospace.app.core.domain.model.AppInfo
import com.monospace.app.core.domain.model.DetoxBadge
import com.monospace.app.core.domain.model.DetoxStats
import com.monospace.app.core.domain.model.FocusProfile
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.domain.model.TaskList
import com.monospace.app.feature.launcher.components.Picker
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onNavigateToDetoxStats: () -> Unit = {},
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detoxStats by viewModel.detoxStats.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FocusEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
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
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FocusTheme.colors.primary)
            }
        } else {
            FocusContent(
                uiState = uiState,
                detoxStats = detoxStats,
                hasUsagePermission = hasUsagePermission,
                hasOverlayPermission = hasOverlayPermission,
                modifier = Modifier.padding(padding),
                onActivate = viewModel::activateProfile,
                onDeactivate = viewModel::deactivate,
                onEdit = viewModel::showEditSheet,
                onDelete = viewModel::deleteProfile,
                onViewDetoxStats = onNavigateToDetoxStats,
                onOpenUsageSettings = viewModel::openUsageSettings,
                onOpenOverlaySettings = viewModel::openOverlaySettings,
                onRefreshUsagePermission = viewModel::refreshPermissions
            )
        }
    }

    if (uiState.showCreateSheet) {
        ProfileSheet(
            editingProfile = uiState.editingProfile,
            availableLists = uiState.availableLists,
            installedApps = uiState.installedApps,
            hasUsagePermission = hasUsagePermission,
            onDismiss = viewModel::dismissSheet,
            onSave = viewModel::saveProfile,
            onOpenUsageSettings = viewModel::openUsageSettings
        )
    }
}

@Composable
private fun FocusContent(
    uiState: FocusUiState,
    detoxStats: DetoxStats = DetoxStats(),
    hasUsagePermission: Boolean = true,
    hasOverlayPermission: Boolean = true,
    modifier: Modifier = Modifier,
    onActivate: (String) -> Unit,
    onDeactivate: () -> Unit,
    onEdit: (FocusProfile) -> Unit,
    onDelete: (String) -> Unit,
    onViewDetoxStats: () -> Unit = {},
    onOpenUsageSettings: () -> Unit = {},
    onOpenOverlaySettings: () -> Unit = {},
    onRefreshUsagePermission: () -> Unit = {}
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
        // Usage Permission banner
        if (!hasUsagePermission && uiState.profiles.isNotEmpty()) {
            item {
                UsagePermissionBanner(
                    onGrant = {
                        onOpenUsageSettings()
                        onRefreshUsagePermission()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Overlay Permission banner — bắt buộc để auto-jump hoạt động
        if (!hasOverlayPermission && uiState.profiles.isNotEmpty()) {
            item {
                OverlayPermissionBanner(
                    onGrant = {
                        onOpenOverlaySettings()
                        onRefreshUsagePermission()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Streak + badges card
        if (detoxStats.totalSessions > 0 || detoxStats.badges.any { it.unlocked }) {
            item(key = "detox_stats") {
                DetoxStatsCard(stats = detoxStats, onViewMore = onViewDetoxStats)
                Spacer(Modifier.height(4.dp))
            }
        }

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
private fun UsagePermissionBanner(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Security,
                null,
                tint = FocusTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Quyền chặn ứng dụng",
                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.primary)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Cần quyền 'Usage Access' để có thể chặn các ứng dụng gây xao nhãng trong Focus Mode.",
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 13.sp
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = FocusTheme.colors.primary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Cấp quyền ngay",
                style = FocusTheme.typography.label.copy(color = FocusTheme.colors.background)
            )
        }
    }
}

@Composable
private fun OverlayPermissionBanner(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Security,
                null,
                tint = FocusTheme.colors.destructive,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Quyền hiển thị trên cùng",
                style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.destructive)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Cần quyền 'Xuất hiện trên cùng' để tự động nhảy về Monospace khi hết đếm ngược. Không có quyền này, màn hình chặn sẽ không hiện.",
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 13.sp
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = FocusTheme.colors.destructive),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Cấp quyền ngay",
                style = FocusTheme.typography.label.copy(color = FocusTheme.colors.background)
            )
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
            text = {
                Text(
                    "Profile \"${profile.name}\" sẽ bị xóa vĩnh viễn.",
                    color = FocusTheme.colors.secondary
                )
            },
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
        // Active indicator + Auto badge
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (profile.isActive) FocusTheme.colors.success
                        else FocusTheme.colors.divider
                    )
            )
            if (profile.schedule != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp, bottom = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF008080).copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "auto",
                        fontSize = 8.sp,
                        color = Color(0xFF008080),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.name,
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            )

            val subtitle = buildString {
                if (linkedListName != null) append(linkedListName)
                if (profile.allowedAppIds.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append("${profile.allowedAppIds.size} apps bị chặn")
                } else {
                    if (isNotEmpty()) append(" • ")
                    append("Không giới hạn")
                }
            }

            Text(
                subtitle,
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                )
            )

            profile.schedule?.let { sch ->
                val dayStr = formatDaysRange(sch.daysOfWeek)
                val time = "%02d:%02d–%02d:%02d".format(
                    sch.startHour,
                    sch.startMinute,
                    sch.endHour,
                    sch.endMinute
                )
                Text(
                    "$dayStr · $time",
                    style = FocusTheme.typography.caption.copy(
                        color = FocusTheme.colors.primary.copy(alpha = 0.6f),
                        fontSize = 11.sp
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
                        Icon(
                            Icons.Default.Edit,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = { showMenu = false; onEdit() }
                )
                HorizontalDivider(color = FocusTheme.colors.divider)
                DropdownMenuItem(
                    text = { Text("Xóa", color = FocusTheme.colors.destructive) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = FocusTheme.colors.destructive,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = { showMenu = false; showDeleteDialog = true }
                )
            }
        }
    }
}

private fun formatDaysRange(days: Set<Int>): String {
    if (days.isEmpty()) return ""
    if (days.size == 7) return "Mỗi ngày"
    val sorted = days.sorted()
    val isRange = sorted.size > 1 && sorted.last() - sorted.first() == sorted.size - 1

    val dayLabels =
        mapOf(1 to "T2", 2 to "T3", 3 to "T4", 4 to "T5", 5 to "T6", 6 to "T7", 7 to "CN")

    return if (isRange && sorted.size >= 3) {
        "${dayLabels[sorted.first()]}-${dayLabels[sorted.last()]}"
    } else {
        sorted.joinToString(", ") { dayLabels[it] ?: "" }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileSheet(
    editingProfile: FocusProfile?,
    availableLists: List<TaskList>,
    installedApps: List<AppInfo>,
    hasUsagePermission: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, linkedListId: String?, allowedAppIds: Set<String>, schedule: FocusSchedule?) -> Unit,
    onOpenUsageSettings: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPermissionSheet by remember { mutableStateOf(false) }

    var name by remember(editingProfile?.id) { mutableStateOf(editingProfile?.name ?: "") }
    var selectedListId by remember(editingProfile?.id) { mutableStateOf(editingProfile?.linkedListId) }
    var selectedApps by remember(editingProfile?.id) {
        mutableStateOf(editingProfile?.allowedAppIds ?: emptySet())
    }

    var scheduleEnabled by remember(editingProfile?.id) { mutableStateOf(editingProfile?.schedule != null) }
    var startHour by remember(editingProfile?.id) {
        mutableIntStateOf(
            editingProfile?.schedule?.startHour ?: 9
        )
    }
    var startMinute by remember(editingProfile?.id) {
        mutableIntStateOf(
            editingProfile?.schedule?.startMinute ?: 0
        )
    }
    var endHour by remember(editingProfile?.id) {
        mutableIntStateOf(
            editingProfile?.schedule?.endHour ?: 17
        )
    }
    var endMinute by remember(editingProfile?.id) {
        mutableIntStateOf(
            editingProfile?.schedule?.endMinute ?: 0
        )
    }
    var selectedDays by remember(editingProfile?.id) {
        mutableStateOf(editingProfile?.schedule?.daysOfWeek ?: setOf(1, 2, 3, 4, 5))
    }

    var showListMenu by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            Spacer(Modifier.height(20.dp))

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
                    if (name.isEmpty()) Text(
                        "VD: Deep Work, Study...",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.secondary.copy(alpha = 0.5f)
                        )
                    )
                    inner()
                }
            )

            Spacer(Modifier.height(16.dp))

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
                            color = if (selectedListId != null) FocusTheme.colors.primary else FocusTheme.colors.secondary.copy(
                                alpha = 0.5f
                            )
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
                    DropdownMenuItem(text = {
                        Text(
                            "Không gắn",
                            color = FocusTheme.colors.secondary
                        )
                    }, onClick = { selectedListId = null; showListMenu = false })
                    availableLists.forEach { list ->
                        DropdownMenuItem(text = {
                            Text(
                                list.name,
                                color = FocusTheme.colors.primary
                            )
                        }, onClick = { selectedListId = list.id; showListMenu = false })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Section: Apps bị chặn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Apps bị chặn",
                    style = FocusTheme.typography.label.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 12.sp
                    )
                )
                TextButton(
                    onClick = { showAppPicker = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "+ Thêm app",
                        style = FocusTheme.typography.label.copy(color = FocusTheme.colors.primary)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            if (selectedApps.isEmpty()) {
                Text(
                    "Không giới hạn apps",
                    style = FocusTheme.typography.body.copy(
                        color = FocusTheme.colors.secondary.copy(
                            alpha = 0.5f
                        )
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedApps.forEach { pkg ->
                        val appName = installedApps.find { it.packageName == pkg }?.name ?: pkg
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FocusTheme.colors.surface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                appName,
                                style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.primary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedApps = selectedApps - pkg },
                                tint = FocusTheme.colors.secondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Section: Lịch tự động
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bật lịch tự động",
                    style = FocusTheme.typography.label.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 12.sp
                    )
                )
                Switch(
                    checked = scheduleEnabled,
                    onCheckedChange = { scheduleEnabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = FocusTheme.colors.success)
                )
            }

            if (scheduleEnabled) {
                Spacer(Modifier.height(16.dp))

                Text(
                    "Bắt đầu",
                    style = FocusTheme.typography.label.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 11.sp
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Picker(
                        items = (0..23).map { "%02d".format(it) },
                        initialIndex = startHour
                    ) { startHour = it.toInt() }
                    Text(
                        ":",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = FocusTheme.typography.title
                    )
                    Picker(
                        items = (0..59).map { "%02d".format(it) },
                        initialIndex = startMinute
                    ) { startMinute = it.toInt() }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Kết thúc",
                    style = FocusTheme.typography.label.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 11.sp
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Picker(
                        items = (0..23).map { "%02d".format(it) },
                        initialIndex = endHour
                    ) { endHour = it.toInt() }
                    Text(
                        ":",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = FocusTheme.typography.title
                    )
                    Picker(
                        items = (0..59).map { "%02d".format(it) },
                        initialIndex = endMinute
                    ) { endMinute = it.toInt() }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Ngày trong tuần",
                    style = FocusTheme.typography.label.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 11.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    (1..7).forEach { day ->
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.background)
                                .clickable {
                                    selectedDays =
                                        if (isSelected) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayLabels[day - 1],
                                style = FocusTheme.typography.label.copy(
                                    color = if (isSelected) FocusTheme.colors.background else FocusTheme.colors.secondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!hasUsagePermission && selectedApps.isNotEmpty()) {
                        showPermissionSheet = true
                    } else {
                        val sch = if (scheduleEnabled) FocusSchedule(
                            startHour,
                            startMinute,
                            endHour,
                            endMinute,
                            selectedDays
                        ) else null
                        onSave(name, selectedListId, selectedApps, sch)
                    }
                },
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

    if (showPermissionSheet) {
        PermissionExplainerSheet(
            onDismiss = { showPermissionSheet = false },
            onGrant = {
                showPermissionSheet = false
                onOpenUsageSettings()
            }
        )
    }

    if (showAppPicker) {
        AppPickerSheet(
            installedApps = installedApps,
            selectedAppIds = selectedApps,
            onDismiss = { showAppPicker = false },
            onConfirmed = { selectedApps = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionExplainerSheet(onDismiss: () -> Unit, onGrant: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FocusTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security,
                null,
                tint = FocusTheme.colors.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Tại sao cần quyền chặn ứng dụng?",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Để tính năng chặn ứng dụng hoạt động, Monospace cần quyền 'Usage Access' từ hệ thống Android. Quyền này cho phép app nhận biết khi nào một ứng dụng gây xao nhãng đang mở để hiển thị màn hình chặn.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FocusTheme.colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cấp quyền trong Cài đặt",
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.background)
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Để sau", color = FocusTheme.colors.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    installedApps: List<AppInfo>,
    selectedAppIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirmed: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedAppIds) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FocusTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chọn apps bị chặn",
                    style = FocusTheme.typography.title.copy(
                        color = FocusTheme.colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                items(installedApps) { app ->
                    val isChecked = app.packageName in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (isChecked) selected -= app.packageName else selected += app.packageName }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            app.name,
                            modifier = Modifier.weight(1f),
                            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                        )
                        if (isChecked) Icon(
                            Icons.Default.Check,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
                }
            }

            Button(
                onClick = { onConfirmed(selected); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FocusTheme.colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Xong",
                    style = FocusTheme.typography.headline.copy(color = FocusTheme.colors.background)
                )
            }
        }
    }
}

// ─── Detox Stats ─────────────────────────────────────────────────────────────

@Composable
private fun DetoxStatsCard(stats: DetoxStats, onViewMore: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakStat(label = "Streak hiện tại", value = "${stats.currentStreak} ngày")
                StreakStat(label = "Dài nhất", value = "${stats.longestStreak} ngày")
                StreakStat(label = "Tổng sessions", value = "${stats.totalSessions}")
            }
            TextButton(onClick = onViewMore) {
                Text(
                    "Chi tiết",
                    style = FocusTheme.typography.caption.copy(
                        color = FocusTheme.colors.primary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        if (stats.badges.isNotEmpty()) {
            HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
            BadgesRow(badges = stats.badges)
        }
    }
}

@Composable
private fun StreakStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )
        Text(
            label,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun BadgesRow(badges: List<DetoxBadge>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Badges",
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 11.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            badges.forEach { badge ->
                BadgeChip(badge = badge)
            }
        }
    }
}

@Composable
private fun BadgeChip(badge: DetoxBadge) {
    val alpha = if (badge.unlocked) 1f else 0.35f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (badge.unlocked) FocusTheme.colors.primary.copy(alpha = 0.12f)
                else FocusTheme.colors.divider.copy(alpha = 0.5f)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(badge.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            badge.name,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.primary.copy(alpha = alpha),
                fontSize = 10.sp
            )
        )
    }
}
