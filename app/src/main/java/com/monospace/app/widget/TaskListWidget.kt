package com.monospace.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.monospace.app.MainActivity
import com.monospace.app.core.database.entity.TaskEntity
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.ZoneId

class TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val tasks = ep.taskDao().getTodayTasksSnapshot(dayStart, dayEnd)

        provideContent {
            TaskListWidgetContent(
                context = context,
                tasks = tasks.take(MAX_VISIBLE),
                remaining = (tasks.size - MAX_VISIBLE).coerceAtLeast(0)
            )
        }
    }

    companion object {
        const val MAX_VISIBLE = 5
    }
}

@Composable
private fun TaskListWidgetContent(
    context: Context,
    tasks: List<TaskEntity>,
    remaining: Int
) {
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .padding(16.dp)
            .clickable(openApp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks hôm nay",
                    style = TextStyle(
                        color = WidgetColors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                    style = TextStyle(
                        color = WidgetColors.secondary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(GlanceModifier.height(10.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Không có task nào hôm nay 🎉",
                        style = TextStyle(
                            color = WidgetColors.secondary,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                tasks.forEach { task ->
                    TaskRow(task = task)
                    Spacer(GlanceModifier.height(8.dp))
                }

                if (remaining > 0) {
                    Text(
                        text = "+$remaining task khác",
                        style = TextStyle(
                            color = WidgetColors.secondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity) {
    val isDone = task.taskStatus == "DONE"
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(
                    if (isDone) WidgetColors.secondary else WidgetColors.primary
                )
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = task.title,
            style = TextStyle(
                color = if (isDone) WidgetColors.secondary else WidgetColors.onSurface,
                fontSize = 13.sp
            ),
            maxLines = 1
        )
    }
}
