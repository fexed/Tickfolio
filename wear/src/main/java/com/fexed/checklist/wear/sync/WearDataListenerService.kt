package com.fexed.checklist.wear.sync

import android.net.Uri
import android.util.Log
import com.fexed.checklist.wear.WearApplication
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "TickfolioWearListener"
private const val CHECKLIST_PATH_PREFIX = "/tickfolio/checklists/"
private const val PROGRESS_PATH_PREFIX = "/tickfolio/progress/"

class WearDataListenerService : WearableListenerService() {
    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        Log.e(TAG, "Wear data event processing failed", error)
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val events = dataEvents.map { event ->
            Triple(event.type, event.dataItem.uri, event.dataItem.data?.copyOf())
        }
        Log.i(TAG, "Received ${events.size} Data Layer events")
        events.forEach { (type, uri, data) ->
            when {
                type == DataEvent.TYPE_CHANGED &&
                    uri.path?.startsWith(CHECKLIST_PATH_PREFIX) == true &&
                    data != null -> serviceScope.launch {
                        applySnapshot(uri, data)
                    }

                type == DataEvent.TYPE_DELETED &&
                    uri.path?.startsWith(CHECKLIST_PATH_PREFIX) == true -> serviceScope.launch {
                        removeChecklist(uri)
                    }

                type == DataEvent.TYPE_DELETED &&
                    uri.path?.startsWith(PROGRESS_PATH_PREFIX) == true -> serviceScope.launch {
                        acknowledgeProgress(uri)
                    }
            }
        }
    }

    override fun onPeerConnected(node: Node) {
        serviceScope.launch {
            WearSyncCoordinator.flushPending(this@WearDataListenerService)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun applySnapshot(uri: Uri, data: ByteArray) {
        val snapshot = try {
            gson.fromJson(String(data, Charsets.UTF_8), WearChecklistSnapshot::class.java)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to decode a checklist snapshot from the phone", error)
            null
        }
        if (snapshot == null || snapshot.syncId.isBlank()) {
            Log.e(TAG, "Ignoring an invalid checklist snapshot from the phone")
            return
        }
        (application as WearApplication).checklistStore.applySnapshot(snapshot)
        Log.i(TAG, "Saved checklist snapshot with ${snapshot.items.size} items")
    }

    private suspend fun removeChecklist(uri: Uri) {
        val syncId = uri.lastPathSegment ?: return
        (application as WearApplication).checklistStore.removeChecklist(syncId)
    }

    private suspend fun acknowledgeProgress(uri: Uri) {
        val requestId = uri.lastPathSegment ?: return
        (application as WearApplication).checklistStore.acknowledge(requestId)
    }
}
