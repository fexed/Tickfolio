package com.fexed.checklist.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
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

enum class ChecklistSortOption {
    TITLE,
    DUE_DATE,
    LAST_EDITED,
    CREATED,
    LAST_OPENED
}

data class UserPreferences(
    val startPage: StartPageOption,
    val themeMode: ThemeMode,
    val lastOpenedChecklistId: Long?,
    val driveBackupEnabled: Boolean = false,
    val driveConsentAsked: Boolean = false,
    val driveFolderName: String = "Tickfolio",
    val lastDriveSyncAt: Long? = null,
    val driveSyncError: String? = null,
    val checklistSortOption: ChecklistSortOption = ChecklistSortOption.LAST_OPENED,
    val checklistSortAscending: Boolean = false
)

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_START_PAGE = stringPreferencesKey("start_page")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LAST_OPENED_ID = longPreferencesKey("last_opened_id")
        val KEY_DRIVE_BACKUP_ENABLED = booleanPreferencesKey("drive_backup_enabled")
        val KEY_DRIVE_CONSENT_ASKED = booleanPreferencesKey("drive_consent_asked")
        val KEY_DRIVE_FOLDER_NAME = stringPreferencesKey("drive_folder_name")
        val KEY_LAST_DRIVE_SYNC_AT = longPreferencesKey("last_drive_sync_at")
        val KEY_DRIVE_SYNC_ERROR = stringPreferencesKey("drive_sync_error")
        val KEY_CHECKLIST_SORT_OPTION = stringPreferencesKey("checklist_sort_option")
        val KEY_CHECKLIST_SORT_ASCENDING = booleanPreferencesKey("checklist_sort_ascending")
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
        val checklistSortOption = try {
            ChecklistSortOption.valueOf(
                prefs[KEY_CHECKLIST_SORT_OPTION] ?: ChecklistSortOption.LAST_OPENED.name
            )
        } catch (_: IllegalArgumentException) {
            ChecklistSortOption.LAST_OPENED
        }

        UserPreferences(
            startPage = startPage,
            themeMode = themeMode,
            lastOpenedChecklistId = lastOpenedId,
            driveBackupEnabled = prefs[KEY_DRIVE_BACKUP_ENABLED] ?: false,
            driveConsentAsked = prefs[KEY_DRIVE_CONSENT_ASKED] ?: false,
            driveFolderName = prefs[KEY_DRIVE_FOLDER_NAME] ?: "Tickfolio",
            lastDriveSyncAt = prefs[KEY_LAST_DRIVE_SYNC_AT],
            driveSyncError = prefs[KEY_DRIVE_SYNC_ERROR],
            checklistSortOption = checklistSortOption,
            checklistSortAscending = prefs[KEY_CHECKLIST_SORT_ASCENDING] ?: false
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

    suspend fun setChecklistSortOption(option: ChecklistSortOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHECKLIST_SORT_OPTION] = option.name
        }
    }

    suspend fun setChecklistSortAscending(ascending: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHECKLIST_SORT_ASCENDING] = ascending
        }
    }

    suspend fun setLastOpenedChecklistId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_OPENED_ID] = id
        }
    }

    suspend fun setDriveBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRIVE_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setDriveConsentAsked() {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRIVE_CONSENT_ASKED] = true
        }
    }

    suspend fun setDriveFolderName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRIVE_FOLDER_NAME] = name.trim()
        }
    }

    suspend fun setLastDriveSyncAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_DRIVE_SYNC_AT] = timestamp
            prefs.remove(KEY_DRIVE_SYNC_ERROR)
        }
    }

    suspend fun setDriveSyncError(error: String?) {
        context.dataStore.edit { prefs ->
            if (error == null) prefs.remove(KEY_DRIVE_SYNC_ERROR)
            else prefs[KEY_DRIVE_SYNC_ERROR] = error
        }
    }
}
