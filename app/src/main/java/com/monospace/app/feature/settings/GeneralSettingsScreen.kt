package com.monospace.app.feature.settings

import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.AddTaskPosition
import com.monospace.app.core.domain.model.AppTheme
import com.monospace.app.core.domain.model.SecondStatus
import com.monospace.app.ui.theme.FocusTheme
import java.util.Locale

private data class LanguageOption(val tag: String, val display: String)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("", "System default"),
    LanguageOption("en", "English"),
    LanguageOption("vi", "Tiếng Việt")
)

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
    var showThemePicker by remember { mutableStateOf(false) }
    var showAddTaskPicker by remember { mutableStateOf(false) }
    var showSecondStatusPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val currentLanguageDisplay = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) "System default"
            else SUPPORTED_LANGUAGES.find { it.tag == locales.get(0).toLanguageTag().substringBefore("-") }?.display
                ?: locales.get(0).displayLanguage.replaceFirstChar { it.uppercase() }
        } else {
            val lang = Locale.getDefault().language
            SUPPORTED_LANGUAGES.find { it.tag == lang }?.display
                ?: Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() }
        }
    }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = FocusTheme.colors.primary
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Card 1: Appearance ─────────────────────────────────────────────
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    label = "Theme",
                    trailing = {
                        TrailingChevron(
                            value = if (settings.theme == AppTheme.MINIMALIST) "Minimalist" else "Reminders"
                        )
                    },
                    onClick = { showThemePicker = true }
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Default.Language,
                    label = "Language",
                    trailing = {
                        TrailingNav(value = currentLanguageDisplay)
                    },
                    onClick = { showLanguagePicker = true }
                )
            }

            // ── Card 2: Task behavior ──────────────────────────────────────────
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.PlaylistAdd,
                    label = "Add task to",
                    trailing = {
                        TrailingChevron(
                            value = if (settings.addTaskPosition == AddTaskPosition.BOTTOM) "Bottom" else "Top"
                        )
                    },
                    onClick = { showAddTaskPicker = true }
                )
            }

            // ── Card 3: Second status ──────────────────────────────────────────
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Tune,
                    label = "Second status",
                    trailing = {
                        TrailingChevron(
                            value = if (settings.secondStatus == SecondStatus.CANCELLED) "Cancelled" else "In progress"
                        )
                    },
                    onClick = { showSecondStatusPicker = true }
                )
            }

            // ── Card 4: Scroll ─────────────────────────────────────────────────
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.SwapVert,
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
            }
            Text(
                "When enabled, tasks are added to the reverse end of the list.",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // ── Card 5: Security ───────────────────────────────────────────────
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    label = "Passcode",
                    trailing = {
                        TrailingNav(value = if (lockPin != null) "Enabled" else "Not set")
                    },
                    onClick = { showPasscodeDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Pickers ────────────────────────────────────────────────────────────────
    if (showThemePicker) {
        RadioPickerSheet(
            title = "Theme",
            options = listOf("Minimalist", "Reminders"),
            selectedIndex = if (settings.theme == AppTheme.MINIMALIST) 0 else 1,
            onSelect = { idx ->
                viewModel.update(settings.copy(theme = if (idx == 0) AppTheme.MINIMALIST else AppTheme.REMINDERS))
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showAddTaskPicker) {
        RadioPickerSheet(
            title = "Add task to",
            options = listOf("Bottom", "Top"),
            selectedIndex = if (settings.addTaskPosition == AddTaskPosition.BOTTOM) 0 else 1,
            onSelect = { idx ->
                viewModel.update(settings.copy(addTaskPosition = if (idx == 0) AddTaskPosition.BOTTOM else AddTaskPosition.TOP))
                showAddTaskPicker = false
            },
            onDismiss = { showAddTaskPicker = false }
        )
    }

    if (showSecondStatusPicker) {
        RadioPickerSheet(
            title = "Second status",
            options = listOf("Cancelled", "In progress"),
            selectedIndex = if (settings.secondStatus == SecondStatus.CANCELLED) 0 else 1,
            onSelect = { idx ->
                viewModel.update(settings.copy(secondStatus = if (idx == 0) SecondStatus.CANCELLED else SecondStatus.IN_PROGRESS))
                showSecondStatusPicker = false
            },
            onDismiss = { showSecondStatusPicker = false }
        )
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            currentTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
                if (locales.isEmpty) "" else locales.get(0).toLanguageTag().substringBefore("-")
            } else {
                Locale.getDefault().language
            },
            onSelect = { tag ->
                showLanguagePicker = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val localeManager = context.getSystemService(LocaleManager::class.java)
                    localeManager.applicationLocales = if (tag.isEmpty()) LocaleList.getEmptyLocaleList()
                    else LocaleList.forLanguageTags(tag)
                } else {
                    var launched = false
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                        launched = true
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
            },
            onDismiss = { showLanguagePicker = false }
        )
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

// ── Shared card/row components ─────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = FocusTheme.colors.divider.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().clickable { onClick() }
    else
        Modifier.fillMaxWidth()

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = FocusTheme.colors.primary
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
        )
        trailing()
    }
}

@Composable
private fun TrailingChevron(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary, fontSize = 15.sp)
        )
        Spacer(Modifier.size(2.dp))
        Icon(
            Icons.Default.UnfoldMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = FocusTheme.colors.secondary
        )
    }
}

@Composable
private fun TrailingNav(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary, fontSize = 15.sp)
        )
        Spacer(Modifier.size(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = FocusTheme.colors.secondary
        )
    }
}

// ── Generic radio picker sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioPickerSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        Text(
            text = title,
            style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
        options.forEachIndexed { idx, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                )
                if (idx == selectedIndex) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                }
            }
            if (idx < options.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = FocusTheme.colors.divider.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ── Language picker sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    currentTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        Text(
            text = "Language",
            style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
        SUPPORTED_LANGUAGES.forEachIndexed { idx, lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(lang.tag) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lang.display,
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                )
                if (lang.tag == currentTag) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = FocusTheme.colors.primary
                    )
                }
            }
            if (idx < SUPPORTED_LANGUAGES.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = FocusTheme.colors.divider.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
            Text(
                text = "Language changes will open system settings on this Android version.",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ── Passcode dialog ────────────────────────────────────────────────────────────

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
                if (currentPin != null) "Change Passcode" else "Set Passcode",
                style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter a 4-digit PIN to protect Focus Mode.",
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
                    supportingText = if (error) {{ Text("PIN must be 4 digits") }} else null,
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
            TextButton(onClick = { if (pin.length == 4) onSave(pin) else error = true }) {
                Text("Save", color = FocusTheme.colors.primary)
            }
        },
        dismissButton = {
            Row {
                if (currentPin != null) {
                    TextButton(onClick = onClear) {
                        Text("Remove PIN", color = FocusTheme.colors.destructive)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = FocusTheme.colors.secondary)
                }
            }
        }
    )
}
