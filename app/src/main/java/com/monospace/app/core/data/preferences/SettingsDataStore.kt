package com.monospace.app.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import java.time.LocalDate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.monospace.app.core.domain.model.AddTaskPosition
import com.monospace.app.core.domain.model.AppShortcut
import com.monospace.app.core.domain.model.AppTheme
import com.monospace.app.core.domain.model.GeneralSettings
import com.monospace.app.core.domain.model.GroupOption
import com.monospace.app.core.domain.model.SecondStatus
import com.monospace.app.core.domain.model.SortOption
import com.monospace.app.core.domain.model.TaskAlignment
import com.monospace.app.core.domain.model.TaskDisplaySettings
import com.monospace.app.core.domain.model.ViewSettings
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "monospace_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val KEY_DEFAULT_LIST_ID = stringPreferencesKey("default_list_id")
        private val KEY_LAUNCHER_SHORTCUTS = stringPreferencesKey("launcher_shortcuts")
        private val KEY_SIDEBAR_ITEM_ORDER = stringPreferencesKey("sidebar_item_order")
        private val KEY_SIDEBAR_HIDDEN_ITEMS = stringPreferencesKey("sidebar_hidden_items")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // TaskDisplaySettings keys
        private val KEY_TASK_SHOW_STATUS_CIRCLE = booleanPreferencesKey("task_show_status_circle")
        private val KEY_TASK_LOWERCASE = booleanPreferencesKey("task_lowercase")
        private val KEY_TASK_FONT_SIZE = intPreferencesKey("task_font_size")
        private val KEY_TASK_ALIGNMENT = stringPreferencesKey("task_alignment")

        // GeneralSettings keys
        private val KEY_GENERAL_THEME = stringPreferencesKey("general_theme")
        private val KEY_GENERAL_ADD_TASK_POSITION = stringPreferencesKey("general_add_task_position")
        private val KEY_GENERAL_SECOND_STATUS = stringPreferencesKey("general_second_status")
        private val KEY_GENERAL_REVERSE_SCROLL = booleanPreferencesKey("general_reverse_scroll")

        // ViewSettings keys
        private val KEY_VIEW_SHOW_OVERDUE = booleanPreferencesKey("view_show_overdue")
        private val KEY_VIEW_SHOW_IN_PROGRESS = booleanPreferencesKey("view_show_in_progress")
        private val KEY_VIEW_SHOW_COMPLETED = booleanPreferencesKey("view_show_completed")
        private val KEY_VIEW_SHOW_TIME = booleanPreferencesKey("view_show_time")
        private val KEY_VIEW_SHOW_FOLDER = booleanPreferencesKey("view_show_folder")
        private val KEY_VIEW_SORT_BY = stringPreferencesKey("view_sort_by")
        private val KEY_VIEW_GROUP_BY = stringPreferencesKey("view_group_by")
    }

    // --- Default List ---

    val defaultListId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_LIST_ID] ?: "default"
    }

    suspend fun setDefaultListId(listId: String) {
        context.dataStore.edit { it[KEY_DEFAULT_LIST_ID] = listId }
    }

    // --- Sidebar item order (lưu dạng CSV: "today,upcoming,search,settings") ---

    val sidebarItemOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIDEBAR_ITEM_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setSidebarItemOrder(order: List<String>) {
        context.dataStore.edit { it[KEY_SIDEBAR_ITEM_ORDER] = order.joinToString(",") }
    }

    // --- Onboarding ---

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = true }
    }

    // --- Sidebar hidden items ---

    val sidebarHiddenItems: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIDEBAR_HIDDEN_ITEMS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun setSidebarHiddenItems(hidden: Set<String>) {
        context.dataStore.edit { it[KEY_SIDEBAR_HIDDEN_ITEMS] = hidden.joinToString(",") }
    }

    // --- ViewSettings ---

    val viewSettings: Flow<ViewSettings> = context.dataStore.data.map { prefs ->
        ViewSettings(
            showOverdue = prefs[KEY_VIEW_SHOW_OVERDUE] ?: true,
            showInProgress = prefs[KEY_VIEW_SHOW_IN_PROGRESS] ?: true,
            showCompleted = prefs[KEY_VIEW_SHOW_COMPLETED] ?: true,
            showTime = prefs[KEY_VIEW_SHOW_TIME] ?: true,
            showFolder = prefs[KEY_VIEW_SHOW_FOLDER] ?: true,
            sortBy = SortOption.entries.firstOrNull { it.name == prefs[KEY_VIEW_SORT_BY] } ?: SortOption.MANUAL,
            groupBy = GroupOption.entries.firstOrNull { it.name == prefs[KEY_VIEW_GROUP_BY] } ?: GroupOption.NONE
        )
    }

    suspend fun setViewSettings(settings: ViewSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIEW_SHOW_OVERDUE] = settings.showOverdue
            prefs[KEY_VIEW_SHOW_IN_PROGRESS] = settings.showInProgress
            prefs[KEY_VIEW_SHOW_COMPLETED] = settings.showCompleted
            prefs[KEY_VIEW_SHOW_TIME] = settings.showTime
            prefs[KEY_VIEW_SHOW_FOLDER] = settings.showFolder
            prefs[KEY_VIEW_SORT_BY] = settings.sortBy.name
            prefs[KEY_VIEW_GROUP_BY] = settings.groupBy.name
        }
    }

    // --- TaskDisplaySettings ---

    val taskDisplaySettings: Flow<TaskDisplaySettings> = context.dataStore.data.map { prefs ->
        TaskDisplaySettings(
            showStatusCircle = prefs[KEY_TASK_SHOW_STATUS_CIRCLE] ?: true,
            lowercase = prefs[KEY_TASK_LOWERCASE] ?: false,
            fontSize = prefs[KEY_TASK_FONT_SIZE] ?: 17,
            alignment = TaskAlignment.entries.firstOrNull { it.name == prefs[KEY_TASK_ALIGNMENT] } ?: TaskAlignment.LEADING
        )
    }

    suspend fun setTaskDisplaySettings(settings: TaskDisplaySettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TASK_SHOW_STATUS_CIRCLE] = settings.showStatusCircle
            prefs[KEY_TASK_LOWERCASE] = settings.lowercase
            prefs[KEY_TASK_FONT_SIZE] = settings.fontSize
            prefs[KEY_TASK_ALIGNMENT] = settings.alignment.name
        }
    }

    // --- Notion Integration ---

    private val KEY_NOTION_ACCESS_TOKEN = stringPreferencesKey("notion_access_token")
    private val KEY_NOTION_WORKSPACE = stringPreferencesKey("notion_workspace_name")

    val notionAccessToken: Flow<String?> = context.dataStore.data.map { it[KEY_NOTION_ACCESS_TOKEN] }
    val notionWorkspaceName: Flow<String?> = context.dataStore.data.map { it[KEY_NOTION_WORKSPACE] }

    suspend fun setNotionConnection(token: String, workspaceName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTION_ACCESS_TOKEN] = token
            prefs[KEY_NOTION_WORKSPACE] = workspaceName
        }
    }

    suspend fun clearNotionConnection() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_NOTION_ACCESS_TOKEN)
            prefs.remove(KEY_NOTION_WORKSPACE)
        }
    }

    // --- TabBarSettings ---

    val tabBarShowUpcoming: Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("tab_show_upcoming")] ?: true }
    val tabBarShowSearch: Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("tab_show_search")] ?: true }

    suspend fun setTabBarShowUpcoming(show: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey("tab_show_upcoming")] = show }
    }

    suspend fun setTabBarShowSearch(show: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey("tab_show_search")] = show }
    }

    // --- GeneralSettings ---

    val generalSettings: Flow<GeneralSettings> = context.dataStore.data.map { prefs ->
        GeneralSettings(
            theme = AppTheme.entries.firstOrNull { it.name == prefs[KEY_GENERAL_THEME] } ?: AppTheme.MINIMALIST,
            addTaskPosition = AddTaskPosition.entries.firstOrNull { it.name == prefs[KEY_GENERAL_ADD_TASK_POSITION] } ?: AddTaskPosition.BOTTOM,
            secondStatus = SecondStatus.entries.firstOrNull { it.name == prefs[KEY_GENERAL_SECOND_STATUS] } ?: SecondStatus.CANCELLED,
            reverseScrollDirection = prefs[KEY_GENERAL_REVERSE_SCROLL] ?: false
        )
    }

    suspend fun setGeneralSettings(settings: GeneralSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GENERAL_THEME] = settings.theme.name
            prefs[KEY_GENERAL_ADD_TASK_POSITION] = settings.addTaskPosition.name
            prefs[KEY_GENERAL_SECOND_STATUS] = settings.secondStatus.name
            prefs[KEY_GENERAL_REVERSE_SCROLL] = settings.reverseScrollDirection
        }
    }

    // --- Launcher Shortcuts ---

    val launcherShortcuts: Flow<List<AppShortcut>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_LAUNCHER_SHORTCUTS] ?: return@map emptyList()
        runCatching {
            val type = object : TypeToken<List<AppShortcut>>() {}.type
            gson.fromJson<List<AppShortcut>>(json, type)
        }.getOrDefault(emptyList())
    }

    suspend fun setLauncherShortcuts(shortcuts: List<AppShortcut>) {
        context.dataStore.edit { it[KEY_LAUNCHER_SHORTCUTS] = gson.toJson(shortcuts) }
    }
}
