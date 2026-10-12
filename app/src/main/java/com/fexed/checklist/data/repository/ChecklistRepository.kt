package com.fexed.checklist.data.repository

import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.AppPreferences
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ChecklistRepository(
    private val checklistDao: ChecklistDao,
    private val appPreferences: AppPreferences
) {
    val allChecklists: Flow<List<ChecklistWithItems>> = checklistDao.getAllChecklistsWithItems()

    val userPreferences: Flow<UserPreferences> = appPreferences.userPreferencesFlow

    fun getChecklist(id: Long): Flow<ChecklistWithItems?> {
        return checklistDao.getChecklistWithItems(id)
    }

    suspend fun getChecklistById(id: Long): Checklist? {
        return checklistDao.getChecklistById(id)
    }

    suspend fun getLatestOpenedChecklist(): Checklist? {
        return checklistDao.getLatestOpenedChecklist()
    }

    suspend fun markChecklistOpened(id: Long) {
        val now = System.currentTimeMillis()
        checklistDao.updateLastOpened(id, now)
        appPreferences.setLastOpenedChecklistId(id)
    }

    suspend fun createChecklist(
        title: String,
        description: String = "",
        iconName: String = "list",
        colorTag: Long = 0L,
        reminderTime: Long? = null,
        geofenceLat: Double? = null,
        geofenceLng: Double? = null,
        geofenceRadius: Float = 200f,
        geofenceName: String? = null
    ): Long {
        val checklist = Checklist(
            title = title.trim(),
            description = description.trim(),
            iconName = iconName,
            colorTag = colorTag,
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis(),
            reminderTime = reminderTime,
            geofenceLat = geofenceLat,
            geofenceLng = geofenceLng,
            geofenceRadius = geofenceRadius,
            geofenceName = geofenceName
        )
        val id = checklistDao.insertChecklist(checklist)
        appPreferences.setLastOpenedChecklistId(id)
        return id
    }

    suspend fun updateChecklist(checklist: Checklist) {
        checklistDao.updateChecklist(checklist)
    }

    suspend fun deleteChecklist(id: Long) {
        checklistDao.deleteChecklistById(id)
    }

    suspend fun duplicateChecklist(checklistId: Long): Long? {
        val existing = checklistDao.getChecklistWithItems(checklistId).firstOrNull() ?: return null
        val newChecklist = existing.checklist.copy(
            id = 0,
            title = "${existing.checklist.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis()
        )
        val newId = checklistDao.insertChecklist(newChecklist)
        val newItems = existing.items.map { item ->
            item.copy(
                id = 0,
                checklistId = newId,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        }
        checklistDao.insertItems(newItems)
        return newId
    }

    // Checklist Items
    suspend fun addItem(
        checklistId: Long,
        title: String,
        description: String = "",
        tag: String = "",
        imageUri: String? = null
    ): Long {
        val item = ChecklistItem(
            checklistId = checklistId,
            title = title.trim(),
            description = description.trim(),
            tag = tag.trim(),
            imageUri = imageUri,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        return checklistDao.insertItem(item)
    }

    suspend fun updateItem(item: ChecklistItem) {
        checklistDao.updateItem(item)
    }

    suspend fun deleteItem(id: Long) {
        checklistDao.deleteItemById(id)
    }

    suspend fun toggleItemCompleted(id: Long, isCompleted: Boolean) {
        checklistDao.setItemCompleted(id, isCompleted)
    }

    suspend fun resetAllItems(checklistId: Long) {
        checklistDao.resetAllItems(checklistId)
    }

    suspend fun completeAllItems(checklistId: Long) {
        checklistDao.completeAllItems(checklistId)
    }

    // Settings
    suspend fun setStartPage(option: StartPageOption) {
        appPreferences.setStartPage(option)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appPreferences.setThemeMode(mode)
    }
}
