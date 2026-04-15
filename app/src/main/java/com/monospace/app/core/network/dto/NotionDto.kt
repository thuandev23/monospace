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
