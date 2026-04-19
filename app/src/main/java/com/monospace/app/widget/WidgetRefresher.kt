package com.monospace.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface WidgetRefresher {
    fun refresh()
}

@Singleton
class WidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {

    override fun refresh() {
        update(TaskListWidgetReceiver::class.java)
        update(ClockDateWidgetReceiver::class.java)
    }

    private fun update(receiverClass: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, receiverClass))
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, receiverClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        )
    }
}
