package com.monospace.app.core.network.api

import com.monospace.app.core.network.dto.NotionDatabasesResponse
import com.monospace.app.core.network.dto.NotionQueryResponse
import com.monospace.app.core.network.dto.NotionTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface NotionApiService {

    @POST("oauth/token")
    suspend fun exchangeCodeForToken(
        @Header("Authorization") basicAuth: String,
        @Body body: Map<String, String>
    ): Response<NotionTokenResponse>

    @GET("databases")
    suspend fun getDatabases(
        @Header("Authorization") bearerToken: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28"
    ): Response<NotionDatabasesResponse>

    @POST("databases/{databaseId}/query")
    suspend fun queryDatabase(
        @Path("databaseId") databaseId: String,
        @Header("Authorization") bearerToken: String,
        @Header("Notion-Version") notionVersion: String = "2022-06-28",
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Response<NotionQueryResponse>
}
