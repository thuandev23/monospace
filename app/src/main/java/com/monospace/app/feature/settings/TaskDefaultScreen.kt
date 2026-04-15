package com.monospace.app.feature.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.TaskAlignment
import com.monospace.app.core.domain.model.TaskDisplaySettings
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDefaultScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: TaskDefaultViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Task Default", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Live Preview ─────────────────────────────────────────────────
            Text(
                "Preview",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            )
            TaskPreview(settings = settings)

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))

            // ── Show Status Circle ────────────────────────────────────────────
            SettingRow(
                label = "Show status circle",
                trailing = {
                    Switch(
                        checked = settings.showStatusCircle,
                        onCheckedChange = { viewModel.update(settings.copy(showStatusCircle = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FocusTheme.colors.background,
                            checkedTrackColor = FocusTheme.colors.primary,
                            uncheckedThumbColor = FocusTheme.colors.background,
                            uncheckedTrackColor = FocusTheme.colors.divider
                        )
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))

            // ── Lowercase ─────────────────────────────────────────────────────
            SettingRow(
                label = "Lowercase",
                trailing = {
                    Switch(
                        checked = settings.lowercase,
                        onCheckedChange = { viewModel.update(settings.copy(lowercase = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FocusTheme.colors.background,
                            checkedTrackColor = FocusTheme.colors.primary,
                            uncheckedThumbColor = FocusTheme.colors.background,
                            uncheckedTrackColor = FocusTheme.colors.divider
                        )
                    )
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))

            // ── Font Size ─────────────────────────────────────────────────────
            SettingRow(
                label = "Font size",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (settings.fontSize > 10) viewModel.update(settings.copy(fontSize = settings.fontSize - 1)) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FocusTheme.colors.surface)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            settings.fontSize.toString(),
                            style = FocusTheme.typography.headline.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { if (settings.fontSize < 36) viewModel.update(settings.copy(fontSize = settings.fontSize + 1)) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FocusTheme.colors.surface)
                        ) {
                            Icon(Icons.Default.Add, null, tint = FocusTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            HorizontalDivider(color = FocusTheme.colors.divider.copy(alpha = 0.5f))

            // ── Alignment ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Alignment",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                )
                val alignments = listOf(
                    TaskAlignment.LEADING to "Leading",
                    TaskAlignment.CENTER to "Center",
                    TaskAlignment.TRAILING to "Trailing"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(alignments) { (alignment, label) ->
                        val selected = settings.alignment == alignment
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) FocusTheme.colors.primary else FocusTheme.colors.surface)
                                .clickable { viewModel.update(settings.copy(alignment = alignment)) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = FocusTheme.typography.label.copy(
                                    color = if (selected) FocusTheme.colors.background else FocusTheme.colors.primary,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TaskPreview(settings: TaskDisplaySettings) {
    val sampleTasks = listOf("Design system review", "Write unit tests", "Update ROADMAP")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sampleTasks.forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (settings.showStatusCircle) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(FocusTheme.colors.surface)
                            .then(
                                Modifier.background(
                                    color = FocusTheme.colors.divider,
                                    shape = CircleShape
                                )
                            )
                    )
                }
                val displayTitle = if (settings.lowercase) task.lowercase() else task
                val align = when (settings.alignment) {
                    TaskAlignment.CENTER -> TextAlign.Center
                    TaskAlignment.TRAILING -> TextAlign.End
                    TaskAlignment.LEADING -> TextAlign.Start
                }
                Text(
                    displayTitle,
                    modifier = Modifier.weight(1f),
                    style = FocusTheme.typography.body.copy(
                        color = FocusTheme.colors.primary,
                        fontSize = settings.fontSize.sp
                    ),
                    textAlign = align
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary))
        trailing()
    }
}
