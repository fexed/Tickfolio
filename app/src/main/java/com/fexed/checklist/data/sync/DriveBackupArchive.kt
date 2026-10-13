package com.fexed.checklist.data.sync

import android.content.Context
import com.google.gson.Gson
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DriveBackupArchive(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    fun write(
        backup: DriveBackup,
        photoFilesByName: Map<String, File>,
        destination: File
    ) {
        validateManifest(backup)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_NAME))
            zip.write(gson.toJson(backup).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            backup.items.mapNotNull { it.imageFileName }.distinct().forEach { imageName ->
                requireValidImageName(imageName)
                val image = photoFilesByName[imageName]
                    ?: throw IOException("Backup photo is unavailable: $imageName")
                if (!image.isFile) throw IOException("Backup photo is missing: $imageName")
                zip.putNextEntry(ZipEntry(imageName))
                BufferedInputStream(FileInputStream(image)).use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    fun read(archive: File): ExtractedDriveBackup {
        val directory = File(context.cacheDir, "drive-sync-${UUID.randomUUID()}")
        if (!directory.mkdirs()) throw IOException("Unable to create temporary backup directory")
        var backup: DriveBackup? = null
        val photos = mutableMapOf<String, File>()
        var totalBytes = 0L

        try {
            ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) throw IOException("Unexpected directory in Drive backup")
                    when {
                        entry.name == MANIFEST_NAME -> {
                            if (backup != null) throw IOException("Duplicate Drive backup manifest")
                            val bytes = zip.readBounded(MAX_MANIFEST_BYTES)
                            backup = gson.fromJson(String(bytes, Charsets.UTF_8), DriveBackup::class.java)
                                ?: throw IOException("Drive backup manifest is empty")
                            totalBytes += bytes.size
                        }
                        isValidImageName(entry.name) -> {
                            if (entry.name in photos) throw IOException("Duplicate photo in Drive backup")
                            val file = File(directory, entry.name.substringAfter('/'))
                            FileOutputStream(file).use { output ->
                                val copied = zip.copyBoundedTo(output, MAX_IMAGE_BYTES)
                                totalBytes += copied
                            }
                            photos[entry.name] = file
                        }
                        else -> throw IOException("Unexpected file in Drive backup: ${entry.name}")
                    }
                    if (totalBytes > MAX_BACKUP_BYTES) throw IOException("Drive backup is too large")
                    zip.closeEntry()
                }
            }

            val loadedBackup = backup ?: throw IOException("Drive backup manifest is missing")
            validateManifest(loadedBackup)
            loadedBackup.items.mapNotNull { it.imageFileName }.distinct().forEach { imageName ->
                requireValidImageName(imageName)
                if (imageName !in photos) throw IOException("Drive backup photo is missing: $imageName")
            }
            return ExtractedDriveBackup(loadedBackup, photos, directory)
        } catch (exception: Exception) {
            directory.deleteRecursively()
            throw exception
        }
    }

    private fun validateManifest(backup: DriveBackup) {
        if (backup.formatVersion != DriveBackup.FORMAT_VERSION) {
            throw IOException("Unsupported Drive backup version: ${backup.formatVersion}")
        }
        if (backup.checklists.size > MAX_RECORDS || backup.items.size > MAX_RECORDS) {
            throw IOException("Drive backup contains too many records")
        }
        if (backup.checklists.any { it.syncId.isBlank() } || backup.items.any {
                it.syncId.isBlank() || it.checklistSyncId.isBlank()
            }
        ) {
            throw IOException("Drive backup contains invalid record identifiers")
        }
        backup.items.mapNotNull { it.imageFileName }.forEach(::requireValidImageName)
        if (backup.tombstones.any {
                it.recordType !in setOf("checklist", "item") || it.syncId.isBlank()
            }
        ) {
            throw IOException("Drive backup contains invalid deletion records")
        }
    }

    private fun requireValidImageName(name: String) {
        if (!isValidImageName(name)) throw IOException("Invalid photo path in Drive backup")
    }

    private fun isValidImageName(name: String): Boolean =
        name.matches(Regex("""images/[A-Za-z0-9-]+\.jpg"""))

    private fun java.util.zip.ZipInputStream.readBounded(maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        copyBoundedTo(output, maxBytes)
        return output.toByteArray()
    }

    private fun java.util.zip.ZipInputStream.copyBoundedTo(
        output: java.io.OutputStream,
        maxBytes: Long
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            copied += count
            if (copied > maxBytes) throw IOException("Drive backup entry is too large")
            output.write(buffer, 0, count)
        }
        return copied
    }

    companion object {
        private const val MANIFEST_NAME = "manifest.json"
        private const val BUFFER_SIZE = 8 * 1024
        private const val MAX_MANIFEST_BYTES = 32L * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 50L * 1024 * 1024
        private const val MAX_BACKUP_BYTES = 512L * 1024 * 1024
        private const val MAX_RECORDS = 100_000
    }
}

data class ExtractedDriveBackup(
    val backup: DriveBackup,
    val photoFilesByName: Map<String, File>,
    val temporaryDirectory: File
) {
    fun clean() {
        temporaryDirectory.deleteRecursively()
    }
}
