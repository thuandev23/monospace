package com.monospace.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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

        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val colors = WidgetThemeStore.load(context, appWidgetId).toColorSet()

        provideContent {
            TaskListWidgetContent(
                context = context,
                tasks = tasks.take(MAX_VISIBLE),
                remaining = (tasks.size - MAX_VISIBLE).coerceAtLeast(0),
                colors = colors
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
    remaining: Int,
    colors: WidgetColorSet = WidgetTheme.AUTO.toColorSet()
) {
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(16.dp)
            .clickable(openApp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks hôm nay",
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                    style = TextStyle(color = colors.secondary, fontSize = 12.sp)
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
                        style = TextStyle(color = colors.secondary, fontSize = 13.sp)
                    )
                }
            } else {
                tasks.forEach { task ->
                    TaskRow(task = task, colors = colors)
                    Spacer(GlanceModifier.height(8.dp))
                }
                if (remaining > 0) {
                    Text(
                        text = "+$remaining task khác",
                        style = TextStyle(color = colors.secondary, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, colors: WidgetColorSet) {
    val isDone = task.taskStatus == "DONE"
    val toggleAction = actionRunCallback<ToggleTaskAction>(
        actionParametersOf(taskIdKey to task.id, taskDoneKey to isDone)
    )
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(toggleAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(if (isDone) colors.secondary else colors.primary)
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = task.title,
            style = TextStyle(
                color = if (isDone) colors.secondary else colors.onSurface,
                fontSize = 13.sp
            ),
            maxLines = 1
        )
    }
}
