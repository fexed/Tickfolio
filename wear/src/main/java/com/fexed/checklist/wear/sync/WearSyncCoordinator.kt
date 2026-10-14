package com.fexed.checklist.wear.sync

import android.content.Context
import android.util.Log
import com.fexed.checklist.wear.WearApplication
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.tasks.await

private const val TAG = "TickfolioWearSync"
private const val CHECKLIST_PATH_PREFIX = "/tickfolio/checklists/"
private const val PROGRESS_PATH_PREFIX = "/tickfolio/progress/"

object WearSyncCoordinator {
    private val gson = Gson()

    suspend fun refreshSnapshots(context: Context) {
        val store = (context.applicationContext as WearApplication).checklistStore
        val dataItems = Wearable.getDataClient(context).getDataItems().await()
        val snapshots = try {
            dataItems.mapNotNull(::parseSnapshot)
        } finally {
            dataItems.release()
        }
        snapshots.forEach { store.applySnapshot(it) }
        Log.i(TAG, "Loaded ${snapshots.size} existing checklist snapshots from Wear Data Layer")
    }

    suspend fun flushPending(context: Context) {
        val store = (context.applicationContext as WearApplication).checklistStore
        val dataClient = Wearable.getDataClient(context)
        store.pendingUpdates().forEach { update ->
            try {
                dataClient.putDataItem(
                    PutDataRequest.create(PROGRESS_PATH_PREFIX + update.requestId)
                        .setData(gson.toJson(update).toByteArray(Charsets.UTF_8))
                        .setUrgent()
                ).await()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to queue a checklist progress update for phone sync", error)
            }
        }
    }

    private fun parseSnapshot(dataItem: DataItem): WearChecklistSnapshot? {
        val path = dataItem.uri.path
        if (path?.startsWith(CHECKLIST_PATH_PREFIX) != true) return null
        val data = dataItem.data ?: return null
        val snapshot = try {
            gson.fromJson(String(data, Charsets.UTF_8), WearChecklistSnapshot::class.java)
        } catch (error: JsonParseException) {
            Log.e(TAG, "Unable to decode a checklist snapshot from Wear Data Layer", error)
            return null
        } ?: return null
        if (snapshot.syncId.isBlank() || snapshot.syncId != dataItem.uri.lastPathSegment) {
            Log.e(TAG, "Ignoring a checklist snapshot with an invalid sync ID")
            return null
        }
        return snapshot
    }
}
