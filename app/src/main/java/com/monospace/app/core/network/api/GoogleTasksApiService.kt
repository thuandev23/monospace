package com.monospace.app.core.network.api

import com.monospace.app.core.network.dto.GoogleTaskBody
import com.monospace.app.core.network.dto.GoogleTask
import com.monospace.app.core.network.dto.GoogleTaskListsResponse
import com.monospace.app.core.network.dto.GoogleTasksResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleTasksApiService {

    @GET("tasks/v1/users/@me/lists")
    suspend fun getTaskLists(
        @Header("Authorization") bearer: String
    ): Response<GoogleTaskListsResponse>

    @GET("tasks/v1/lists/{listId}/tasks")
    suspend fun getTasks(
        @Path("listId") listId: String,
        @Header("Authorization") bearer: String,
        @Query("pageToken") pageToken: String? = null,
        @Query("showCompleted") showCompleted: Boolean = true,
        @Query("showDeleted") showDeleted: Boolean = false
    ): Response<GoogleTasksResponse>

    @POST("tasks/v1/lists/{listId}/tasks")
    suspend fun createTask(
        @Path("listId") listId: String,
        @Header("Authorization") bearer: String,
        @Body body: GoogleTaskBody
    ): Response<GoogleTask>

    @PATCH("tasks/v1/lists/{listId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("listId") listId: String,
        @Path("taskId") taskId: String,
        @Header("Authorization") bearer: String,
        @Body body: GoogleTaskBody
    ): Response<GoogleTask>

    @DELETE("tasks/v1/lists/{listId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("listId") listId: String,
        @Path("taskId") taskId: String,
        @Header("Authorization") bearer: String
    ): Response<Void>
}
