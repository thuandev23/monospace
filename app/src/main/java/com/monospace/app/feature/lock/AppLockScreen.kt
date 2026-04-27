package com.monospace.app.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.R
import com.monospace.app.ui.theme.FocusTheme

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    correctPin: String
) {
    BackHandler { }

    var input by remember { mutableStateOf("") }
    var shake by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.label_enter_pin),
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.height(40.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                val filled = i < input.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) FocusTheme.colors.primary
                            else FocusTheme.colors.surface
                        )
                        .border(1.5.dp, FocusTheme.colors.divider, CircleShape)
                )
            }
        }

        if (shake) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.error_wrong_pin),
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.destructive,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(Modifier.height(48.dp))

        // Numpad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )
        keys.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    NumKey(
                        label = key,
                        onClick = {
                            when (key) {
                                "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                "" -> {}
                                else -> {
                                    if (input.length < 4) {
                                        input += key
                                        shake = false
                                        if (input.length == 4) {
                                            if (input == correctPin) {
                                                onUnlock()
                                            } else {
                                                shake = true
                                                input = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NumKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (label.isEmpty()) FocusTheme.colors.background else FocusTheme.colors.surface)
            .clickable(enabled = label.isNotEmpty()) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = stringResource(R.string.label_remove_pin),
                tint = FocusTheme.colors.primary,
                modifier = Modifier.size(22.dp)
            )
        } else if (label.isNotEmpty()) {
            Text(
                label,
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}
