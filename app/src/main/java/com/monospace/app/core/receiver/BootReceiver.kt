package com.monospace.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.monospace.app.core.data.mapper.toDomain
import com.monospace.app.core.database.dao.TaskDao
import com.monospace.app.core.sync.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // BroadcastReceiver không có coroutine scope — dùng goAsync để không bị kill sớm
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = taskDao.getTasksWithFutureReminders(System.currentTimeMillis())
                tasks.forEach { entity ->
                    reminderScheduler.scheduleReminder(entity.toDomain())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
