package com.fexed.checklist.data.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveClient(
    private val accessToken: String,
    private val gson: Gson = Gson()
) {
    fun findOrCreateFolder(folderName: String): DriveFile {
        val name = folderName.trim()
        require(name.isNotEmpty() && '/' !in name && '\\' !in name) {
            "Drive folder name must be a single non-empty name"
        }
        val escapedName = escapeQueryValue(name)
        val query = "name = '$escapedName' and mimeType = '$FOLDER_MIME_TYPE' and 'root' in parents and trashed = false"
        val existing = listFiles(query).firstOrNull()
        return existing ?: createFolder(name)
    }

    fun findBackup(folderId: String): DriveFile? {
        val query = "name = '$BACKUP_FILE_NAME' and '$folderId' in parents and trashed = false"
        return listFiles(query).firstOrNull()
    }

    fun downloadBackup(fileId: String, destination: File) {
        val connection = openConnection("$FILES_URL/${encodePathSegment(fileId)}?alt=media", "GET")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw apiException(connection, status)
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(destination.outputStream()).use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun uploadBackup(folderId: String, existingFileId: String?, source: File): DriveFile {
        if (!source.isFile) throw IOException("Drive backup archive is missing")
        val startUrl = if (existingFileId == null) {
            "$UPLOAD_URL?uploadType=resumable&fields=id,name,mimeType"
        } else {
            "$UPLOAD_URL/${encodePathSegment(existingFileId)}?uploadType=resumable&fields=id,name,mimeType"
        }
        val metadata = JsonObject().apply {
            addProperty("name", BACKUP_FILE_NAME)
            addProperty("mimeType", ZIP_MIME_TYPE)
            if (existingFileId == null) {
                add("parents", gson.toJsonTree(listOf(folderId)))
            }
        }
        val uploadUrl = startResumableUpload(startUrl, existingFileId == null, metadata, source.length())
        return transferResumableUpload(uploadUrl, source)
    }

    private fun createFolder(name: String): DriveFile {
        val metadata = JsonObject().apply {
            addProperty("name", name)
            addProperty("mimeType", FOLDER_MIME_TYPE)
            add("parents", gson.toJsonTree(listOf("root")))
        }
        val connection = openConnection("$FILES_URL?fields=id,name,mimeType", "POST")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.outputStream.use { it.write(gson.toJson(metadata).toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw apiException(connection, status)
            return parseDriveFile(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun listFiles(query: String): List<DriveFile> {
        val url = "$FILES_URL?q=${encodeQuery(query)}&pageSize=100&fields=files(id,name,mimeType)"
        val connection = openConnection(url, "GET")
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw apiException(connection, status)
            val response = JsonParser.parseString(
                connection.inputStream.bufferedReader().use { it.readText() }
            ).asJsonObject
            val files = response.getAsJsonArray("files") ?: return emptyList()
            return files.map { parseDriveFile(it.asJsonObject) }
        } finally {
            connection.disconnect()
        }
    }

    private fun startResumableUpload(
        url: String,
        creating: Boolean,
        metadata: JsonObject,
        contentLength: Long
    ): String {
        val connection = openConnection(url, if (creating) "POST" else "PATCH")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("X-Upload-Content-Type", ZIP_MIME_TYPE)
            connection.setRequestProperty("X-Upload-Content-Length", contentLength.toString())
            connection.outputStream.use { it.write(gson.toJson(metadata).toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) throw apiException(connection, status)
            return connection.getHeaderField("Location")
                ?: throw IOException("Google Drive did not return an upload session")
        } finally {
            connection.disconnect()
        }
    }

    private fun transferResumableUpload(uploadUrl: String, source: File): DriveFile {
        val connection = openConnection(uploadUrl, "PUT")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", ZIP_MIME_TYPE)
            connection.setFixedLengthStreamingMode(source.length())
            BufferedInputStream(FileInputStream(source)).use { input ->
                BufferedOutputStream(connection.outputStream).use { output -> input.copyTo(output) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw apiException(connection, status)
            return parseDriveFile(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }

    private fun apiException(connection: HttpURLConnection, status: Int): IOException {
        val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
            ?.take(MAX_ERROR_LENGTH)
            .orEmpty()
        return DriveApiException(
            statusCode = status,
            message = "Google Drive request failed (HTTP $status)${if (detail.isBlank()) "" else ": $detail"}"
        )
    }

    private fun parseDriveFile(json: String): DriveFile =
        parseDriveFile(JsonParser.parseString(json).asJsonObject)

    private fun parseDriveFile(json: JsonObject): DriveFile {
        val id = json.get("id")?.asString ?: throw IOException("Google Drive response omitted file ID")
        return DriveFile(
            id = id,
            name = json.get("name")?.asString.orEmpty(),
            mimeType = json.get("mimeType")?.asString.orEmpty()
        )
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun encodePathSegment(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun escapeQueryValue(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")

    companion object {
        const val BACKUP_FILE_NAME = "tickfolio_backup.zip"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val ZIP_MIME_TYPE = "application/zip"
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val MAX_ERROR_LENGTH = 2_000
    }
}

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String
)

class DriveApiException(
    val statusCode: Int,
    message: String
) : IOException(message)
