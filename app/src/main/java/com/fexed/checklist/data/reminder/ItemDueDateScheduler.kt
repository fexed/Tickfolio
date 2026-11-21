package com.fexed.checklist.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.util.RecurrenceSchedule
import java.util.concurrent.TimeUnit

object ItemDueDateScheduler {
    private const val WORK_TAG = "item_due_date"
    private const val WORK_PREFIX = "item_due_date_"
    private const val ITEM_TAG_PREFIX = "item_due_date_sync_"
    private const val DUE_TAG_PREFIX = "item_due_date_at_"

    fun reconcile(context: Context, items: List<ChecklistItem>) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val workInfos: List<WorkInfo> = workManager.getWorkInfosByTag(WORK_TAG).get()
        val now = System.currentTimeMillis()
        val itemsBySyncId = items.associateBy { it.syncId }
        val desiredDueDates = items.mapNotNull { item ->
            if (item.isCompleted) return@mapNotNull null
            val dueAt = item.dueAt ?: return@mapNotNull null
            val recurrence = item.dueRecurrenceFrequency
                ?.let { name -> ChecklistRecurrence.values().firstOrNull { it.name == name } }
            if (recurrence == null && item.dueNotificationSentAt == dueAt) return@mapNotNull null
            val scheduledAt = if (recurrence == null) {
                dueAt
            } else {
                RecurrenceSchedule.nextOccurrenceAtOrAfter(dueAt, now, recurrence)
            }
            item.syncId to scheduledAt
        }.toMap()
        val activeWork = workInfos
            .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            .mapNotNull { info -> itemSyncId(info)?.let { it to info } }
            .groupBy({ it.first }, { it.second })

        (workInfos.mapNotNull(::itemSyncId) + desiredDueDates.keys)
            .distinct()
            .forEach { syncId ->
                val desiredDueAt = desiredDueDates[syncId]
                val item = itemsBySyncId[syncId]
                val hasValidWork = item != null && activeWork[syncId].orEmpty().any { info ->
                    val scheduledAt = info.let(::dueAt) ?: return@any false
                    val recurrence = item.dueRecurrenceFrequency
                        ?.let { name -> ChecklistRecurrence.values().firstOrNull { it.name == name } }
                    if (recurrence == null) {
                        item.dueAt == scheduledAt
                    } else {
                        nextRecurringOccurrence(item, scheduledAt - 1L) == scheduledAt
                    }
                }
                if (desiredDueAt == null) {
                    workManager.cancelUniqueWork(workName(syncId))
                    return@forEach
                }
                if (hasValidWork) return@forEach

                workManager.cancelUniqueWork(workName(syncId))
                workManager.enqueueUniqueWork(
                    workName(syncId),
                    ExistingWorkPolicy.REPLACE,
                    buildWorkRequest(syncId, desiredDueAt)
                )
            }
    }

    fun schedule(context: Context, syncId: String, dueAt: Long) {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName(syncId), ExistingWorkPolicy.REPLACE, buildWorkRequest(syncId, dueAt))
    }

    fun scheduleNext(context: Context, syncId: String, dueAt: Long) {
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName(syncId), ExistingWorkPolicy.APPEND_OR_REPLACE, buildWorkRequest(syncId, dueAt))
    }

    fun nextRecurringOccurrence(item: ChecklistItem, now: Long): Long? {
        val dueAt = item.dueAt ?: return null
        val frequency = item.dueRecurrenceFrequency
            ?.let { name -> ChecklistRecurrence.values().firstOrNull { it.name == name } }
            ?: return null
        return RecurrenceSchedule.nextOccurrenceAtOrAfter(dueAt, now, frequency)
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
