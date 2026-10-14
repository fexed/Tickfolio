package com.fexed.checklist.data.wear

import android.net.Uri
import android.util.Log
import com.fexed.checklist.ChecklistApplication
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val TAG = "TickfolioWearListener"
private const val PROGRESS_PATH_PREFIX = "/tickfolio/progress/"

class WearDataListenerService : com.google.android.gms.wearable.WearableListenerService() {
    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        Log.e(TAG, "Unable to process a Wear progress update", error)
    }
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val updateMutex = Mutex()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val updates = mutableListOf<Pair<Uri, ByteArray>>()
        dataEvents.forEach { event ->
            val uri = event.dataItem.uri
            if (
                event.type == DataEvent.TYPE_CHANGED &&
                uri.path?.startsWith(PROGRESS_PATH_PREFIX) == true
            ) {
                event.dataItem.data?.let { updates += uri to it.copyOf() }
            }
        }
        Log.i(TAG, "Received ${updates.size} Wear progress updates")

        updates.forEach { (uri, bytes) ->
            serviceScope.launch { processProgressUpdate(uri, bytes) }
        }
    }

    override fun onPeerConnected(node: Node) {
        Log.i(TAG, "Wear peer connected")
        serviceScope.launch {
            val dataItems = Wearable.getDataClient(this@WearDataListenerService)
                .getDataItems()
                .await()
            val updates = try {
                dataItems.mapNotNull { item ->
                    val path = item.uri.path
                    val bytes = item.data
                    if (path?.startsWith(PROGRESS_PATH_PREFIX) == true && bytes != null) {
                        item.uri to bytes.copyOf()
                    } else {
                        null
                    }
                }
            } finally {
                dataItems.release()
            }
            updates.forEach { (uri, bytes) -> processProgressUpdate(uri, bytes) }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun processProgressUpdate(uri: Uri, bytes: ByteArray) {
        updateMutex.withLock {
            val update = WearSync.parseProgressUpdate(bytes)
            if (update == null || update.requestId != uri.lastPathSegment) {
                Log.e(TAG, "Discarding malformed Wear progress update")
            } else {
                val application = application as ChecklistApplication
                application.repository.applyWearProgress(
                    syncId = update.syncId,
                    completedChecks = update.completedChecks,
                    updatedAt = update.updatedAt
                )
                try {
                    WearSync.publishChecklists(
                        application,
                        application.database.checklistDao()
                    )
                } catch (error: Exception) {
                    Log.e(TAG, "Progress was applied, but the Wear snapshot could not be refreshed", error)
                }
            }
            Wearable.getDataClient(this@WearDataListenerService)
                .deleteDataItems(uri)
                .await()
        }
    }
}
