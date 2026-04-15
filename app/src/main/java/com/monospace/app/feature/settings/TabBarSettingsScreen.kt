package com.monospace.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.ui.theme.FocusTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBarSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: TabBarSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tab Bar",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "Choose which tabs appear in the bottom navigation bar. Today and Settings are always visible.",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 13.sp
                )
            )

            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Today — always on (read-only)
                    TabBarRow(
                        icon = {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FocusTheme.colors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString(),
                                    style = FocusTheme.typography.label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FocusTheme.colors.background
                                    )
                                )
                            }
                        },
                        label = "Today",
                        checked = true,
                        enabled = false,
                        onCheckedChange = {}
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                    )

                    // Upcoming
                    TabBarRow(
                        icon = {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = "Upcoming",
                        checked = settings.showUpcoming,
                        onCheckedChange = { viewModel.setShowUpcoming(it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                    )

                    // Search
                    TabBarRow(
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = "Search",
                        checked = settings.showSearch,
                        onCheckedChange = { viewModel.setShowSearch(it) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                        color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                    )

                    // Settings — always on (read-only)
                    TabBarRow(
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                null,
                                tint = FocusTheme.colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = "Settings",
                        checked = true,
                        enabled = false,
                        onCheckedChange = {}
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TabBarRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            label,
            style = FocusTheme.typography.body.copy(
                color = if (enabled) FocusTheme.colors.primary else FocusTheme.colors.secondary,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusTheme.colors.background,
                checkedTrackColor = FocusTheme.colors.primary,
                uncheckedThumbColor = FocusTheme.colors.background,
                uncheckedTrackColor = FocusTheme.colors.divider,
                disabledCheckedTrackColor = FocusTheme.colors.divider,
                disabledCheckedThumbColor = FocusTheme.colors.background
            )
        )
    }
}
