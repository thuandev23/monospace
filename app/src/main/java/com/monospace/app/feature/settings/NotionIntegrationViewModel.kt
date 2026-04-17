package com.monospace.app.feature.settings

import android.util.Base64
import androidx.lifecycle.ViewModel
import com.monospace.app.BuildConfig
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.data.preferences.SettingsDataStore
import com.monospace.app.core.network.api.NotionApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotionUiState(
    val isConnected: Boolean = false,
    val workspaceName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotionIntegrationViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val notionApiService: NotionApiService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotionUiState> = combine(
        settingsDataStore.notionAccessToken,
        settingsDataStore.notionWorkspaceName,
        _isLoading,
        _error
    ) { token, workspace, loading, error ->
        NotionUiState(
            isConnected = token != null,
            workspaceName = workspace,
            isLoading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotionUiState())

    // Called after the user authorizes via OAuth and we receive the auth code
    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val clientId = NOTION_CLIENT_ID
                val clientSecret = NOTION_CLIENT_SECRET
                val credentials = Base64.encodeToString(
                    "$clientId:$clientSecret".toByteArray(),
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

    fun disconnect() {
        viewModelScope.launch {
            settingsDataStore.clearNotionConnection()
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
