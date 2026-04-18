package com.monospace.app.feature.settings

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersIntegrationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RemindersIntegrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingAccountName by remember { mutableStateOf<String?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // After consent, retry connecting with the same account
        pendingAccountName?.let { viewModel.connectAccount(it) }
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (accountName != null) {
            pendingAccountName = accountName
            viewModel.connectAccount(accountName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.consentIntent.collect { intent ->
            consentLauncher.launch(intent)
        }
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reminders",
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

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF4285F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Text(
                "Sync with Google Tasks",
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                if (uiState.connectedAccount != null)
                    "Syncing tasks with ${uiState.connectedAccount}"
                else
                    "Connect your Google account to sync tasks with Google Tasks.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            if (uiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        uiState.error!!,
                        style = FocusTheme.typography.caption.copy(
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (uiState.connectedAccount != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Connected account",
                            style = FocusTheme.typography.caption.copy(
                                color = FocusTheme.colors.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            uiState.connectedAccount!!,
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (uiState.lastSynced != null) {
                            Text(
                                "Last synced: ${uiState.lastSynced}",
                                style = FocusTheme.typography.caption.copy(
                                    color = FocusTheme.colors.secondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                ) {
                    Column {
                        SyncOptionRow("Sync direction", "Two-way")
                        SyncOptionRow("Auto sync", "Every 15 minutes")
                    }
                }

                // Sync now button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.primary)
                        .clickable(enabled = !uiState.isSyncing) { viewModel.syncNow() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = FocusTheme.colors.background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Sync Now",
                            style = FocusTheme.typography.label.copy(
                                color = FocusTheme.colors.background,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                        .clickable { viewModel.disconnect() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Disconnect",
                        style = FocusTheme.typography.label.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF4285F4))
                        .clickable(enabled = !uiState.isSyncing) {
                            val intent = AccountManager.newChooseAccountIntent(
                                null, null, arrayOf("com.google"),
                                null, null, null, null
                            )
                            accountPickerLauncher.launch(intent)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Sign in with Google",
                            style = FocusTheme.typography.label.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SyncOptionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary, fontSize = 15.sp)
        )
        Text(
            value,
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary, fontSize = 15.sp)
        )
    }
}
