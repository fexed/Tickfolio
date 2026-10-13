package com.fexed.checklist.data.sync

import com.fexed.checklist.data.model.SyncTombstone

object DriveBackupMerger {

    fun remoteWins(local: ItemRecord?, remote: ItemRecord?): Boolean =
        when {
            local == null -> remote != null
            remote == null -> false
            else -> compare(remote, local, ItemRecord::updatedAt) > 0
        }

    fun merge(local: DriveBackup, remote: DriveBackup): DriveBackup {
        require(local.formatVersion == DriveBackup.FORMAT_VERSION)
        require(remote.formatVersion == DriveBackup.FORMAT_VERSION)

        val checklists = mergeRecords(local.checklists, remote.checklists, ChecklistRecord::syncId, ChecklistRecord::updatedAt)
        val items = mergeRecords(local.items, remote.items, ItemRecord::syncId, ItemRecord::updatedAt)
        val tombstones = mergeRecords(
            local.tombstones,
            remote.tombstones,
            { "${it.recordType}:${it.syncId}" },
            SyncTombstone::deletedAt
        )

        val winningChecklistIds = checklists.filterNotDeleted(tombstones, ChecklistRecord::syncId, ChecklistRecord::updatedAt)
            .mapTo(mutableSetOf()) { it.syncId }
        val winningItems = items.filterNotDeleted(tombstones, ItemRecord::syncId, ItemRecord::updatedAt)
            .filter { it.checklistSyncId in winningChecklistIds }
        val winningItemIds = winningItems.mapTo(mutableSetOf()) { it.syncId }
        val winningChecklists = checklists.filter { it.syncId in winningChecklistIds }
        val retainedTombstones = tombstones.filter { tombstone ->
            !when (tombstone.recordType) {
                SyncTombstone.CHECKLIST -> tombstone.syncId in winningChecklistIds
                SyncTombstone.ITEM -> tombstone.syncId in winningItemIds
                else -> false
            }
        }

        return DriveBackup(
            exportedAt = maxOf(local.exportedAt, remote.exportedAt),
            checklists = winningChecklists,
            items = winningItems,
            tombstones = retainedTombstones
        )
    }

    private fun <T> mergeRecords(
        local: List<T>,
        remote: List<T>,
        key: (T) -> String,
        modifiedAt: (T) -> Long
    ): List<T> {
        val result = local.associateByTo(linkedMapOf(), key)
        remote.forEach { candidate ->
            val existing = result[key(candidate)]
            if (existing == null || compare(candidate, existing, modifiedAt) > 0) {
                result[key(candidate)] = candidate
            }
        }
        return result.values.toList()
    }

    private fun <T> List<T>.filterNotDeleted(
        tombstones: List<SyncTombstone>,
        key: (T) -> String,
        modifiedAt: (T) -> Long
    ): List<T> = filter { record ->
        val type = when (record) {
            is ChecklistRecord -> SyncTombstone.CHECKLIST
            is ItemRecord -> SyncTombstone.ITEM
            else -> return@filter true
        }
        val tombstone = tombstones.firstOrNull { it.recordType == type && it.syncId == key(record) }
        tombstone == null || modifiedAt(record) > tombstone.deletedAt
    }

    private fun <T> compare(left: T, right: T, modifiedAt: (T) -> Long): Int {
        val timestampOrder = modifiedAt(left).compareTo(modifiedAt(right))
        if (timestampOrder != 0) return timestampOrder
        return left.toString().compareTo(right.toString())
    }
}
