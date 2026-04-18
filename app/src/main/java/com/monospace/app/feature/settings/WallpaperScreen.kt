package com.monospace.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.WallpaperAlignment
import com.monospace.app.core.domain.model.WallpaperConfig
import com.monospace.app.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val colorPresets = listOf(
    "#111111" to "#EFEFEF",
    "#1C1C1E" to "#F2F2F7",
    "#0A1628" to "#D8E8F5",
    "#1A1A2E" to "#E0DFFF",
    "#FFFFFF" to "#111111",
    "#F5F5F5" to "#1C1C1C",
    "#F7F0E6" to "#3C3230",
    "#2C3E50" to "#ECF0F1",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WallpaperViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val tasks by viewModel.todayTasks.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var isApplying by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.applyResult.collect { success ->
            isApplying = false
            snackbarHostState.showSnackbar(if (success) "Wallpaper applied" else "Failed to apply wallpaper")
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Wallpaper", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FocusTheme.colors.primary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isApplying) FocusTheme.colors.primary else FocusTheme.colors.surface)
                            .clickable(enabled = !isApplying) {
                                isApplying = true
                                viewModel.applyWallpaper()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            if (isApplying) "Applying…" else "Apply",
                            style = FocusTheme.typography.label.copy(
                                color = if (!isApplying) FocusTheme.colors.background else FocusTheme.colors.secondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
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
        ) {
            // Phone preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 19.5f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = FocusTheme.colors.divider,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    WallpaperCanvas(
                        config = config,
                        tasks = tasks,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("Style", "Content").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedTab == index) FocusTheme.colors.primary
                                else FocusTheme.colors.surface
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = FocusTheme.typography.label.copy(
                                color = if (selectedTab == index) FocusTheme.colors.background
                                        else FocusTheme.colors.secondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                    if (index == 0) Spacer(Modifier.width(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            when (selectedTab) {
                0 -> StyleTab(config = config, onConfigChange = viewModel::updateConfig)
                1 -> ContentTab(config = config, onConfigChange = viewModel::updateConfig)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StyleTab(config: WallpaperConfig, onConfigChange: (WallpaperConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Background",
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 12.sp
            )
        )

        ColorPresetGrid(
            presets = colorPresets,
            selectedBg = config.backgroundColorHex,
            onSelect = { bg, text ->
                onConfigChange(config.copy(backgroundColorHex = bg, textColorHex = text))
            }
        )

        HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))

        Text(
            "Position",
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 12.sp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperAlignment.entries.forEach { alignment ->
                val isSelected = config.contentAlignment == alignment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.surface)
                        .clickable { onConfigChange(config.copy(contentAlignment = alignment)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        alignment.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = FocusTheme.typography.label.copy(
                            color = if (isSelected) FocusTheme.colors.background else FocusTheme.colors.secondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPresetGrid(
    presets: List<Pair<String, String>>,
    selectedBg: String,
    onSelect: (bg: String, text: String) -> Unit
) {
    val rows = presets.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (bg, text) ->
                    val bgColor = bg.toComposeColor(Color.Black)
                    val isSelected = selectedBg.equals(bg, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.divider,
                                shape = CircleShape
                            )
                            .clickable { onSelect(bg, text) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentTab(config: WallpaperConfig, onConfigChange: (WallpaperConfig) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        ContentToggleRow(
            label = "Time",
            checked = config.showTime,
            onCheckedChange = { onConfigChange(config.copy(showTime = it)) }
        )
        HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))
        ContentToggleRow(
            label = "Date",
            checked = config.showDate,
            onCheckedChange = { onConfigChange(config.copy(showDate = it)) }
        )
        HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))
        ContentToggleRow(
            label = "Tasks",
            checked = config.showTasks,
            onCheckedChange = { onConfigChange(config.copy(showTasks = it)) }
        )

        if (config.showTasks) {
            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tasks shown",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StepButton(label = "−", enabled = config.taskLimit > 1) {
                        onConfigChange(config.copy(taskLimit = config.taskLimit - 1))
                    }
                    Text(
                        "${config.taskLimit}",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                    StepButton(label = "+", enabled = config.taskLimit < 10) {
                        onConfigChange(config.copy(taskLimit = config.taskLimit + 1))
                    }
                }
            }
        }

        HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))
        ContentToggleRow(
            label = "Show on home screen",
            checked = config.showOnHome,
            onCheckedChange = { onConfigChange(config.copy(showOnHome = it)) }
        )
        HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.4f))
        ContentToggleRow(
            label = "Auto-update daily",
            checked = config.autoUpdate,
            onCheckedChange = { onConfigChange(config.copy(autoUpdate = it)) }
        )
    }
}

@Composable
private fun ContentToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusTheme.colors.background,
                checkedTrackColor = FocusTheme.colors.primary,
                uncheckedThumbColor = FocusTheme.colors.secondary,
                uncheckedTrackColor = FocusTheme.colors.surface
            )
        )
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) FocusTheme.colors.surface else FocusTheme.colors.background)
            .border(1.dp, FocusTheme.colors.divider, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = FocusTheme.typography.body.copy(
                color = if (enabled) FocusTheme.colors.primary else FocusTheme.colors.divider,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
        )
    }
}
