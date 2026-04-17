package com.monospace.app.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TaskListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TaskListWidget()
}
