package com.monospace.app.core.network.api

import com.monospace.app.core.network.dto.NotionDatabasesResponse
import com.monospace.app.core.network.dto.NotionTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
}
