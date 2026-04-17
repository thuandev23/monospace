package com.monospace.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotionIntegrationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotionIntegrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatabasePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val uri = activity?.intent?.data
        if (uri?.scheme == "monospace" && uri.host == "notion-auth") {
            uri.getQueryParameter("code")?.let { code ->
                viewModel.handleAuthCode(code)
                activity.intent = activity.intent.setData(null)
            }
        }
    }

    // Auto-show DB picker when databases just loaded and none selected
    LaunchedEffect(state.databases) {
        if (state.databases.isNotEmpty() && state.databaseId == null) {
            showDatabasePicker = true
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notion",
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            NotionLogo()

            Text(
                "Connect to Notion",
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                if (state.isConnected)
                    "Workspace đã kết nối. Tasks sẽ được sync với Notion database."
                else
                    "Sync tasks với Notion database. Kết nối workspace để bắt đầu.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            if (state.isConnected) {
                ConnectedContent(
                    state = state,
                    onPickDatabase = {
                        viewModel.fetchDatabases()
                        showDatabasePicker = true
                    },
                    onSyncNow = viewModel::syncNow,
                    onDisconnect = viewModel::disconnect
                )
            } else {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = FocusTheme.colors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    IntegrationButton(
                        label = "Connect with Notion",
                        isPrimary = true,
                        onClick = {
                            val url = NotionIntegrationViewModel.buildOAuthUrl()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }
            }

            if (state.error != null) {
                Text(
                    state.error!!,
                    style = FocusTheme.typography.caption.copy(
                        color = androidx.compose.ui.graphics.Color(0xFFCC3333),
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatabasePicker) {
        DatabasePickerSheet(
            databases = state.databases,
            isLoading = state.isLoading,
            onSelect = { db ->
                viewModel.selectDatabase(db)
                showDatabasePicker = false
            },
            onDismiss = { showDatabasePicker = false }
        )
    }
}

@Composable
private fun NotionLogo() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "N",
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.background,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ConnectedContent(
    state: NotionUiState,
    onPickDatabase: () -> Unit,
    onSyncNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    // Workspace info
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Workspace",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                state.workspaceName ?: "My Workspace",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    // Database selection
    InfoCard(clickable = true, onClick = onPickDatabase) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Database",
                    style = FocusTheme.typography.caption.copy(
                        color = FocusTheme.colors.secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    state.databaseName ?: "Chưa chọn — nhấn để chọn",
                    style = FocusTheme.typography.body.copy(
                        color = if (state.databaseName != null) FocusTheme.colors.primary
                        else FocusTheme.colors.secondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text("›", style = FocusTheme.typography.title.copy(color = FocusTheme.colors.secondary))
        }
    }

    // Last synced
    if (state.lastSynced != null) {
        Text(
            "Sync lần cuối: ${state.lastSynced}",
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 12.sp
            )
        )
    }

    // Sync now button
    if (state.databaseId != null) {
        if (state.isSyncing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = FocusTheme.colors.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    "Đang sync...",
                    style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
                )
            }
        } else {
            IntegrationButton(label = "Sync ngay", isPrimary = true, onClick = onSyncNow)
        }
    }

    IntegrationButton(label = "Disconnect", isPrimary = false, onClick = onDisconnect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabasePickerSheet(
    databases: List<com.monospace.app.core.network.dto.NotionDatabase>,
    isLoading: Boolean,
    onSelect: (com.monospace.app.core.network.dto.NotionDatabase) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FocusTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Chọn database",
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FocusTheme.colors.primary)
                }
            } else if (databases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Không tìm thấy database nào.\nHãy chia sẻ database với integration trong Notion.",
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.secondary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn {
                    items(databases) { db ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(db) }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                db.name,
                                style = FocusTheme.typography.body.copy(
                                    color = FocusTheme.colors.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = FocusTheme.colors.divider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun IntegrationButton(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPrimary) FocusTheme.colors.primary else FocusTheme.colors.surface)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = FocusTheme.typography.label.copy(
                color = if (isPrimary) FocusTheme.colors.background else FocusTheme.colors.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        )
    }
}
