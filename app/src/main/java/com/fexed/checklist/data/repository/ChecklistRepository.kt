package com.fexed.checklist.data.repository

import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.AppPreferences
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.data.preferences.UserPreferences
import com.fexed.checklist.util.RecurrenceSchedule
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

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

    suspend fun getChecklistsWithGeofence(): List<Checklist> {
        return checklistDao.getChecklistsWithGeofence()
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
            updatedAt = System.currentTimeMillis(),
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
        checklistDao.updateChecklist(
            checklist.copy(updatedAt = maxOf(System.currentTimeMillis(), checklist.updatedAt + 1))
        )
    }

    suspend fun updateChecklistReminders(
        checklistId: Long,
        reminderTime: Long?,
        geofenceLat: Double?,
        geofenceLng: Double?,
        geofenceRadius: Float,
        geofenceName: String?,
        recurrence: ChecklistRecurrence?,
        recurrenceTimeMinutes: Int,
        recurrenceDayOfWeek: Int,
        recurrenceDayOfMonth: Int
    ) {
        val checklist = checklistDao.getChecklistById(checklistId) ?: return
        val now = System.currentTimeMillis()
        val normalizedTime = recurrenceTimeMinutes.coerceIn(0, 23 * 60 + 59)
        val normalizedDayOfWeek = recurrenceDayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        val normalizedDayOfMonth = recurrenceDayOfMonth.coerceIn(1, 31)
        val nextRecurrenceAt = recurrence?.let {
            RecurrenceSchedule.firstOccurrenceAfter(
                now = now,
                frequency = it,
                timeMinutes = normalizedTime,
                dayOfWeek = normalizedDayOfWeek,
                dayOfMonth = normalizedDayOfMonth
            )
        }

        checklistDao.updateChecklist(
            checklist.copy(
                reminderTime = reminderTime,
                geofenceLat = geofenceLat,
                geofenceLng = geofenceLng,
                geofenceRadius = geofenceRadius,
                geofenceName = geofenceName,
                recurrenceFrequency = recurrence?.name,
                recurrenceTimeMinutes = recurrence?.let { normalizedTime },
                recurrenceDayOfWeek = recurrence?.let { normalizedDayOfWeek },
                recurrenceDayOfMonth = recurrence?.let { normalizedDayOfMonth },
                recurrenceNextAt = nextRecurrenceAt,
                updatedAt = maxOf(now, checklist.updatedAt + 1)
            )
        )
    }

    suspend fun deleteChecklist(id: Long) {
        checklistDao.deleteChecklistWithTombstone(id, System.currentTimeMillis())
    }

    suspend fun duplicateChecklist(checklistId: Long): Long? {
        val existing = checklistDao.getChecklistWithItems(checklistId).firstOrNull() ?: return null
        val newChecklist = existing.checklist.copy(
            id = 0,
            title = "${existing.checklist.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastOpenedAt = System.currentTimeMillis(),
            syncId = UUID.randomUUID().toString(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = checklistDao.insertChecklist(newChecklist)
        val newItems = existing.items.map { item ->
            item.copy(
                id = 0,
                checklistId = newId,
                syncId = UUID.randomUUID().toString(),
                isCompleted = false,
                completedChecks = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                dueNotificationSentAt = null
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
        imageUri: String? = null,
        requiredChecks: Int = 1,
        dueAt: Long? = null
    ): Long {
        val item = ChecklistItem(
            checklistId = checklistId,
            title = title.trim(),
            description = description.trim(),
            tag = tag.trim(),
            imageUri = imageUri,
            isCompleted = false,
            requiredChecks = requiredChecks.coerceAtLeast(1),
            completedChecks = 0,
            createdAt = System.currentTimeMillis(),
            dueAt = dueAt
        )
        return checklistDao.insertItem(item)
    }

    suspend fun updateItem(item: ChecklistItem) {
        checklistDao.updateItem(
            item.copy(updatedAt = maxOf(System.currentTimeMillis(), item.updatedAt + 1))
        )
    }

    suspend fun deleteItem(id: Long) {
        checklistDao.deleteItemWithTombstone(id, System.currentTimeMillis())
    }

    suspend fun toggleItemCompleted(id: Long, isCompleted: Boolean) {
        checklistDao.setItemCompleted(id, isCompleted, System.currentTimeMillis())
    }

    suspend fun setItemCheckCount(id: Long, completedChecks: Int) {
        checklistDao.setItemCheckCount(id, completedChecks, System.currentTimeMillis())
    }

    suspend fun resetAllItems(checklistId: Long) {
        checklistDao.resetAllItems(checklistId, System.currentTimeMillis())
    }

    suspend fun completeAllItems(checklistId: Long) {
        checklistDao.completeAllItems(checklistId, System.currentTimeMillis())
    }

    // Settings
    suspend fun setStartPage(option: StartPageOption) {
        appPreferences.setStartPage(option)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appPreferences.setThemeMode(mode)
    }

    suspend fun setDriveFolderName(name: String) {
        appPreferences.setDriveFolderName(name)
    }
}
