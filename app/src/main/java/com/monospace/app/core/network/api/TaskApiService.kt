package com.monospace.app.core.network.api

import com.monospace.app.core.network.dto.ApiResponse
import com.monospace.app.core.network.dto.TaskDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {

    @GET("tasks")
    suspend fun getTasks(
        @Query("list_id") listId: String,
        @Query("updated_after") updatedAfter: Long? = null
    ): Response<ApiResponse<List<TaskDto>>>

    @POST("tasks")
    suspend fun createTask(
        @Body task: TaskDto
    ): Response<ApiResponse<TaskDto>>

    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body task: TaskDto
    ): Response<ApiResponse<TaskDto>>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
