package com.fexed.checklist.data.sync

import com.fexed.checklist.data.model.SyncTombstone
import com.fexed.checklist.data.preferences.TagColorRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupMergerTest {

    @Test
    fun mergeKeepsNewestChecklistAndItemRecords() {
        val local = DriveBackup(
            exportedAt = 10,
            checklists = listOf(checklist("list", updatedAt = 3, title = "Local title")),
            items = listOf(item("item", updatedAt = 7, title = "Local item", dueAt = 300))
        )
        val remote = DriveBackup(
            exportedAt = 20,
            checklists = listOf(checklist("list", updatedAt = 5, title = "Remote title")),
            items = listOf(item("item", updatedAt = 6, title = "Remote item", dueAt = 200))
        )

        val merged = DriveBackupMerger.merge(local, remote)

        assertEquals(20, merged.exportedAt)
        assertEquals("Remote title", merged.checklists.single().title)
        assertEquals("Local item", merged.items.single().title)
        assertEquals(300L, merged.items.single().dueAt)
    }

    @Test
    fun deletionTombstonePreventsOlderRecordFromReturning() {
        val local = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 2)),
            tombstones = listOf(SyncTombstone(SyncTombstone.CHECKLIST, "list", 4))
        )
        val remote = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 3))
        )

        val merged = DriveBackupMerger.merge(local, remote)

        assertTrue(merged.checklists.isEmpty())
        assertTrue(merged.items.isEmpty())
        assertEquals(4, merged.tombstones.single().deletedAt)
    }

    @Test
    fun newerRecordWinsOverOldTombstone() {
        val local = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 8)),
            tombstones = listOf(SyncTombstone(SyncTombstone.CHECKLIST, "list", 4))
        )

        val merged = DriveBackupMerger.merge(local, DriveBackup())

        assertEquals("list", merged.checklists.single().syncId)
        assertTrue(merged.tombstones.isEmpty())
    }

    @Test
    fun itemsAreDroppedWhenTheirChecklistIsDeleted() {
        val local = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 2)),
            items = listOf(item("item", checklistSyncId = "list", updatedAt = 5))
        )
        val remote = DriveBackup(
            tombstones = listOf(SyncTombstone(SyncTombstone.CHECKLIST, "list", 3))
        )

        val merged = DriveBackupMerger.merge(local, remote)

        assertTrue(merged.checklists.isEmpty())
        assertTrue(merged.items.isEmpty())
    }

    @Test
    fun mergeKeepsMultipleTagsAndNewestUniqueTagColors() {
        val local = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 1)),
            items = listOf(item("item", updatedAt = 5, tags = listOf("Travel", "Essentials"))),
            tagColors = mapOf("travel" to TagColorRecord(0xFF123456.toInt(), 10))
        )
        val remote = DriveBackup(
            checklists = listOf(checklist("list", updatedAt = 1)),
            items = listOf(item("item", updatedAt = 4, tags = listOf("Old"))),
            tagColors = mapOf("Travel" to TagColorRecord(0xFF654321.toInt(), 20))
        )

        val merged = DriveBackupMerger.merge(local, remote)

        assertEquals(listOf("Travel", "Essentials"), merged.items.single().tags)
        assertEquals(0xFF654321.toInt(), merged.tagColors?.get("travel")?.colorArgb)
    }

    private fun checklist(
        syncId: String,
        updatedAt: Long,
        title: String = "Checklist"
    ) = ChecklistRecord(
        syncId = syncId,
        updatedAt = updatedAt,
        title = title,
        description = "",
        iconName = "list",
        colorTag = 0,
        createdAt = 1,
        reminderTime = null,
        geofenceLat = null,
        geofenceLng = null,
        geofenceRadius = 200f,
        geofenceName = null,
        recurrenceFrequency = null,
        recurrenceTimeMinutes = null,
        recurrenceDayOfWeek = null,
        recurrenceDayOfMonth = null,
        recurrenceNextAt = null
    )

    private fun item(
        syncId: String,
        checklistSyncId: String = "list",
        updatedAt: Long,
        title: String = "Item",
        dueAt: Long? = null,
        tags: List<String>? = null
    ) = ItemRecord(
        syncId = syncId,
        checklistSyncId = checklistSyncId,
        updatedAt = updatedAt,
        title = title,
        description = "",
        tag = "",
        tags = tags,
        imageFileName = null,
        isCompleted = false,
        requiredChecks = 1,
        completedChecks = 0,
        position = 0,
        createdAt = 1,
        dueAt = dueAt
    )
}
