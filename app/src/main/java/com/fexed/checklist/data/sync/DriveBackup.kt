package com.fexed.checklist.data.sync

import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.SyncTombstone

data class DriveBackup(
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val checklists: List<ChecklistRecord> = emptyList(),
    val items: List<ItemRecord> = emptyList(),
    val tombstones: List<SyncTombstone> = emptyList()
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

data class ChecklistRecord(
    val syncId: String,
    val updatedAt: Long,
    val title: String,
    val description: String,
    val iconName: String,
    val colorTag: Long,
    val createdAt: Long,
    val reminderTime: Long?,
    val geofenceLat: Double?,
    val geofenceLng: Double?,
    val geofenceRadius: Float,
    val geofenceName: String?,
    val recurrenceFrequency: String?,
    val recurrenceTimeMinutes: Int?,
    val recurrenceDayOfWeek: Int?,
    val recurrenceDayOfMonth: Int?,
    val recurrenceNextAt: Long?
) {
    companion object {
        fun from(checklist: Checklist) = ChecklistRecord(
            syncId = checklist.syncId,
            updatedAt = checklist.updatedAt,
            title = checklist.title,
            description = checklist.description,
            iconName = checklist.iconName,
            colorTag = checklist.colorTag,
            createdAt = checklist.createdAt,
            reminderTime = checklist.reminderTime,
            geofenceLat = checklist.geofenceLat,
            geofenceLng = checklist.geofenceLng,
            geofenceRadius = checklist.geofenceRadius,
            geofenceName = checklist.geofenceName,
            recurrenceFrequency = checklist.recurrenceFrequency,
            recurrenceTimeMinutes = checklist.recurrenceTimeMinutes,
            recurrenceDayOfWeek = checklist.recurrenceDayOfWeek,
            recurrenceDayOfMonth = checklist.recurrenceDayOfMonth,
            recurrenceNextAt = checklist.recurrenceNextAt
        )
    }
}

data class ItemRecord(
    val syncId: String,
    val checklistSyncId: String,
    val updatedAt: Long,
    val title: String,
    val description: String,
    val tag: String,
    val imageFileName: String?,
    val isCompleted: Boolean,
    val requiredChecks: Int,
    val completedChecks: Int,
    val position: Int,
    val createdAt: Long,
    val dueAt: Long? = null
) {
    companion object {
        fun from(item: ChecklistItem, checklistSyncId: String) = ItemRecord(
            syncId = item.syncId,
            checklistSyncId = checklistSyncId,
            updatedAt = item.updatedAt,
            title = item.title,
            description = item.description,
            tag = item.tag,
            imageFileName = item.imageUri?.let { "images/${item.syncId}.jpg" },
            isCompleted = item.isCompleted,
            requiredChecks = item.requiredChecks,
            completedChecks = item.completedChecks,
            position = item.position,
            createdAt = item.createdAt,
            dueAt = item.dueAt
        )
    }
}
