package com.monospace.app.feature.settings

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.BuildConfig
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.network.api.NotionApiService
import com.monospace.app.core.network.dto.NotionDatabase
import com.monospace.app.core.network.dto.NotionPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class NotionUiState(
    val isConnected: Boolean = false,
    val workspaceName: String? = null,
    val databaseId: String? = null,
    val databaseName: String? = null,
    val lastSynced: String? = null,
    val databases: List<NotionDatabase> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotionIntegrationViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val notionApiService: NotionApiService,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _databases = MutableStateFlow<List<NotionDatabase>>(emptyList())

    val uiState: StateFlow<NotionUiState> = combine(
        settingsDataStore.notionAccessToken,
        settingsDataStore.notionWorkspaceName,
        settingsDataStore.notionDatabaseId,
        settingsDataStore.notionDatabaseName,
        settingsDataStore.notionLastSynced,
        _databases,
        _isLoading,
        _isSyncing,
        _error
    ) { values ->
        val token = values[0] as String?
        val workspace = values[1] as String?
        val dbId = values[2] as String?
        val dbName = values[3] as String?
        val lastSynced = values[4] as String?
        @Suppress("UNCHECKED_CAST")
        val databases = values[5] as List<NotionDatabase>
        val loading = values[6] as Boolean
        val syncing = values[7] as Boolean
        val error = values[8] as String?
        NotionUiState(
            isConnected = token != null,
            workspaceName = workspace,
            databaseId = dbId,
            databaseName = dbName,
            lastSynced = lastSynced,
            databases = databases,
            isLoading = loading,
            isSyncing = syncing,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotionUiState())

    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val credentials = Base64.encodeToString(
                    "$NOTION_CLIENT_ID:$NOTION_CLIENT_SECRET".toByteArray(),
                    Base64.NO_WRAP
                )
                val response = notionApiService.exchangeCodeForToken(
                    basicAuth = "Basic $credentials",
                    body = mapOf(
                        "grant_type" to "authorization_code",
                        "code" to code,
                        "redirect_uri" to NOTION_REDIRECT_URI
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()!!
                    settingsDataStore.setNotionConnection(
                        token = body.accessToken,
                        workspaceName = body.workspaceName ?: "My Workspace"
                    )
                    fetchDatabases()
                } else {
                    _error.value = "Connection failed (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchDatabases() {
        viewModelScope.launch {
            val token = settingsDataStore.notionAccessToken.first() ?: return@launch
            _isLoading.value = true
            try {
                val response = notionApiService.getDatabases(bearerToken = "Bearer $token")
                if (response.isSuccessful) {
                    _databases.value = response.body()?.results ?: emptyList()
                } else {
                    _error.value = "Không tải được danh sách database (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDatabase(db: NotionDatabase) {
        viewModelScope.launch {
            settingsDataStore.setNotionDatabase(id = db.id, name = db.name)
            _databases.value = emptyList()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val token = settingsDataStore.notionAccessToken.first() ?: return@launch
            val dbId = settingsDataStore.notionDatabaseId.first() ?: return@launch
            _isSyncing.value = true
            _error.value = null
            try {
                pushLocalChanges(token, dbId)

                val allPages = mutableListOf<NotionPage>()
                var cursor: String? = null
                do {
                    val body: Map<String, Any> =
                        if (cursor != null) mapOf("start_cursor" to cursor!!) else emptyMap()
                    val response = notionApiService.queryDatabase(
                        databaseId = dbId,
                        bearerToken = "Bearer $token",
                        body = body
                    )
                    if (!response.isSuccessful) {
                        _error.value = "Sync thất bại (${response.code()})"
                        return@launch
                    }
                    val result = response.body()!!
                    allPages.addAll(result.results)
                    cursor = if (result.hasMore) result.nextCursor else null
                } while (cursor != null)

                val tasks = allPages.mapNotNull { it.toTask(defaultListId = "default") }
                taskRepository.mergeRemoteTasks(tasks)
                settingsDataStore.setNotionLastSynced(
                    Instant.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun pushLocalChanges(token: String, dbId: String) {
        val bearer = "Bearer $token"
        val pending = taskRepository.getPendingTasks()
        for (task in pending) {
            try {
                when (task.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        val response = notionApiService.createPage(
                            bearerToken = bearer,
                            body = buildCreatePageBody(dbId, task)
                        )
                        if (response.isSuccessful) {
                            val notionId = response.body()!!.id.replace("-", "")
                            taskRepository.saveTask(task.copy(id = notionId, syncStatus = SyncStatus.SYNCED))
                            taskRepository.hardDeleteTask(task.id)
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        val pageId = task.id.toNotionPageId() ?: continue
                        val response = notionApiService.updatePage(
                            pageId = pageId,
                            bearerToken = bearer,
                            body = mapOf("properties" to buildPropertiesMap(task))
                        )
                        if (response.isSuccessful) {
                            taskRepository.saveTask(task.copy(syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        val pageId = task.id.toNotionPageId() ?: continue
                        val response = notionApiService.updatePage(
                            pageId = pageId,
                            bearerToken = bearer,
                            body = mapOf("archived" to true)
                        )
                        if (response.isSuccessful) {
                            taskRepository.hardDeleteTask(task.id)
                        }
                    }
                    SyncStatus.SYNCED -> Unit
                }
            } catch (_: Exception) {
                // best-effort: continue pushing remaining tasks
            }
        }
    }

    private fun String.toNotionPageId(): String? {
        if (length != 32 || !all { it.isLetterOrDigit() }) return null
        return "${substring(0, 8)}-${substring(8, 12)}-${substring(12, 16)}-${substring(16, 20)}-${substring(20)}"
    }

    private fun buildCreatePageBody(dbId: String, task: Task): Map<String, Any> =
        mapOf(
            "parent" to mapOf("database_id" to dbId),
            "properties" to buildPropertiesMap(task)
        )

    private fun buildPropertiesMap(task: Task): Map<String, Any> {
        val props = mutableMapOf<String, Any>(
            "Name" to mapOf("title" to listOf(mapOf("text" to mapOf("content" to task.title)))),
            "Done" to mapOf("checkbox" to (task.status == TaskStatus.DONE))
        )
        task.startDateTime?.let { instant ->
            val dateStr = instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
            props["Date"] = mapOf("date" to mapOf("start" to dateStr))
        }
        task.notes?.let { notes ->
            if (notes.isNotBlank()) {
                props["Notes"] = mapOf(
                    "rich_text" to listOf(mapOf("text" to mapOf("content" to notes)))
                )
            }
        }
        return props
    }

    fun disconnect() {
        viewModelScope.launch {
            settingsDataStore.clearNotionConnection()
            _databases.value = emptyList()
            _error.value = null
        }
    }

    fun clearError() { _error.value = null }

    companion object {
        val NOTION_CLIENT_ID get() = BuildConfig.NOTION_CLIENT_ID
        val NOTION_CLIENT_SECRET get() = BuildConfig.NOTION_CLIENT_SECRET
        const val NOTION_REDIRECT_URI = "monospace://notion-auth"
        fun buildOAuthUrl() =
            "https://api.notion.com/v1/oauth/authorize" +
                "?client_id=$NOTION_CLIENT_ID" +
                "&response_type=code" +
                "&owner=user" +
                "&redirect_uri=${android.net.Uri.encode(NOTION_REDIRECT_URI)}"
    }
}

private fun NotionPage.toTask(defaultListId: String): Task? {
    val title = properties.values.firstOrNull { it.type == "title" }
        ?.plainText()?.trim() ?: return null
    if (title.isBlank()) return null

    val isDone = properties.values
        .firstOrNull { it.type == "checkbox" }?.checkbox ?: false

    val startInstant = properties.values
        .firstOrNull { it.type == "date" }?.date?.start?.let { dateStr ->
            runCatching {
                LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
        }

    val notes = properties.values
        .firstOrNull { it.type == "rich_text" }?.plainText()?.trim()

    return Task(
        id = id.replace("-", ""),
        title = title,
        notes = notes,
        listId = defaultListId,
        status = if (isDone) TaskStatus.DONE else TaskStatus.NOT_DONE,
        startDateTime = startInstant,
        endDateTime = null
    )
}
