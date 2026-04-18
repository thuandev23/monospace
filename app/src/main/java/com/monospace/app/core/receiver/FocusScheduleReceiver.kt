package com.monospace.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.monospace.app.core.domain.repository.FocusProfileRepository
import com.monospace.app.core.service.AppBlockingService
import com.monospace.app.core.sync.FocusScheduleEnforcer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusScheduleReceiver : BroadcastReceiver() {

    @Inject lateinit var focusProfileRepository: FocusProfileRepository
    @Inject lateinit var enforcer: FocusScheduleEnforcer

    override fun onReceive(context: Context, intent: Intent) {
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = focusProfileRepository.getById(profileId) ?: return@launch
                val schedule = profile.schedule ?: return@launch

                when (intent.action) {
                    ACTION_START -> {
                        focusProfileRepository.activate(profileId)
                        if (profile.allowedAppIds.isNotEmpty()) {
                            AppBlockingService.start(context)
                        }
                        enforcer.scheduleNextStop(profileId, schedule)
                        // Pre-schedule the next START for the following week occurrence
                        enforcer.scheduleNextStart(profileId, schedule)
                    }
                    ACTION_STOP -> {
                        focusProfileRepository.deactivate()
                        AppBlockingService.stop(context)
                        enforcer.scheduleNextStart(profileId, schedule)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.monospace.app.FOCUS_SCHEDULE_START"
        const val ACTION_STOP  = "com.monospace.app.FOCUS_SCHEDULE_STOP"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
    }
}
