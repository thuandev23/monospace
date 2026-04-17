package com.monospace.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(TaskListWidget::class.java).forEach { id ->
            TaskListWidget().update(context, id)
        }
        manager.getGlanceIds(ClockDateWidget::class.java).forEach { id ->
            ClockDateWidget().update(context, id)
        }
    }
}
