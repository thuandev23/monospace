package com.monospace.app.feature.settings

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.BuildConfig
import com.monospace.app.core.data.preferences.SettingsDataStore
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
                val response = notionApiService.queryDatabase(
                    databaseId = dbId,
                    bearerToken = "Bearer $token"
                )
                if (response.isSuccessful) {
                    val pages = response.body()?.results ?: emptyList()
                    val tasks = pages.mapNotNull { it.toTask(defaultListId = "default") }
                    taskRepository.mergeRemoteTasks(tasks)
                    settingsDataStore.setNotionLastSynced(
                        Instant.now().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    )
                } else {
                    _error.value = "Sync thất bại (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSyncing.value = false
            }
        }
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
