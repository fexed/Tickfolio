package com.fexed.checklist.data.wear

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fexed.checklist.data.local.ChecklistDao
import com.fexed.checklist.data.model.ChecklistWithItems
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

private const val TAG = "TickfolioWearSync"
private const val CHECKLIST_PATH_PREFIX = "/tickfolio/checklists/"

data class WearChecklistSnapshot(
    val syncId: String,
    val title: String,
    val updatedAt: Long,
    val items: List<WearItemSnapshot>
)

data class WearItemSnapshot(
    val syncId: String,
    val title: String,
    val tag: String,
    val requiredChecks: Int,
    val completedChecks: Int,
    val updatedAt: Long,
    val position: Int
)

data class WearProgressUpdate(
    val requestId: String,
    val syncId: String,
    val completedChecks: Int,
    val updatedAt: Long
)

object WearSync {
    private val gson = Gson()
    private val publishMutex = Mutex()

    suspend fun publishChecklists(context: Context, dao: ChecklistDao) = withContext(Dispatchers.IO) {
        publishMutex.withLock {
            val dataClient = Wearable.getDataClient(context)
            val snapshots = dao.getAllChecklistsForWidget().map(::toSnapshot)
            val expectedPaths = snapshots.mapTo(mutableSetOf()) { CHECKLIST_PATH_PREFIX + it.syncId }
            val connectedNodes = Wearable.getNodeClient(context).connectedNodes.await()
            Log.i(
                TAG,
                "Publishing ${snapshots.size} checklist snapshots; connected Wear nodes: ${connectedNodes.size}"
            )

            snapshots.forEach { snapshot ->
                val request = PutDataRequest.create(CHECKLIST_PATH_PREFIX + snapshot.syncId)
                    .setData(gson.toJson(snapshot).toByteArray(Charsets.UTF_8))
                    .setUrgent()
                dataClient.putDataItem(request).await()
            }

            val existing = dataClient.getDataItems().await()
            try {
                val staleUris = existing.mapNotNull { item ->
                    item.uri.takeIf {
                        it.path?.startsWith(CHECKLIST_PATH_PREFIX) == true &&
                            it.path !in expectedPaths
                    }
                }
                staleUris.forEach { dataClient.deleteDataItems(it).await() }
            } finally {
                existing.release()
            }
            Log.i(TAG, "Wear checklist snapshot publication completed")
        }
    }

    fun parseProgressUpdate(data: ByteArray): WearProgressUpdate? {
        val update = try {
            gson.fromJson(String(data, Charsets.UTF_8), WearProgressUpdate::class.java)
        } catch (error: JsonParseException) {
            Log.e(TAG, "Unable to decode a Wear progress update", error)
            return null
        } ?: return null
        if (
            update.requestId.isBlank() ||
            update.syncId.isBlank() ||
            update.completedChecks < 0 ||
            update.updatedAt <= 0
        ) {
            Log.e(TAG, "Ignoring invalid Wear progress update")
            return null
        }
        return update
    }

    private fun toSnapshot(checklist: ChecklistWithItems) = WearChecklistSnapshot(
        syncId = checklist.checklist.syncId,
        title = checklist.checklist.title,
        updatedAt = checklist.checklist.updatedAt,
        items = checklist.items
            .sortedWith(compareBy({ it.position }, { it.createdAt }))
            .map { item ->
                WearItemSnapshot(
                    syncId = item.syncId,
                    title = item.title,
                    tag = item.tag,
                    requiredChecks = item.requiredChecks,
                    completedChecks = item.completedChecks,
                    updatedAt = item.updatedAt,
                    position = item.position
                )
            }
    )
}
