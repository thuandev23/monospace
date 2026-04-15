package com.monospace.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

            // Notion logo placeholder
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

            Text(
                "Connect to Notion",
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                if (state.isConnected)
                    "Your Notion workspace is connected. Tasks will sync with your selected Notion database."
                else
                    "Sync your tasks with a Notion database. Connect your workspace to get started.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            if (state.isConnected) {
                // Connected state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Connected workspace",
                            style = FocusTheme.typography.caption.copy(
                                color = FocusTheme.colors.secondary,
                                fontSize = 12.sp,
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

                // Disconnect button
                IntegrationButton(
                    label = "Disconnect",
                    isPrimary = false,
                    onClick = { viewModel.disconnect() }
                )
            } else {
                // Connect button
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
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
