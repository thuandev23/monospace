package com.monospace.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.monospace.app.MainActivity
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockDateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskCount = fetchTodayTaskCount(context)
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val date = formatDate(LocalDate.now())
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val colors = WidgetThemeStore.load(context, appWidgetId).toColorSet()

        provideContent {
            ClockDateWidgetContent(
                context = context,
                time = time,
                date = date,
                taskCount = taskCount,
                colors = colors
            )
        }
    }

    private suspend fun fetchTodayTaskCount(context: Context): Int {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return ep.taskDao().getTodayTasksSnapshot(dayStart, dayEnd).size
    }

    private fun formatDate(date: LocalDate): String {
        val dayOfWeek = when (date.dayOfWeek.value) {
            1 -> "Thứ 2"
            2 -> "Thứ 3"
            3 -> "Thứ 4"
            4 -> "Thứ 5"
            5 -> "Thứ 6"
            6 -> "Thứ 7"
            7 -> "CN"
            else -> ""
        }
        return "$dayOfWeek, ${date.format(DateTimeFormatter.ofPattern("dd/MM"))}"
    }
}

@Composable
private fun ClockDateWidgetContent(
    context: Context,
    time: String,
    date: String,
    taskCount: Int,
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
            .clickable(openApp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = time,
                style = TextStyle(
                    color = colors.primary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = date,
                style = TextStyle(color = colors.secondary, fontSize = 13.sp)
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = if (taskCount == 0) "Không có task" else "$taskCount task hôm nay",
                style = TextStyle(color = colors.accent, fontSize = 12.sp)
            )
        }
    }
}
