package com.fexed.checklist.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.fexed.checklist.data.local.ChecklistDatabase
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.SyncTombstone
import com.fexed.checklist.data.preferences.AppPreferences
import com.fexed.checklist.util.ImageStorageHelper
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first

class DriveSyncEngine(
    private val context: Context,
    private val database: ChecklistDatabase,
    private val gson: Gson = Gson()
) {
    suspend fun sync(accessToken: String, folderName: String) {
        val client = GoogleDriveClient(accessToken, gson)
        val folder = client.findOrCreateFolder(folderName)
        val remoteFile = client.findBackup(folder.id)
        val localState = readLocalState()
        var downloadedArchive: File? = null
        var extractedRemote: ExtractedDriveBackup? = null
        try {
            if (remoteFile != null) {
                downloadedArchive = File.createTempFile("tickfolio_drive_download_", ".zip", context.cacheDir)
                client.downloadBackup(remoteFile.id, downloadedArchive)
            }
            val remote = downloadedArchive?.let { DriveBackupArchive(context, gson).read(it) }
            extractedRemote = remote

            val merged = if (remote == null) {
                localState.backup
            } else {
                DriveBackupMerger.merge(localState.backup, remote.backup)
            }
            val imagePaths = resolveImages(merged, localState, remote)
            applyBackup(merged, imagePaths)

            val finalLocalState = readLocalState()
            if (remote != null && samePayload(finalLocalState.backup, remote.backup)) return

            val outgoing = finalLocalState.backup.copy(
                exportedAt = maxOf(remote?.backup?.exportedAt ?: 0L, System.currentTimeMillis())
            )
            val photoFiles = finalLocalState.photoFilesByName
            val archive = File.createTempFile("tickfolio_drive_upload_", ".zip", context.cacheDir)
            try {
                DriveBackupArchive(context, gson).write(outgoing, photoFiles, archive)
                client.uploadBackup(folder.id, remoteFile?.id, archive)
            } finally {
                archive.delete()
            }
        } finally {
            downloadedArchive?.delete()
            extractedRemote?.clean()
        }
    }

    private suspend fun readLocalState(): LocalState {
        val dao = database.checklistDao()
        val checklists = dao.getChecklistsForSync()
        val checklistSyncIds = checklists.associate { it.id to it.syncId }
        val imageFiles = mutableMapOf<String, File>()
        val items = dao.getItemsForSync().map { item ->
            val checklistSyncId = checklistSyncIds[item.checklistId]
                ?: throw IOException("Checklist item ${item.id} has no parent checklist")
            val record = ItemRecord.from(item, checklistSyncId)
            item.imageUri?.let { path ->
                val image = File(path)
                if (image.isFile) imageFiles[record.imageFileName!!] = image
            }
            record
        }
        return LocalState(
            backup = DriveBackup(
                exportedAt = 0,
                checklists = checklists.map(ChecklistRecord::from),
                items = items,
                tombstones = dao.getSyncTombstones(),
                tagColors = AppPreferences(context).userPreferencesFlow.first().tagColors
            ),
            photoFilesByName = imageFiles
        )
    }

    private fun resolveImages(
        merged: DriveBackup,
        local: LocalState,
        remote: ExtractedDriveBackup?
    ): Map<String, String> {
        val localItems = local.backup.items.associateBy { it.syncId }
        val remoteItems = remote?.backup?.items?.associateBy { it.syncId }.orEmpty()
        val result = mutableMapOf<String, String>()

        merged.items.forEach { item ->
            val imageName = item.imageFileName ?: return@forEach
            val localRecord = localItems[item.syncId]
            val remoteRecord = remoteItems[item.syncId]
            val remoteImage = remote?.photoFilesByName?.get(imageName)
            val localImage = local.photoFilesByName[imageName]
            val preferredImage = if (DriveBackupMerger.remoteWins(localRecord, remoteRecord)) {
                remoteImage
            } else {
                localImage
            }
            val source = preferredImage?.takeIf(File::isFile)
                ?: remoteImage?.takeIf(File::isFile)
                ?: localImage?.takeIf(File::isFile)
                ?: throw IOException("Unable to locate backup photo for item ${item.syncId}")

            val canonicalPath = source.canonicalPath
            val internalDirectory = context.filesDir.canonicalPath + File.separator
            result[item.syncId] = if (canonicalPath.startsWith(internalDirectory)) {
                source.absolutePath
            } else {
                ImageStorageHelper.copyImageToInternalStorage(context, source)
            }
        }
        return result
    }

    private suspend fun applyBackup(backup: DriveBackup, imagePaths: Map<String, String>) {
        val dao = database.checklistDao()
        val appPreferences = AppPreferences(context)
        val currentTagColors = appPreferences.userPreferencesFlow.first().tagColors
        var mergedTagColors = currentTagColors
        database.withTransaction {
            val currentChecklists = dao.getChecklistsForSync()
            val currentChecklistSyncIds = currentChecklists.associate { it.id to it.syncId }
            val currentItems = dao.getItemsForSync().map { item ->
                val checklistSyncId = currentChecklistSyncIds[item.checklistId]
                    ?: throw IOException("Checklist item ${item.id} has no parent checklist")
                ItemRecord.from(item, checklistSyncId)
            }
            val currentBackup = DriveBackup(
                checklists = currentChecklists.map(ChecklistRecord::from),
                items = currentItems,
                tombstones = dao.getSyncTombstones(),
                tagColors = currentTagColors
            )
            val latestBackup = DriveBackupMerger.merge(currentBackup, backup)
            mergedTagColors = latestBackup.tagColors.orEmpty()

            latestBackup.checklists.forEach { record ->
                val existing = dao.getChecklistBySyncId(record.syncId)
                val checklist = Checklist(
                    id = existing?.id ?: 0L,
                    title = record.title,
                    description = record.description,
                    iconName = record.iconName,
                    colorTag = record.colorTag,
                    createdAt = record.createdAt,
                    lastOpenedAt = existing?.lastOpenedAt ?: record.createdAt,
                    reminderTime = record.reminderTime,
                    geofenceLat = record.geofenceLat,
                    geofenceLng = record.geofenceLng,
                    geofenceRadius = record.geofenceRadius,
                    geofenceName = record.geofenceName,
                    recurrenceFrequency = record.recurrenceFrequency,
                    recurrenceTimeMinutes = record.recurrenceTimeMinutes,
                    recurrenceDayOfWeek = record.recurrenceDayOfWeek,
                    recurrenceDayOfMonth = record.recurrenceDayOfMonth,
                    recurrenceNextAt = record.recurrenceNextAt,
                    syncId = record.syncId,
                    updatedAt = record.updatedAt
                )
                if (existing == null) dao.insertChecklist(checklist)
                else if (existing != checklist) dao.updateChecklist(checklist)
            }

            val checklistIds = dao.getChecklistsForSync().associate { it.syncId to it.id }
            latestBackup.items.forEach { record ->
                val checklistId = checklistIds[record.checklistSyncId]
                    ?: throw IOException("Backup item ${record.syncId} has no checklist")
                val existing = dao.getItemBySyncId(record.syncId)
                val requiredChecks = record.requiredChecks.coerceAtLeast(1)
                val completedChecks = record.completedChecks.coerceIn(0, requiredChecks)
                val imageUri = if (record.imageFileName == null) {
                    null
                } else {
                    imagePaths[record.syncId]
                        ?: existing?.imageUri?.takeIf { File(it).isFile }
                        ?: throw IOException("Unable to restore photo for item ${record.syncId}")
                }
                val item = ChecklistItem(
                    id = existing?.id ?: 0L,
                    checklistId = checklistId,
                    title = record.title,
                    description = record.description,
                    tags = record.tags.orEmpty().ifEmpty {
                        listOfNotNull(record.tag.takeIf(String::isNotBlank))
                    },
                    imageUri = imageUri,
                    isCompleted = completedChecks >= requiredChecks,
                    requiredChecks = requiredChecks,
                    completedChecks = completedChecks,
                    position = record.position,
                    createdAt = record.createdAt,
                    syncId = record.syncId,
                    updatedAt = record.updatedAt,
                    dueAt = record.dueAt,
                    dueNotificationSentAt = existing?.dueNotificationSentAt
                        ?.takeIf { it == record.dueAt }
                )
                if (existing == null) dao.insertItem(item)
                else if (existing != item) dao.updateItem(item)
            }

            val localTombstones = dao.getSyncTombstones()
            if (localTombstones.toSet() != latestBackup.tombstones.toSet()) {
                dao.clearSyncTombstones()
                if (latestBackup.tombstones.isNotEmpty()) {
                    dao.insertTombstones(latestBackup.tombstones)
                }
            }
            latestBackup.tombstones.forEach { tombstone ->
                when (tombstone.recordType) {
                    SyncTombstone.CHECKLIST -> dao.deleteChecklistBySyncId(tombstone.syncId)
                    SyncTombstone.ITEM -> dao.deleteItemBySyncId(tombstone.syncId)
                }
            }
        }
        appPreferences.setTagColors(mergedTagColors)
    }

    private fun samePayload(left: DriveBackup, right: DriveBackup): Boolean {
        fun normalize(backup: DriveBackup) = backup.copy(
            exportedAt = 0,
            checklists = backup.checklists.sortedBy { it.syncId },
            items = backup.items.sortedBy { it.syncId },
            tombstones = backup.tombstones.sortedWith(compareBy({ it.recordType }, { it.syncId })),
            tagColors = backup.tagColors.orEmpty().toSortedMap()
        )
        return gson.toJson(normalize(left)) == gson.toJson(normalize(right))
    }

    private data class LocalState(
        val backup: DriveBackup,
        val photoFilesByName: Map<String, File>
    )
}
