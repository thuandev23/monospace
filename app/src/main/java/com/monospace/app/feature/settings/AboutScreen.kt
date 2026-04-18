package com.monospace.app.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var showWhatsNew by remember { mutableStateOf(false) }

    val aboutItems = listOf(
        AboutItem("What's New") { showWhatsNew = true },
        AboutItem("Help & Feedback") {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@monospace.app")
                putExtra(Intent.EXTRA_SUBJECT, "Monospace Feedback")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        AboutItem("Follow Us") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/monospaceapp")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        AboutItem("Rate the App") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        },
        AboutItem("Privacy Policy") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://monospace.app/privacy")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    )

    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About",
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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Surface(
                color = FocusTheme.colors.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    aboutItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { item.onClick() }
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.label,
                                style = FocusTheme.typography.body.copy(
                                    color = FocusTheme.colors.primary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null,
                                tint = FocusTheme.colors.divider,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (idx < aboutItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = FocusTheme.colors.divider.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Version info
            Text(
                "Monospace v1.0.0",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class AboutItem(val label: String, val onClick: () -> Unit)

private data class ChangelogEntry(val version: String, val changes: List<String>)

private val CHANGELOG = listOf(
    ChangelogEntry(
        version = "1.0.0",
        changes = listOf(
            "Focus schedule auto-enforcement — set a recurring window and Focus activates automatically",
            "App blocking during focus sessions with allowed-app whitelist",
            "Notion two-way sync with full pagination support",
            "Google Tasks integration with OAuth account picker",
            "Home screen widgets with custom wallpaper themes",
            "Detox statistics — daily screen-time and focus session tracking",
            "Focus profiles with linked task lists and schedules",
            "Reminder notifications that survive device reboot",
        )
    )
)

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FocusTheme.colors.surface,
        title = {
            Text(
                "What's New",
                style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CHANGELOG.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Version ${entry.version}",
                            style = FocusTheme.typography.body.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                        entry.changes.forEach { change ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "•",
                                    style = FocusTheme.typography.body.copy(
                                        color = FocusTheme.colors.secondary,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    change,
                                    style = FocusTheme.typography.body.copy(
                                        color = FocusTheme.colors.secondary,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Got it",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                )
            }
        }
    )
}
