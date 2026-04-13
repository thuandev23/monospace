package com.monospace.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class TaskDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("is_completed") val isCompleted: Boolean = false,
    @SerializedName("list_id") val listId: String,
    @SerializedName("priority") val priority: Int = 0,
    @SerializedName("start_date_time") val startDateTime: Long? = null,
    @SerializedName("end_date_time") val endDateTime: Long? = null,
    @SerializedName("is_all_day") val isAllDay: Boolean = true,
    @SerializedName("reminder_value") val reminderValue: Int? = null,
    @SerializedName("reminder_unit") val reminderUnit: String? = null,
    @SerializedName("reminder_time") val reminderTime: String? = null,
    @SerializedName("repeat_interval") val repeatInterval: Int? = null,
    @SerializedName("repeat_unit") val repeatUnit: String? = null,
    @SerializedName("repeat_days_of_week") val repeatDaysOfWeek: String? = null,
    @SerializedName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

data class ApiResponse<T>(
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String? = null
)
