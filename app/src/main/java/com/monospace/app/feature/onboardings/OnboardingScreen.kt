package com.monospace.app.feature.onboardings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme
import kotlinx.coroutines.delay

private data class OnboardingPage(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                titleRes = R.string.onboarding_title_welcome,
                subtitleRes = R.string.onboarding_subtitle_welcome,
                icon = {}
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_schedule,
                subtitleRes = R.string.onboarding_subtitle_schedule,
                icon = {}
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_notifications,
                subtitleRes = R.string.onboarding_subtitle_notifications,
                icon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.size(64.dp), tint = FocusTheme.colors.primary) }
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onFinish() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Icon area
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(FocusTheme.colors.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 0) {
                        Icon(
                            painter = painterResource(id = R.drawable.content_copy_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = FocusTheme.colors.primary
                        )
                    } else {
                        pages[currentPage].icon()
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = stringResource(pages[currentPage].titleRes),
                    style = FocusTheme.typography.title.copy(
                        color = FocusTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(pages[currentPage].subtitleRes),
                    style = FocusTheme.typography.body.copy(
                        color = FocusTheme.colors.secondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { index ->
                        val isActive = index == currentPage
                        val width by animateFloatAsState(
                            targetValue = if (isActive) 24f else 8f,
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .size(width = width.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) FocusTheme.colors.primary
                                    else FocusTheme.colors.divider
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action button
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            // Last page — request notification permission then finish
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onFinish()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FocusTheme.colors.primary,
                        contentColor = FocusTheme.colors.background
                    )
                ) {
                    if (currentPage == pages.lastIndex) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.action_get_started), style = FocusTheme.typography.headline)
                        }
                    } else {
                        Text(stringResource(R.string.action_continue), style = FocusTheme.typography.headline)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentPage < pages.lastIndex) {
                    TextButton(onClick = onFinish) {
                        Text(
                            stringResource(R.string.action_skip),
                            style = FocusTheme.typography.label.copy(color = FocusTheme.colors.secondary)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
