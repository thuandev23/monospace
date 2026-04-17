package com.monospace.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class NotionTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("workspace_name") val workspaceName: String?,
    @SerializedName("workspace_id") val workspaceId: String?
)

data class NotionDatabase(
    val id: String,
    val title: List<NotionRichText>
) {
    val name: String get() = title.firstOrNull()?.plainText ?: "Untitled"
}

data class NotionRichText(
    @SerializedName("plain_text") val plainText: String
)

data class NotionDatabasesResponse(
    val results: List<NotionDatabase>
)

// ─── Query database ───────────────────────────────────────────────────────────

data class NotionQueryResponse(
    val results: List<NotionPage>,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_cursor") val nextCursor: String?
)

data class NotionPage(
    val id: String,
    val properties: Map<String, NotionProperty>,
    @SerializedName("last_edited_time") val lastEditedTime: String?
)

data class NotionProperty(
    val type: String,
    val title: List<NotionRichText>? = null,
    @SerializedName("rich_text") val richText: List<NotionRichText>? = null,
    val checkbox: Boolean? = null,
    val date: NotionDateValue? = null,
    val select: NotionSelect? = null
) {
    fun plainText(): String? = when (type) {
        "title" -> title?.joinToString("") { it.plainText }
        "rich_text" -> richText?.joinToString("") { it.plainText }
        else -> null
    }
}

data class NotionDateValue(
    val start: String?
)

data class NotionSelect(
    val name: String?
)
