package com.monospace.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    fun updateAll(context: Context) {
        updateWidget(context, TaskListWidgetReceiver::class.java)
        updateWidget(context, ClockDateWidgetReceiver::class.java)
    }

    private fun updateWidget(context: Context, receiverClass: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass))
        if (ids.isEmpty()) return
        val intent = Intent(context, receiverClass).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
