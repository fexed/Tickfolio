package com.fexed.checklist.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "checklist_settings")

enum class StartPageOption(val label: String, val description: String) {
    ALL_CHECKLISTS("All Checklists", "Always show the full list of checklists on launch"),
    LATEST_OPENED("Latest Opened", "Automatically open the most recently viewed checklist")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Paper Light"),
    DARK("Parchment Dark")
}

data class UserPreferences(
    val startPage: StartPageOption,
    val themeMode: ThemeMode,
    val lastOpenedChecklistId: Long?
)

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_START_PAGE = stringPreferencesKey("start_page")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LAST_OPENED_ID = longPreferencesKey("last_opened_id")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val startPageName = prefs[KEY_START_PAGE] ?: StartPageOption.ALL_CHECKLISTS.name
        val startPage = try {
            StartPageOption.valueOf(startPageName)
        } catch (_: Exception) {
            StartPageOption.ALL_CHECKLISTS
        }

        val themeModeName = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val lastOpenedId = prefs[KEY_LAST_OPENED_ID]

        UserPreferences(
            startPage = startPage,
            themeMode = themeMode,
            lastOpenedChecklistId = lastOpenedId
        )
    }

    suspend fun setStartPage(option: StartPageOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_START_PAGE] = option.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setLastOpenedChecklistId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_OPENED_ID] = id
        }
    }
}
