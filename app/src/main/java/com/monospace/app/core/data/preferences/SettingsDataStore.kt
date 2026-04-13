package com.monospace.app.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    // --- Sidebar hidden items ---

    val sidebarHiddenItems: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIDEBAR_HIDDEN_ITEMS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun setSidebarHiddenItems(hidden: Set<String>) {
        context.dataStore.edit { it[KEY_SIDEBAR_HIDDEN_ITEMS] = hidden.joinToString(",") }
    }
}
