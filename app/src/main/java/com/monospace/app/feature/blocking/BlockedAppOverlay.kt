package com.monospace.app.feature.blocking

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.monospace.app.R
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.feature.focus.FocusTimerState
import com.monospace.app.ui.theme.FocusTheme

private enum class UnlockScreen { NONE, BREATHING, TOUCH_GRASS, PUSH_UP, PASSCODE }

@Composable
fun BlockedAppOverlay(
    blockedPackage: String?,
    activeProfileName: String? = null,
    timerState: FocusTimerState? = null,
    onDismiss: () -> Unit,
    settingsDataStore: SettingsDataStore? = null
) {
    AnimatedVisibility(
        visible = blockedPackage != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        var activeUnlock by remember { mutableStateOf(UnlockScreen.NONE) }
        val savedPin by if (settingsDataStore != null) {
            settingsDataStore.lockPin.collectAsState(initial = null)
        } else {
            remember { mutableStateOf<String?>(null) }
        }

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
                UnlockScreen.PASSCODE -> PasscodeScreen(
                    correctPin = savedPin ?: "0000",
                    onSuccess = {
                        AppBlockingState.grantTemporaryUnlock(30 * 60 * 1000L) // 30 mins
                        activeUnlock = UnlockScreen.NONE
                    },
                    onCancel = { activeUnlock = UnlockScreen.NONE }
                )
                UnlockScreen.NONE -> BlockedMainContent(
                    blockedPackage = blockedPackage ?: "",
                    activeProfileName = activeProfileName,
                    timerState = timerState,
                    hasPin = savedPin != null,
                    onBreathing = { activeUnlock = UnlockScreen.BREATHING },
                    onTouchGrass = { activeUnlock = UnlockScreen.TOUCH_GRASS },
                    onPushUp = { activeUnlock = UnlockScreen.PUSH_UP },
                    onPasscode = { activeUnlock = UnlockScreen.PASSCODE },
                    onDeactivate = onDismiss
                )
            }
        }
    }
}

@Composable
private fun BlockedMainContent(
    blockedPackage: String,
    activeProfileName: String?,
    timerState: FocusTimerState?,
    hasPin: Boolean,
    onBreathing: () -> Unit,
    onTouchGrass: () -> Unit,
    onPushUp: () -> Unit,
    onPasscode: () -> Unit,
    onDeactivate: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val appInfo = remember(blockedPackage) {
        runCatching {
            val info = pm.getApplicationInfo(blockedPackage, PackageManager.GET_META_DATA)
            val name = pm.getApplicationLabel(info).toString()
            val icon = pm.getApplicationIcon(info).toBitmap().asImageBitmap()
            name to icon
        }.getOrNull()
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
            if (appInfo != null) {
                Image(
                    bitmap = appInfo.second,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = appInfo.first,
                    style = FocusTheme.typography.title.copy(
                        color = FocusTheme.colors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
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
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (activeProfileName != null) stringResource(R.string.label_focusing_on, activeProfileName) else stringResource(R.string.label_in_focus),
                style = FocusTheme.typography.headline.copy(
                    color = FocusTheme.colors.primary,
                    fontSize = 24.sp
                )
            )

            if (timerState != null && timerState.isRunning) {
                Spacer(Modifier.height(8.dp))
                CountdownDisplay(timerState.remainingSeconds)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.msg_app_blocked_desc, appInfo?.first ?: blockedPackage),
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.label_want_a_break),
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
                    label = stringResource(R.string.label_unlock_breath),
                    sublabel = stringResource(R.string.label_duration_5m),
                    onClick = onBreathing
                )
                UnlockButton(
                    modifier = Modifier.weight(1f),
                    emoji = "🌿",
                    label = stringResource(R.string.label_unlock_grass),
                    sublabel = stringResource(R.string.label_duration_15m),
                    onClick = onTouchGrass
                )
                UnlockButton(
                    modifier = Modifier.weight(1f),
                    emoji = "💪",
                    label = stringResource(R.string.label_unlock_pushup),
                    sublabel = stringResource(R.string.label_duration_3m),
                    onClick = onPushUp
                )
            }

            Spacer(Modifier.height(24.dp))

            if (hasPin) {
                TextButton(onClick = onPasscode) {
                    Text(
                        text = stringResource(R.string.action_unlock_passcode),
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.primary
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            TextButton(onClick = onDeactivate) {
                Text(
                    text = stringResource(R.string.action_turn_off_focus),
                    style = FocusTheme.typography.body.copy(
                        color = FocusTheme.colors.destructive
                    )
                )
            }
        }
    }
}

@Composable
private fun PasscodeScreen(
    correctPin: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.label_passcode),
            style = FocusTheme.typography.title.copy(color = FocusTheme.colors.primary)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val filled = index < input.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) FocusTheme.colors.primary 
                            else FocusTheme.colors.divider
                        )
                )
            }
        }
        
        if (isError) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.error_wrong_pin), // Using existing string
                style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.destructive)
            )
        }
        
        Spacer(Modifier.height(48.dp))
        
        // PIN Pad
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "back")
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (j in 0 until 3) {
                        val item = numbers[i * 3 + j]
                        if (item.isEmpty()) {
                            Spacer(Modifier.size(64.dp))
                        } else if (item == "back") {
                            IconButton(
                                onClick = { if (input.isNotEmpty()) input = input.dropLast(1); isError = false },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = FocusTheme.colors.primary)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(FocusTheme.colors.surface)
                                    .clickable {
                                        if (input.length < 4) {
                                            input += item
                                            isError = false
                                            if (input.length == 4) {
                                                if (input == correctPin) onSuccess()
                                                else {
                                                    isError = true
                                                    input = ""
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    item,
                                    style = FocusTheme.typography.title.copy(
                                        fontSize = 24.sp,
                                        color = FocusTheme.colors.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.action_cancel), style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary))
        }
    }
}

@Composable
private fun CountdownDisplay(remainingSeconds: Long) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    Text(
        text = stringResource(R.string.label_remaining_time, minutes, seconds),
        style = FocusTheme.typography.title.copy(
            color = FocusTheme.colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    )
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
