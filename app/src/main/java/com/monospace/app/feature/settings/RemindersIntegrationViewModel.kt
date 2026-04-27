package com.monospace.app.feature.settings

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.domain.model.ListIds
import com.monospace.app.core.domain.model.SyncStatus
import com.monospace.app.core.domain.model.Task
import com.monospace.app.core.domain.model.TaskStatus
import com.monospace.app.core.domain.repository.TaskRepository
import com.monospace.app.core.network.api.GoogleTasksApiService
import com.monospace.app.core.network.dto.GoogleTask
import com.monospace.app.core.network.dto.GoogleTaskBody
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.resume

data class RemindersUiState(
    val connectedAccount: String? = null,
    val lastSynced: String? = null,
    val isSyncing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RemindersIntegrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val googleTasksApiService: GoogleTasksApiService,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val accountManager = AccountManager.get(context)

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _consentIntent = MutableSharedFlow<Intent>()
    val consentIntent: SharedFlow<Intent> = _consentIntent.asSharedFlow()

    val uiState: StateFlow<RemindersUiState> = combine(
        settingsDataStore.googleTasksAccount,
        settingsDataStore.googleTasksLastSynced,
        _isSyncing,
        _error
    ) { account, lastSynced, syncing, error ->
        RemindersUiState(
            connectedAccount = account,
            lastSynced = lastSynced,
            isSyncing = syncing,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RemindersUiState())

    fun connectAccount(accountName: String) {
        viewModelScope.launch {
            _error.value = null
            val account = accountManager.getAccountsByType("com.google")
                .firstOrNull { it.name == accountName } ?: run {
                _error.value = "Account not found"
                return@launch
            }
            val (token, intent) = getAuthToken(account)
            when {
                intent != null -> _consentIntent.emit(intent)
                token != null -> connectWithToken(accountName, token)
                else -> _error.value = "Could not get auth token"
            }
        }
    }

    private suspend fun getAuthToken(account: android.accounts.Account): Pair<String?, Intent?> =
        suspendCancellableCoroutine { cont ->
            accountManager.getAuthToken(account, SCOPE, null, false, { future ->
                try {
                    val bundle = future.result
                    @Suppress("DEPRECATION")
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable(AccountManager.KEY_INTENT, Intent::class.java)
                    } else {
                        bundle.getParcelable(AccountManager.KEY_INTENT)
                    }
                    if (intent != null) {
                        cont.resume(Pair(null, intent))
                    } else {
                        cont.resume(Pair(bundle.getString(AccountManager.KEY_AUTHTOKEN), null))
                    }
                } catch (e: Exception) {
                    cont.resume(Pair(null, null))
                }
            }, null)
        }

    private fun connectWithToken(accountName: String, token: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val response = googleTasksApiService.getTaskLists("Bearer $token")
                if (response.isSuccessful) {
                    val firstList = response.body()?.items?.firstOrNull() ?: run {
                        _error.value = "No task lists found in Google Tasks"
                        return@launch
                    }
                    settingsDataStore.setGoogleTasksConnection(accountName, firstList.id)
                    performSync(token, firstList.id)
                } else {
                    _error.value = "Connection failed (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val accountName = settingsDataStore.googleTasksAccount.first() ?: return@launch
            val listId = settingsDataStore.googleTasksListId.first() ?: return@launch
            val account = accountManager.getAccountsByType("com.google")
                .firstOrNull { it.name == accountName } ?: return@launch
            val (token, intent) = getAuthToken(account)
            if (intent != null) { _consentIntent.emit(intent); return@launch }
            if (token == null) { _error.value = "Could not get auth token"; return@launch }
            _isSyncing.value = true
            _error.value = null
            try {
                performSync(token, listId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun performSync(token: String, gtListId: String) {
        pushLocalChanges(token, gtListId)
        pullTasks(token, gtListId)
        settingsDataStore.setGoogleTasksLastSynced(
            Instant.now().atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        )
    }

    private suspend fun pushLocalChanges(token: String, gtListId: String) {
        val bearer = "Bearer $token"
        val pending = taskRepository.getPendingTasks()
        for (task in pending) {
            try {
                when (task.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        if (task.externalSource != null) continue
                        val response = googleTasksApiService.createTask(gtListId, bearer, task.toGoogleTaskBody())
                        if (response.isSuccessful) {
                            val gtId = response.body()?.id ?: continue
                            taskRepository.saveTask(task.copy(id = gtId, syncStatus = SyncStatus.SYNCED, externalSource = "google_tasks"))
                            taskRepository.hardDeleteTask(task.id)
                        }
                    }
                    SyncStatus.PENDING_UPDATE -> {
                        if (task.externalSource != "google_tasks") continue
                        val response = googleTasksApiService.updateTask(gtListId, task.id, bearer, task.toGoogleTaskBody())
                        if (response.isSuccessful) taskRepository.saveTask(task.copy(syncStatus = SyncStatus.SYNCED))
                    }
                    SyncStatus.PENDING_DELETE -> {
                        if (task.externalSource != "google_tasks") continue
                        val response = googleTasksApiService.deleteTask(gtListId, task.id, bearer)
                        if (response.isSuccessful) taskRepository.hardDeleteTask(task.id)
                    }
                    SyncStatus.SYNCED -> Unit
                }
            } catch (_: Exception) {
                // best-effort: continue with remaining tasks
            }
        }
    }

    private suspend fun pullTasks(token: String, gtListId: String) {
        val bearer = "Bearer $token"
        val allGTasks = mutableListOf<GoogleTask>()
        var pageToken: String? = null
        do {
            val response = googleTasksApiService.getTasks(
                gtListId, bearer, pageToken = pageToken, showCompleted = true
            )
            if (!response.isSuccessful) break
            val body = response.body()!!
            allGTasks.addAll(body.items?.filter { it.deleted != true } ?: emptyList())
            pageToken = body.nextPageToken
        } while (pageToken != null)

        val tasks = allGTasks.mapNotNull { it.toTask(defaultListId = ListIds.DEFAULT) }
        taskRepository.mergeRemoteTasks(tasks)
    }

    fun disconnect() {
        viewModelScope.launch { settingsDataStore.clearGoogleTasksConnection() }
    }

    fun clearError() { _error.value = null }

    companion object {
        const val SCOPE = "oauth2:https://www.googleapis.com/auth/tasks"
    }
}

private fun Task.toGoogleTaskBody(): GoogleTaskBody {
    val dueRfc3339 = startDateTime?.atZone(ZoneId.systemDefault())
        ?.toLocalDate()
        ?.let { "${it}T00:00:00.000Z" }
    return GoogleTaskBody(
        title = title,
        notes = notes,
        status = if (status == TaskStatus.DONE) "completed" else "needsAction",
        due = dueRfc3339
    )
}

private fun GoogleTask.toTask(defaultListId: String): Task? {
    val taskId = id ?: return null
    val taskTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val startInstant = due?.let { dueStr ->
        runCatching { Instant.parse(dueStr) }.getOrNull()
            ?: runCatching {
                LocalDate.parse(dueStr.substringBefore("T"))
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
    }
    return Task(
        id = taskId,
        title = taskTitle,
        notes = notes?.trim()?.takeIf { it.isNotBlank() },
        listId = defaultListId,
        status = if (status == "completed") TaskStatus.DONE else TaskStatus.NOT_DONE,
        startDateTime = startInstant,
        endDateTime = null,
        externalSource = "google_tasks"
    )
}
