package com.monospace.app.core.domain.usecase

import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.network.api.TaskApiService
import com.monospace.app.core.network.dto.ApiResponse
import com.monospace.app.core.network.dto.TaskDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PullTasksUseCaseTest {

    private val apiService: TaskApiService = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val useCase = PullTasksUseCase(apiService, taskRepository)

    private fun makeDto(id: String, listId: String = "list-1") = TaskDto(
        id = id,
        title = "Task $id",
        listId = listId,
    )

    // ─── success path ─────────────────────────────────────────────────────────

    @Test
    fun invoke_returnsSuccessWithCountWhenResponseIsSuccessful() = runTest {
        val dtos = listOf(makeDto("t1"), makeDto("t2"), makeDto("t3"))
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = dtos))

        val result = useCase("list-1")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow())
    }

    @Test
    fun invoke_mergesRemoteTasksIntoRepositoryOnSuccess() = runTest {
        val dtos = listOf(makeDto("t1"), makeDto("t2"))
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = dtos))

        useCase("list-1")

        coVerify { taskRepository.mergeRemoteTasks(any()) }
    }

    @Test
    fun invoke_returnsSuccessWithZeroWhenBodyDataIsNull() = runTest {
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = null))

        val result = useCase("list-1")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun invoke_passesListIdAndUpdatedAfterToApiService() = runTest {
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = emptyList()))

        useCase("list-99", updatedAfter = 1234567890L)

        coVerify { apiService.getTasks("list-99", 1234567890L) }
    }

    @Test
    fun invoke_passesNullUpdatedAfterByDefault() = runTest {
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = emptyList()))

        useCase("list-1")

        coVerify { apiService.getTasks("list-1", null) }
    }

    // ─── error path ───────────────────────────────────────────────────────────

    @Test
    fun invoke_returnsFailureWhenServerReturnsErrorCode() = runTest {
        val errorResponse = Response.error<ApiResponse<List<TaskDto>>>(
            500,
            okhttp3.ResponseBody.create(null, "Internal Server Error")
        )
        coEvery { apiService.getTasks(any(), any()) } returns errorResponse

        val result = useCase("list-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("500") == true)
    }

    @Test
    fun invoke_returnsFailureWhenNetworkExceptionIsThrown() = runTest {
        coEvery { apiService.getTasks(any(), any()) } throws Exception("Network timeout")

        val result = useCase("list-1")

        assertTrue(result.isFailure)
        assertEquals("Network timeout", result.exceptionOrNull()?.message)
    }

    @Test
    fun invoke_doesNotCallMergeWhenResponseFails() = runTest {
        coEvery { apiService.getTasks(any(), any()) } throws RuntimeException("Connection refused")

        useCase("list-1")

        coVerify(exactly = 0) { taskRepository.mergeRemoteTasks(any()) }
    }

    @Test
    fun invoke_doesNotCallMergeWhenServerReturnsErrorCode() = runTest {
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))

        useCase("list-1")

        coVerify(exactly = 0) { taskRepository.mergeRemoteTasks(any()) }
    }

    // ─── DTO mapping ──────────────────────────────────────────────────────────

    @Test
    fun invoke_mapsDtoStatusToTaskStatus() = runTest {
        val dto = makeDto("t1").copy(taskStatus = "IN_PROGRESS")
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = listOf(dto)))

        val result = useCase("list-1")

        assertTrue(result.isSuccess)
        coVerify { taskRepository.mergeRemoteTasks(match { tasks ->
            tasks.first().status.name == "IN_PROGRESS"
        }) }
    }

    @Test
    fun invoke_mapsDtoPriorityToTaskPriority() = runTest {
        val dto = makeDto("t1").copy(priority = 3) // HIGH
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = listOf(dto)))

        useCase("list-1")

        coVerify { taskRepository.mergeRemoteTasks(match { tasks ->
            tasks.first().priority.value == 3
        }) }
    }

    @Test
    fun invoke_mergedTasksHaveSyncedStatus() = runTest {
        val dto = makeDto("t1")
        coEvery { apiService.getTasks(any(), any()) } returns
            Response.success(ApiResponse(data = listOf(dto)))

        useCase("list-1")

        coVerify { taskRepository.mergeRemoteTasks(match { tasks ->
            tasks.first().syncStatus.name == "SYNCED"
        }) }
    }
}
