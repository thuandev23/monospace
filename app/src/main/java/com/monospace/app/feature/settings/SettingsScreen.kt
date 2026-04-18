package com.monospace.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme
import java.util.Calendar

// ─── SettingItem: onClick nullable → không hiện arrow nếu null ───────────────

data class EditableListItem(
    val id: String,
    val title: String,
    val icon: @Composable () -> Unit,
    val isVisible: Boolean = true,
    val hasCheckbox: Boolean = true,
    val canReorder: Boolean = true
)

@Composable
fun SettingsScreen(
    onNavigateToFocus: () -> Unit = {},
    onNavigateToLists: () -> Unit = {},
    onNavigateToTaskDefault: () -> Unit = {},
    onNavigateToProUpgrade: () -> Unit = {},
    onNavigateToGeneral: () -> Unit = {},
    onNavigateToWallpaper: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToTabBar: () -> Unit = {},
    onNavigateToNotion: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val savedOrder by viewModel.sidebarItemOrder.collectAsState()
    val savedHidden by viewModel.sidebarHiddenItems.collectAsState()
    val dbFolders by viewModel.folders.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    // Main Section Items
    var mainItems by remember {
        mutableStateOf(
            listOf(
                EditableListItem(
                    "all",
                    "All",
                    {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }),
                EditableListItem("today", "Today", {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString(),
                            style = FocusTheme.typography.label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }),
                EditableListItem(
                    "upcoming",
                    "Upcoming",
                    {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    })
            )
        )
    }

    // Folders — derived from DB; editFolderItems dùng cho reorder trong edit mode
    val folderItems = dbFolders.map { list ->
        EditableListItem(
            id = list.id,
            title = list.name,
            icon = {
                Icon(
                    Icons.Default.Inbox,
                    null,
                    tint = FocusTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            hasCheckbox = false
        )
    }
    var editFolderItems by remember(folderItems) { mutableStateOf(folderItems) }

    // Reminders
    var reminderItems by remember {
        mutableStateOf(
            listOf(
                EditableListItem("sync_reminders", "Sync with Reminders", { ReminderIcon() }, canReorder = false)
            )
        )
    }

    // Notion
    var notionItems by remember {
        mutableStateOf(
            listOf(
                EditableListItem("notion", "Connect to Notion", { NotionIcon() }, canReorder = false)
            )
        )
    }

    // Restore thứ tự và visibility từ DataStore khi lần đầu load
    LaunchedEffect(savedOrder, savedHidden) {
        if (savedOrder.isNotEmpty()) {
            val ordered = savedOrder.mapNotNull { id -> mainItems.firstOrNull { it.id == id } }
            val remaining = mainItems.filter { it.id !in savedOrder }
            mainItems = (ordered + remaining).map { it.copy(isVisible = it.id !in savedHidden) }
        } else if (savedHidden.isNotEmpty()) {
            mainItems = mainItems.map { it.copy(isVisible = it.id !in savedHidden) }
        }
    }

    fun <T> List<T>.move(from: Int, to: Int): List<T> {
        if (from == to || from !in indices || to !in indices) return this
        return toMutableList().apply {
            add(to, removeAt(from))
        }
    }

    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text(stringResource(R.string.label_new_folder)) },
            text = {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text(stringResource(R.string.hint_folder_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName)
                        newFolderName = ""
                        showAddFolderDialog = false
                    }
                }) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = FocusTheme.colors.surface,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditMode) {
                            Text(
                                text = stringResource(R.string.action_done),
                                style = FocusTheme.typography.body.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocusTheme.colors.primary
                                ),
                                modifier = Modifier
                                    .clickable { isEditMode = false }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        } else {
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MoreHoriz,
                                        contentDescription = null,
                                        tint = FocusTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                MaterialTheme(
                                    shapes = MaterialTheme.shapes.copy(
                                        extraSmall = RoundedCornerShape(24.dp)
                                    ),
                                    colorScheme = MaterialTheme.colorScheme.copy(surface = FocusTheme.colors.surface)
                                ) {
                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false },
                                        modifier = Modifier
                                            .width(220.dp)
                                            .background(FocusTheme.colors.surface, RoundedCornerShape(24.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.label_edit_lists), style = FocusTheme.typography.body) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = FocusTheme.colors.primary) },
                                            onClick = { showMoreMenu = false; isEditMode = true }
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                                            color = FocusTheme.colors.divider.copy(alpha = 0.5f)
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.label_new_folder), style = FocusTheme.typography.body) },
                                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(20.dp), tint = FocusTheme.colors.primary) },
                                            onClick = { showMoreMenu = false; showAddFolderDialog = true }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.label_new_list), style = FocusTheme.typography.body) },
                                            leadingIcon = { ListIcon() },
                                            onClick = { showMoreMenu = false; onNavigateToLists() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.label_new_workspace), style = FocusTheme.typography.body) },
                                            leadingIcon = { NotionIcon(size = 20.dp) },
                                            onClick = { showMoreMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isEditMode) {
                FloatingActionButton(
                    onClick = onNavigateToLists,
                    containerColor = FocusTheme.colors.primary,
                    contentColor = FocusTheme.colors.background,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.label_new_list),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pro upgrade banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(FocusTheme.colors.primary)
                    .clickable { onNavigateToProUpgrade() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Monospace Pro",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        "7 days left in trial",
                        style = FocusTheme.typography.caption.copy(
                            color = FocusTheme.colors.background.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.background.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = FocusTheme.colors.background,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Upgrade",
                        style = FocusTheme.typography.label.copy(
                            color = FocusTheme.colors.background,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Tasks Section
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val itemsToRender =
                        if (isEditMode) mainItems else mainItems.filter { it.isVisible }
                    itemsToRender.forEachIndexed { index, item ->
                        if (isEditMode) {
                            var accumulatedDrag by remember { mutableStateOf(0f) }
                            val threshold = with(LocalDensity.current) { 56.dp.toPx() }

                            EditListItemRow(
                                item = item,
                                onToggleVisibility = {
                                    mainItems = mainItems.map { if (it.id == item.id) it.copy(isVisible = !it.isVisible) else it }
                                    viewModel.saveHiddenItems(mainItems.filter { !it.isVisible }.map { it.id }.toSet())
                                },
                                onDrag = { deltaY ->
                                    accumulatedDrag += deltaY
                                    if (accumulatedDrag > threshold && index < mainItems.size - 1) {
                                        mainItems = mainItems.move(index, index + 1)
                                        accumulatedDrag = 0f
                                        viewModel.saveItemOrder(mainItems.map { it.id })
                                    } else if (accumulatedDrag < -threshold && index > 0) {
                                        mainItems = mainItems.move(index, index - 1)
                                        accumulatedDrag = 0f
                                        viewModel.saveItemOrder(mainItems.map { it.id })
                                    }
                                },
                                onDragEnd = { accumulatedDrag = 0f }
                            )
                        } else {
                            // Main section items are display-only in view mode (sidebar customization)
                            SettingItem(icon = item.icon, title = item.title)
                        }
                        if (index < itemsToRender.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (isEditMode) 96.dp else 56.dp,
                                    end = 16.dp
                                ), color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Folders Section
            SectionHeader(
                title = stringResource(R.string.label_my_folders),
                showAdd = !isEditMode,
                onAddClick = { showAddFolderDialog = true }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val itemsToRender =
                        if (isEditMode) editFolderItems else folderItems.filter { it.isVisible }
                    itemsToRender.forEachIndexed { index, item ->
                        if (isEditMode) {
                            var accumulatedDrag by remember { mutableStateOf(0f) }
                            val threshold = with(LocalDensity.current) { 56.dp.toPx() }

                            EditListItemRow(
                                item = item,
                                onToggleVisibility = {
                                    editFolderItems =
                                        editFolderItems.map { if (it.id == item.id) it.copy(isVisible = !it.isVisible) else it }
                                },
                                onDrag = { deltaY ->
                                    accumulatedDrag += deltaY
                                    if (accumulatedDrag > threshold && index < editFolderItems.size - 1) {
                                        editFolderItems = editFolderItems.move(index, index + 1)
                                        accumulatedDrag = 0f
                                    } else if (accumulatedDrag < -threshold && index > 0) {
                                        editFolderItems = editFolderItems.move(index, index - 1)
                                        accumulatedDrag = 0f
                                    }
                                },
                                onDragEnd = { accumulatedDrag = 0f }
                            )
                        } else {
                            // Folder items are display-only in view mode
                            SettingItem(icon = item.icon, title = item.title)
                        }
                        if (index < itemsToRender.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (isEditMode) 96.dp else 56.dp,
                                    end = 16.dp
                                ), color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Reminders Section
            SectionHeader(title = stringResource(R.string.label_section_reminders))
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val itemsToRender =
                        if (isEditMode) reminderItems else reminderItems.filter { it.isVisible }
                    itemsToRender.forEachIndexed { index, item ->
                        if (isEditMode) {
                            EditListItemRow(
                                item = item,
                                onToggleVisibility = {
                                    reminderItems =
                                        reminderItems.map { if (it.id == item.id) it.copy(isVisible = !it.isVisible) else it }
                                }
                            )
                        } else {
                            SettingItem(
                                icon = item.icon,
                                title = item.title,
                                onClick = if (isPro) onNavigateToReminders else onNavigateToProUpgrade
                            )
                        }
                        if (index < itemsToRender.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (isEditMode) 96.dp else 56.dp,
                                    end = 16.dp
                                ), color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Task Default Section
            SectionHeader(title = "Task Default")
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Task Default",
                    onClick = onNavigateToTaskDefault
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Focus Mode Section
            SectionHeader(title = stringResource(R.string.label_section_focus_mode))
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    icon = {
                        Icon(
                            Icons.Default.Timer,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = stringResource(R.string.label_focus_profiles),
                    onClick = onNavigateToFocus
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Notion Section
            SectionHeader(title = stringResource(R.string.label_section_notion))
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val itemsToRender =
                        if (isEditMode) notionItems else notionItems.filter { it.isVisible }
                    itemsToRender.forEachIndexed { index, item ->
                        if (isEditMode) {
                            EditListItemRow(
                                item = item,
                                onToggleVisibility = {
                                    notionItems =
                                        notionItems.map { if (it.id == item.id) it.copy(isVisible = !it.isVisible) else it }
                                }
                            )
                        } else {
                            SettingItem(
                                icon = item.icon,
                                title = item.title,
                                onClick = if (isPro) onNavigateToNotion else onNavigateToProUpgrade
                            )
                        }
                        if (index < itemsToRender.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (isEditMode) 96.dp else 56.dp,
                                    end = 16.dp
                                ), color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // General Settings Section
            SectionHeader(title = "General")
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    icon = {
                        Icon(
                            Icons.Default.Tune,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "General",
                    onClick = onNavigateToGeneral
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section (Wallpapers + Tab Bar)
            SectionHeader(title = "Appearance")
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(
                        icon = {
                            Icon(
                                Icons.Default.Image,
                                null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = "Wallpapers",
                        onClick = if (isPro) onNavigateToWallpaper else onNavigateToProUpgrade
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                    )
                    SettingItem(
                        icon = {
                            Icon(
                                Icons.Default.Tab,
                                null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = "Tab Bar",
                        onClick = onNavigateToTabBar
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
            SectionHeader(title = "Info")
            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingItem(
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = FocusTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "About",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, showAdd: Boolean = false, onAddClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.secondary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        )
        if (showAdd) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = FocusTheme.colors.divider,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onAddClick() }
            )
        }
    }
}

@Composable
fun EditListItemRow(
    item: EditableListItem,
    onToggleVisibility: () -> Unit,
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.hasCheckbox) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleVisibility() }
                    .border(
                        width = 1.5.dp,
                        color = if (item.isVisible) Color.Black else FocusTheme.colors.divider,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isVisible) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.Black, CircleShape)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            item.icon()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            modifier = Modifier.weight(1f)
        )

        if (item.canReorder) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Reorder",
                tint = FocusTheme.colors.divider.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd
                        )
                    }
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}

@Composable
fun SettingItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: (() -> Unit)? = null,
    badge: String? = null
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)
    } else {
        Modifier.fillMaxWidth().padding(16.dp)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                style = FocusTheme.typography.label.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FocusTheme.colors.divider,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReminderIcon() {
    Column(modifier = Modifier.size(20.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        repeat(3) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            when (i) {
                                0 -> Color(0xFF4285F4)
                                1 -> Color(0xFFEA4335)
                                else -> Color(0xFFFBBC05)
                            }, CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(1.dp)
                        .background(FocusTheme.colors.divider)
                )
            }
        }
    }
}

@Composable
fun NotionIcon(size: androidx.compose.ui.unit.Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(FocusTheme.colors.primary, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "N",
            color = FocusTheme.colors.background,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5).sp
        )
    }
}

@Composable
fun ListIcon() {
    Column(modifier = Modifier.size(20.dp), verticalArrangement = Arrangement.Center) {
        repeat(3) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            when (i) {
                                0 -> Color(0xFF4285F4)
                                1 -> Color(0xFFEA4335)
                                else -> Color(0xFFFBBC05)
                            }, CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(1.dp)
                        .background(FocusTheme.colors.divider)
                )
            }
            if (i < 2) Spacer(modifier = Modifier.height(3.dp))
        }
    }
}
