package com.monospace.app.core.domain.model

data class FocusProfile(
    val id: String,
    val name: String,
    val allowedAppIds: Set<String> = emptySet(),
    val linkedListId: String? = null,
    val schedule: FocusSchedule? = null,
    val isActive: Boolean = false
)

/**
 * Lịch tự động bật/tắt Focus Mode.
 * daysOfWeek: 1 = Thứ Hai, 7 = Chủ Nhật
 */
data class FocusSchedule(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Set<Int>
)
