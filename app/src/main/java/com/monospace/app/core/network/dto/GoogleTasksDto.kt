package com.monospace.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class GoogleTaskList(
    val id: String,
    val title: String
)

data class GoogleTaskListsResponse(
    val items: List<GoogleTaskList>?
)

data class GoogleTask(
    val id: String?,
    val title: String?,
    val notes: String?,
    val status: String?,   // "needsAction" | "completed"
    val due: String?,      // RFC 3339, e.g. "2024-06-01T00:00:00.000Z"
    val updated: String?,
    val deleted: Boolean? = false
)

data class GoogleTasksResponse(
    val items: List<GoogleTask>?,
    @SerializedName("nextPageToken") val nextPageToken: String?
)

data class GoogleTaskBody(
    val title: String,
    val notes: String?,
    val status: String,  // "needsAction" | "completed"
    val due: String?     // RFC 3339 or null
)
