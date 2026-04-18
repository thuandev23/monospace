package com.monospace.app.core.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.monospace.app.core.domain.model.FocusSchedule
import com.monospace.app.core.receiver.FocusScheduleReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusScheduleEnforcer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * (Re)schedule alarms for a profile. Safe to call on every save.
     * If currently within the active window, also fires a START intent immediately.
     */
    fun scheduleProfile(profileId: String, schedule: FocusSchedule?) {
        cancelProfile(profileId)
        if (schedule == null) return

        scheduleNextStart(profileId, schedule)

        if (isWithinSchedule(schedule)) {
            // Already in the active window — fire start now and schedule today's stop
            val startIntent = Intent(context, FocusScheduleReceiver::class.java).apply {
                action = FocusScheduleReceiver.ACTION_START
                putExtra(FocusScheduleReceiver.EXTRA_PROFILE_ID, profileId)
            }
            context.sendBroadcast(startIntent)
            scheduleNextStop(profileId, schedule)
        }
    }

    fun scheduleNextStart(profileId: String, schedule: FocusSchedule) {
        val triggerMs = nextOccurrence(schedule.startHour, schedule.startMinute, schedule.daysOfWeek)
            ?: return
        setAlarm(profileId, FocusScheduleReceiver.ACTION_START, triggerMs)
    }

    fun scheduleNextStop(profileId: String, schedule: FocusSchedule) {
        val triggerMs = nextOccurrenceSameDay(schedule.endHour, schedule.endMinute) ?: return
        setAlarm(profileId, FocusScheduleReceiver.ACTION_STOP, triggerMs)
    }

    fun cancelProfile(profileId: String) {
        cancelAlarm(profileId, FocusScheduleReceiver.ACTION_START)
        cancelAlarm(profileId, FocusScheduleReceiver.ACTION_STOP)
    }

    fun isWithinSchedule(schedule: FocusSchedule): Boolean {
        val now = ZonedDateTime.now()
        val dayOfWeek = now.dayOfWeek.value  // 1=Mon, 7=Sun — matches FocusSchedule
        if (dayOfWeek !in schedule.daysOfWeek) return false
        val currentMinutes = now.hour * 60 + now.minute
        val startMinutes = schedule.startHour * 60 + schedule.startMinute
        val endMinutes = schedule.endHour * 60 + schedule.endMinute
        return currentMinutes in startMinutes until endMinutes
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun nextOccurrence(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long? {
        if (daysOfWeek.isEmpty()) return null
        val now = ZonedDateTime.now()
        for (daysAhead in 0..6) {
            val candidate = now.plusDays(daysAhead.toLong())
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            val dayOfWeek = candidate.dayOfWeek.value
            if (dayOfWeek in daysOfWeek && candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun nextOccurrenceSameDay(hour: Int, minute: Int): Long? {
        val now = ZonedDateTime.now()
        val candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        return if (candidate.isAfter(now)) candidate.toInstant().toEpochMilli() else null
    }

    private fun setAlarm(profileId: String, action: String, triggerAtMs: Long) {
        val pi = buildPendingIntent(profileId, action) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun cancelAlarm(profileId: String, action: String) {
        val pi = buildPendingIntent(profileId, action, noCreate = true) ?: return
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(
        profileId: String,
        action: String,
        noCreate: Boolean = false
    ): PendingIntent? {
        val intent = Intent(context, FocusScheduleReceiver::class.java).apply {
            this.action = action
            putExtra(FocusScheduleReceiver.EXTRA_PROFILE_ID, profileId)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (noCreate) PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(
            context,
            requestCode(profileId, action),
            intent,
            flags
        )
    }

    // Stable unique int per (profileId, action) — low collision risk for < 100 profiles
    private fun requestCode(profileId: String, action: String): Int =
        (profileId + action).hashCode() and Int.MAX_VALUE
}
