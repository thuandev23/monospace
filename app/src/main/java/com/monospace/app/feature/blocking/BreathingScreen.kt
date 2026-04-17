package com.monospace.app.feature.blocking

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.ui.theme.FocusTheme
import kotlinx.coroutines.delay

private const val TOTAL_CYCLES = 3
private const val PHASE_DURATION_MS = 4_000
private const val UNLOCK_DURATION_MS = 5 * 60 * 1_000L

private enum class BreathPhase(val label: String) {
    INHALE("Hít vào"),
    HOLD_IN("Giữ"),
    EXHALE("Thở ra"),
    HOLD_OUT("Giữ")
}

@Composable
fun BreathingScreen(onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }
    var cycle by remember { mutableIntStateOf(1) }
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repeat(TOTAL_CYCLES) { cycleIdx ->
            cycle = cycleIdx + 1

            phase = BreathPhase.INHALE
            scale.animateTo(1f, animationSpec = tween(PHASE_DURATION_MS))

            phase = BreathPhase.HOLD_IN
            delay(PHASE_DURATION_MS.toLong())

            phase = BreathPhase.EXHALE
            scale.animateTo(0.4f, animationSpec = tween(PHASE_DURATION_MS))

            phase = BreathPhase.HOLD_OUT
            delay(PHASE_DURATION_MS.toLong())
        }
        AppBlockingState.grantTemporaryUnlock(UNLOCK_DURATION_MS)
        completed = true
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
            if (completed) {
                Text(
                    "Tốt lắm!",
                    style = FocusTheme.typography.title.copy(
                        color = FocusTheme.colors.success,
                        fontSize = 28.sp
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "5 phút nghỉ ngơi",
                    style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        "Đóng",
                        style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
                    )
                }
            } else {
                Text(
                    "Thở để tạm nghỉ",
                    style = FocusTheme.typography.title.copy(
                        color = FocusTheme.colors.primary,
                        fontSize = 22.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Chu kỳ $cycle / $TOTAL_CYCLES",
                    style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
                )

                Spacer(Modifier.height(48.dp))

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(scale.value)
                            .background(
                                FocusTheme.colors.primary.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                FocusTheme.colors.primary.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                    Text(
                        phase.label,
                        style = FocusTheme.typography.headline.copy(
                            color = FocusTheme.colors.primary,
                            fontSize = 20.sp
                        )
                    )
                }

                Spacer(Modifier.height(48.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        "Hủy",
                        style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
                    )
                }
            }
        }
    }
}
