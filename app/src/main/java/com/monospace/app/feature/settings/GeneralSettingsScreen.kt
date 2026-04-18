package com.monospace.app.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.AddTaskPosition
import com.monospace.app.core.domain.model.AppTheme
import com.monospace.app.core.domain.model.GeneralSettings
import com.monospace.app.core.domain.model.SecondStatus
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val lockPin by viewModel.lockPin.collectAsState()
    val context = LocalContext.current
    var showPasscodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "General",
                        style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FocusTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FocusTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Theme ──────────────────────────────────────────────────────────
            GeneralSectionLabel("Theme")
            SegmentedPicker(
                options = listOf("Minimalist", "Reminders"),
                selectedIndex = if (settings.theme == AppTheme.MINIMALIST) 0 else 1,
                onSelect = { idx ->
                    viewModel.update(
                        settings.copy(theme = if (idx == 0) AppTheme.MINIMALIST else AppTheme.REMINDERS)
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

            // ── Language ───────────────────────────────────────────────────────
            GeneralRow(
                label = "Language",
                trailing = {
                    Text(
                        "System",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.secondary,
                            fontSize = 15.sp
                        )
                    )
                },
                onClick = {
                    var launched = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                            launched = true
                        }
                    }
                    if (!launched) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_LOCALE_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

            // ── Add task to ────────────────────────────────────────────────────
            GeneralSectionLabel("Add task to")
            SegmentedPicker(
                options = listOf("Bottom", "Top"),
                selectedIndex = if (settings.addTaskPosition == AddTaskPosition.BOTTOM) 0 else 1,
                onSelect = { idx ->
                    viewModel.update(
                        settings.copy(addTaskPosition = if (idx == 0) AddTaskPosition.BOTTOM else AddTaskPosition.TOP)
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

            // ── Second status ──────────────────────────────────────────────────
            GeneralSectionLabel("Second status")
            SegmentedPicker(
                options = listOf("Cancelled", "In progress"),
                selectedIndex = if (settings.secondStatus == SecondStatus.CANCELLED) 0 else 1,
                onSelect = { idx ->
                    viewModel.update(
                        settings.copy(secondStatus = if (idx == 0) SecondStatus.CANCELLED else SecondStatus.IN_PROGRESS)
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

            // ── Reverse scroll direction ───────────────────────────────────────
            GeneralRow(
                label = "Reverse scroll direction",
                trailing = {
                    Switch(
                        checked = settings.reverseScrollDirection,
                        onCheckedChange = { viewModel.update(settings.copy(reverseScrollDirection = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FocusTheme.colors.background,
                            checkedTrackColor = FocusTheme.colors.primary,
                            uncheckedThumbColor = FocusTheme.colors.background,
                            uncheckedTrackColor = FocusTheme.colors.divider
                        )
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

            // ── Passcode PIN ───────────────────────────────────────────────────
            GeneralRow(
                label = "Passcode",
                trailing = {
                    Text(
                        if (lockPin != null) "Đã đặt" else "Chưa đặt",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.secondary,
                            fontSize = 15.sp
                        )
                    )
                },
                onClick = { showPasscodeDialog = true }
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPasscodeDialog) {
        PasscodeDialog(
            currentPin = lockPin,
            onDismiss = { showPasscodeDialog = false },
            onSave = { pin ->
                viewModel.setLockPin(pin)
                showPasscodeDialog = false
            },
            onClear = {
                viewModel.setLockPin(null)
                showPasscodeDialog = false
            }
        )
    }
}

@Composable
private fun PasscodeDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        title = {
            Text(
                if (currentPin != null) "Đổi Passcode" else "Đặt Passcode",
                style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Nhập mã PIN 4 chữ số để bảo vệ Focus Mode.",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = false
                        }
                    },
                    placeholder = { Text("••••") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    supportingText = if (error) {{ Text("PIN phải đủ 4 chữ số") }} else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FocusTheme.colors.primary,
                        unfocusedBorderColor = FocusTheme.colors.divider,
                        focusedTextColor = FocusTheme.colors.primary,
                        unfocusedTextColor = FocusTheme.colors.primary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length == 4) onSave(pin) else error = true
            }) {
                Text("Lưu", color = FocusTheme.colors.primary)
            }
        },
        dismissButton = {
            Row {
                if (currentPin != null) {
                    TextButton(onClick = onClear) {
                        Text("Xóa PIN", color = FocusTheme.colors.destructive)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = FocusTheme.colors.secondary)
                }
            }
        }
    )
}

@Composable
private fun GeneralSectionLabel(text: String) {
    Text(
        text,
        style = FocusTheme.typography.caption.copy(
            color = FocusTheme.colors.secondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    )
}

@Composable
private fun GeneralRow(
    label: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
        trailing()
    }
}

@Composable
private fun SegmentedPicker(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FocusTheme.colors.surface),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) FocusTheme.colors.primary else FocusTheme.colors.surface)
                    .clickable { onSelect(idx) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = FocusTheme.typography.label.copy(
                        color = if (selected) FocusTheme.colors.background else FocusTheme.colors.primary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
