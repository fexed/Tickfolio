package com.fexed.checklist.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fexed.checklist.data.model.ChecklistItem
import java.util.concurrent.TimeUnit

object ItemDueDateScheduler {
    private const val WORK_TAG = "item_due_date"
    private const val WORK_PREFIX = "item_due_date_"
    private const val ITEM_TAG_PREFIX = "item_due_date_sync_"
    private const val DUE_TAG_PREFIX = "item_due_date_at_"

    fun reconcile(context: Context, items: List<ChecklistItem>) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val workInfos: List<WorkInfo> = workManager.getWorkInfosByTag(WORK_TAG).get()
        val desiredDueDates = items
            .filter { !it.isCompleted && it.dueAt != null && it.dueNotificationSentAt != it.dueAt }
            .associate { it.syncId to it.dueAt!! }
        val activeWork = workInfos
            .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            .mapNotNull { info -> itemSyncId(info)?.let { it to info } }
            .toMap()

        workInfos.mapNotNull(::itemSyncId)
            .distinct()
            .filter { syncId ->
                val info = activeWork[syncId]
                info == null || desiredDueDates[syncId] != info?.let(::dueAt)
            }
            .forEach { syncId -> workManager.cancelUniqueWork(workName(syncId)) }

        desiredDueDates.forEach { (syncId, dueAt) ->
            val existing = activeWork[syncId]
            if (existing?.let(::dueAt) == dueAt) return@forEach

            workManager.enqueueUniqueWork(
                workName(syncId),
                ExistingWorkPolicy.REPLACE,
                buildWorkRequest(syncId, dueAt)
            )
        }
    }

    fun schedule(context: Context, syncId: String, dueAt: Long) {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName(syncId), ExistingWorkPolicy.REPLACE, buildWorkRequest(syncId, dueAt))
    }

    private fun buildWorkRequest(syncId: String, dueAt: Long) =
        OneTimeWorkRequestBuilder<ItemDueDateWorker>()
            .setInputData(
                Data.Builder()
                    .putString(ItemDueDateWorker.KEY_ITEM_SYNC_ID, syncId)
                    .putLong(ItemDueDateWorker.KEY_DUE_AT, dueAt)
                    .build()
            )
            .setInitialDelay((dueAt - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .addTag(itemTag(syncId))
            .addTag(dueTag(dueAt))
            .build()

    private fun itemSyncId(workInfo: WorkInfo): String? =
        workInfo.tags.firstOrNull { it.startsWith(ITEM_TAG_PREFIX) }
            ?.removePrefix(ITEM_TAG_PREFIX)

    private fun dueAt(workInfo: WorkInfo): Long? =
        workInfo.tags.firstOrNull { it.startsWith(DUE_TAG_PREFIX) }
            ?.removePrefix(DUE_TAG_PREFIX)
            ?.toLongOrNull()

    private fun itemTag(syncId: String): String = "$ITEM_TAG_PREFIX$syncId"

    private fun dueTag(dueAt: Long): String = "$DUE_TAG_PREFIX$dueAt"

    private fun workName(syncId: String): String = "$WORK_PREFIX$syncId"
}
