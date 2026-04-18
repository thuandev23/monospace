package com.monospace.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.WallpaperAlignment
import com.monospace.app.core.domain.model.WallpaperConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun WallpaperCanvas(
    config: WallpaperConfig,
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    live: Boolean = false,
    previewTime: LocalDateTime = LocalDateTime.now()
) {
    var currentTime by remember { mutableStateOf(previewTime) }

    if (live) {
        LaunchedEffect(Unit) {
            while (true) {
                val msUntilNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                delay(msUntilNextMinute)
                currentTime = LocalDateTime.now()
            }
        }
    }

    val bgColor = remember(config.backgroundColorHex) { config.backgroundColorHex.toComposeColor(Color.Black) }
    val textColor = remember(config.textColorHex) { config.textColorHex.toComposeColor(Color.White) }

    val timeText = remember(currentTime) { DateTimeFormatter.ofPattern("HH:mm").format(currentTime) }
    val dateText = remember(currentTime) { DateTimeFormatter.ofPattern("EEE, d MMM").format(currentTime) }
    val limitedTasks = remember(tasks, config.taskLimit) { tasks.take(config.taskLimit) }

    val boxAlignment = when (config.contentAlignment) {
        WallpaperAlignment.TOP -> Alignment.TopStart
        WallpaperAlignment.BOTTOM -> Alignment.BottomStart
        WallpaperAlignment.CENTER -> Alignment.CenterStart
    }

    Box(
        modifier = modifier.background(bgColor),
        contentAlignment = boxAlignment
    ) {
        Column(
            modifier = Modifier.padding(
                start = 28.dp,
                end = 28.dp,
                top = if (config.contentAlignment == WallpaperAlignment.TOP) 64.dp else 28.dp,
                bottom = if (config.contentAlignment == WallpaperAlignment.BOTTOM) 80.dp else 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (config.showTime) {
                Text(
                    text = timeText,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                )
            }

            if (config.showDate) {
                Text(
                    text = dateText,
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    )
                )
            }

            if (config.showTasks && limitedTasks.isNotEmpty()) {
                Spacer(Modifier.height(if (config.showTime || config.showDate) 28.dp else 0.dp))
                limitedTasks.forEach { task ->
                    Text(
                        text = "— ${task.title}",
                        style = TextStyle(
                            color = textColor.copy(alpha = 0.80f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier.padding(vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

internal fun String.toComposeColor(fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)
