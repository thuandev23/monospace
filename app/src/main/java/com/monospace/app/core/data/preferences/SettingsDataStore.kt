package com.monospace.app.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.monospace.app.core.domain.model.GroupOption
import com.monospace.app.core.domain.model.SortOption
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
    companion object {
        private val KEY_DEFAULT_LIST_ID = stringPreferencesKey("default_list_id")
        private val KEY_SIDEBAR_ITEM_ORDER = stringPreferencesKey("sidebar_item_order")
        private val KEY_SIDEBAR_HIDDEN_ITEMS = stringPreferencesKey("sidebar_hidden_items")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

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
}
