package com.monospace.app.feature.launcher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.core.domain.model.AppShortcut
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun LauncherShortcutsSection(
    shortcuts: List<AppShortcut>,
    isEditMode: Boolean,
    onLaunch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onToggleEditMode: () -> Unit,
    onAddClick: () -> Unit
) {
    if (shortcuts.isEmpty() && !isEditMode) return

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Apps",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 11.sp
                )
            )
            Row {
                if (isEditMode) {
                    IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Thêm app",
                            tint = FocusTheme.colors.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onToggleEditMode, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "Xong" else "Chỉnh sửa",
                        tint = FocusTheme.colors.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        shortcuts.forEach { shortcut ->
            ShortcutRow(
                shortcut = shortcut,
                isEditMode = isEditMode,
                onLaunch = { onLaunch(shortcut.packageName) },
                onRemove = { onRemove(shortcut.packageName) }
            )
        }

        if (shortcuts.isEmpty() && isEditMode) {
            Text(
                text = "Nhấn + để thêm app",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = FocusTheme.colors.divider,
            thickness = 0.5.dp
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ShortcutRow(
    shortcut: AppShortcut,
    isEditMode: Boolean,
    onLaunch: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditMode, onClick = onLaunch)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = shortcut.label,
            style = FocusTheme.typography.body.copy(
                color = FocusTheme.colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
        )
        if (isEditMode) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = FocusTheme.colors.destructive,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Add App Sheet ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppShortcutSheet(
    installedApps: List<AppShortcut>,
    pinnedPackages: Set<String>,
    onAdd: (AppShortcut) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(query, installedApps) {
        if (query.isBlank()) installedApps
        else installedApps.filter { it.label.contains(query, ignoreCase = true) }
    }

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
        ) {
            Text(
                "Thêm app shortcut",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(Modifier.height(12.dp))

            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocusTheme.colors.background)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Tìm app...",
                                style = FocusTheme.typography.body.copy(
                                    color = FocusTheme.colors.secondary
                                )
                            )
                        }
                        inner()
                    },
                    singleLine = true
                )
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = FocusTheme.colors.secondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { query = "" }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isPinned = app.packageName in pinnedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isPinned) { onAdd(app); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = app.label,
                            style = FocusTheme.typography.body.copy(
                                color = if (isPinned) FocusTheme.colors.secondary
                                        else FocusTheme.colors.primary
                            )
                        )
                        if (isPinned) {
                            Text(
                                "đã thêm",
                                style = FocusTheme.typography.caption.copy(
                                    color = FocusTheme.colors.secondary,
                                    fontSize = 11.sp
                                )
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = FocusTheme.colors.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
