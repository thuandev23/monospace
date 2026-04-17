package com.monospace.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val taskIdKey = ActionParameters.Key<String>("taskId")
val taskDoneKey = ActionParameters.Key<Boolean>("isDone")

class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        val isDone = parameters[taskDoneKey] ?: false

        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val newStatus = if (isDone) "NOT_DONE" else "DONE"
        withContext(Dispatchers.IO) {
            ep.taskDao().updateTaskStatus(taskId, newStatus)
        }

        // Refresh all widget instances
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(TaskListWidget::class.java).forEach { id ->
            TaskListWidget().update(context, id)
        }
    }
}
