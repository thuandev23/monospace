package com.monospace.app.feature.blocking

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.ui.theme.FocusTheme

private enum class UnlockScreen { NONE, BREATHING, TOUCH_GRASS, PUSH_UP }

@Composable
fun BlockedAppOverlay(
    blockedPackage: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = blockedPackage != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var activeUnlock by remember { mutableStateOf(UnlockScreen.NONE) }

        AnimatedContent(
            targetState = activeUnlock,
            label = "unlock_screen"
        ) { screen ->
            when (screen) {
                UnlockScreen.BREATHING -> BreathingScreen(
                    onDismiss = { activeUnlock = UnlockScreen.NONE }
                )
                UnlockScreen.TOUCH_GRASS -> TouchGrassScreen(
                    onDismiss = { activeUnlock = UnlockScreen.NONE }
                )
                UnlockScreen.PUSH_UP -> PushUpScreen(
                    onDismiss = { activeUnlock = UnlockScreen.NONE }
                )
                UnlockScreen.NONE -> BlockedMainContent(
                    blockedPackage = blockedPackage ?: "",
                    onBreathing = { activeUnlock = UnlockScreen.BREATHING },
                    onTouchGrass = { activeUnlock = UnlockScreen.TOUCH_GRASS },
                    onPushUp = { activeUnlock = UnlockScreen.PUSH_UP },
                    onDeactivate = onDismiss
                )
            }
        }
    }
}

@Composable
private fun BlockedMainContent(
    blockedPackage: String,
    onBreathing: () -> Unit,
    onTouchGrass: () -> Unit,
    onPushUp: () -> Unit,
    onDeactivate: () -> Unit
) {
    val context = LocalContext.current
    val appName = remember(blockedPackage) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(blockedPackage, PackageManager.GET_META_DATA)
            ).toString()
        }.getOrDefault(blockedPackage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(FocusTheme.colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = FocusTheme.colors.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Đang trong Focus",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontSize = 24.sp
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "\"$appName\" bị chặn trong lúc Focus Mode đang bật.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Muốn nghỉ ngắn?",
                style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UnlockButton(
                    modifier = Modifier.weight(1f),
                    emoji = "🌬️",
                    label = "Thở",
                    sublabel = "5 phút",
                    onClick = onBreathing
                )
                UnlockButton(
                    modifier = Modifier.weight(1f),
                    emoji = "🌿",
                    label = "Ra ngoài",
                    sublabel = "15 phút",
                    onClick = onTouchGrass
                )
                UnlockButton(
                    modifier = Modifier.weight(1f),
                    emoji = "💪",
                    label = "Push-up",
                    sublabel = "3 phút",
                    onClick = onPushUp
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onDeactivate) {
                Text(
                    text = "Tắt Focus Mode",
                    style = FocusTheme.typography.body.copy(
                        color = FocusTheme.colors.destructive
                    )
                )
            }
        }
    }
}

@Composable
private fun UnlockButton(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    sublabel: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FocusTheme.colors.surface,
            contentColor = FocusTheme.colors.primary
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(
                label,
                style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.primary)
            )
            Text(
                sublabel,
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 10.sp
                )
            )
        }
    }
}
