package com.fexed.checklist.wear.sync

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

private const val TAG = "TickfolioWearStore"

data class WearChecklistSnapshot(
    val syncId: String = "",
    val title: String = "",
    val updatedAt: Long = 0L,
    val items: List<WearItemSnapshot> = emptyList()
)

data class WearItemSnapshot(
    val syncId: String = "",
    val title: String = "",
    val tag: String = "",
    val requiredChecks: Int = 1,
    val completedChecks: Int = 0,
    val updatedAt: Long = 0L,
    val position: Int = 0
)

data class WearProgressUpdate(
    val requestId: String = "",
    val syncId: String = "",
    val completedChecks: Int = 0,
    val updatedAt: Long = 0L
)

private data class WearCache(
    val checklists: List<WearChecklistSnapshot> = emptyList(),
    val pendingUpdates: List<WearProgressUpdate> = emptyList()
)

class WearChecklistStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "wear_checklist_cache.json"))
    private val mutex = Mutex()
    private val gson = Gson()
    private val _checklists = MutableStateFlow<List<WearChecklistSnapshot>>(emptyList())
    val checklists = _checklists.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _checklists.value = readCache().checklists
        }
    }

    suspend fun pendingUpdates(): List<WearProgressUpdate> = withContext(Dispatchers.IO) {
        mutex.withLock { readCache().pendingUpdates }
    }

    suspend fun applySnapshot(snapshot: WearChecklistSnapshot) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cache = readCache()
            val pendingByItem = cache.pendingUpdates
                .groupBy { it.syncId }
                .mapValues { (_, updates) -> updates.maxBy { it.updatedAt } }
            val previousById = cache.checklists
                .firstOrNull { it.syncId == snapshot.syncId }
                ?.items
                ?.associateBy { it.syncId }
                .orEmpty()
            val previousChecklist = cache.checklists.firstOrNull { it.syncId == snapshot.syncId }
            val checklistIsNewer = previousChecklist?.updatedAt?.let { it > snapshot.updatedAt } == true
            val merged = snapshot.copy(
                title = previousChecklist
                    ?.takeIf { checklistIsNewer }
                    ?.title ?: snapshot.title,
                updatedAt = maxOf(
                    snapshot.updatedAt,
                    previousChecklist?.updatedAt ?: 0L
                ),
                items = snapshot.items.map { incoming ->
                    val item = previousById[incoming.syncId]
                        ?.takeIf { it.updatedAt > incoming.updatedAt }
                        ?: incoming
                    pendingByItem[item.syncId]
                        ?.takeIf { it.updatedAt > item.updatedAt }
                        ?.let { update ->
                            item.copy(
                                completedChecks = update.completedChecks.coerceIn(
                                    0,
                                    item.requiredChecks.coerceAtLeast(1)
                                ),
                                updatedAt = update.updatedAt
                            )
                        } ?: item
                }
            )
            val updated = cache.copy(
                checklists = (cache.checklists.filterNot { it.syncId == snapshot.syncId } + merged)
                    .sortedBy { it.title.lowercase() }
            )
            writeCache(updated)
            _checklists.value = updated.checklists
        }
    }

    suspend fun removeChecklist(syncId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cache = readCache()
            val updated = cache.copy(checklists = cache.checklists.filterNot { it.syncId == syncId })
            writeCache(updated)
            _checklists.value = updated.checklists
        }
    }

    suspend fun incrementProgress(itemSyncId: String): WearProgressUpdate? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cache = readCache()
            val checklist = cache.checklists.firstOrNull { checklist ->
                checklist.items.any { it.syncId == itemSyncId }
            }
                ?: return@withLock null
            val item = checklist.items.firstOrNull { it.syncId == itemSyncId }
                ?: return@withLock null
            val completedChecks = if (item.completedChecks >= item.requiredChecks) {
                0
            } else {
                item.completedChecks + 1
            }
            val update = WearProgressUpdate(
                requestId = UUID.randomUUID().toString(),
                syncId = itemSyncId,
                completedChecks = completedChecks,
                updatedAt = maxOf(System.currentTimeMillis(), item.updatedAt + 1)
            )
            val updatedChecklist = checklist.copy(
                items = checklist.items.map {
                    if (it.syncId == itemSyncId) {
                        it.copy(completedChecks = completedChecks, updatedAt = update.updatedAt)
                    } else {
                        it
                    }
                }
            )
            val updated = cache.copy(
                checklists = cache.checklists.map {
                    if (it.syncId == checklist.syncId) updatedChecklist else it
                },
                pendingUpdates = cache.pendingUpdates + update
            )
            writeCache(updated)
            _checklists.value = updated.checklists
            update
        }
    }

    suspend fun acknowledge(requestId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cache = readCache()
            val updated = cache.copy(
                pendingUpdates = cache.pendingUpdates.filterNot { it.requestId == requestId }
            )
            writeCache(updated)
        }
    }

    private fun readCache(): WearCache {
        if (!file.baseFile.exists()) return WearCache()
        return try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, WearCache::class.java) ?: WearCache()
            }
        } catch (error: FileNotFoundException) {
            WearCache()
        } catch (error: Exception) {
            Log.e(TAG, "Unable to read the Wear checklist cache", error)
            throw error
        }
    }

    private fun writeCache(cache: WearCache) {
        val output = file.startWrite()
        try {
            output.write(gson.toJson(cache).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
