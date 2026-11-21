package com.fexed.checklist.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.util.RecurrenceSchedule
import java.util.concurrent.TimeUnit

object RecurringChecklistReminderScheduler {
    private const val WORK_TAG = "recurring_checklist_reminder"
    private const val WORK_PREFIX = "recurring_checklist_reminder_"
    private const val CHECKLIST_TAG_PREFIX = "recurring_checklist_sync_"
    private const val OCCURRENCE_TAG_PREFIX = "recurring_checklist_at_"

    suspend fun reconcile(context: Context, checklists: List<ChecklistWithItems>) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val dao = (context.applicationContext as ChecklistApplication).database.checklistDao()
        val workInfos = workManager.getWorkInfosByTag(WORK_TAG).get()
        val activeWork = workInfos
            .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
            .mapNotNull { info -> checklistSyncId(info)?.let { it to info } }
            .groupBy({ it.first }, { it.second })
        val checklistsBySyncId = checklists.associateBy { it.checklist.syncId }
        val now = System.currentTimeMillis()
        val desired = mutableMapOf<String, Long>()

        checklists.forEach { entry ->
            val checklist = entry.checklist
            val frequency = checklist.reminderRecurrenceFrequency
                ?.let { name -> ChecklistRecurrence.values().firstOrNull { it.name == name } }
                ?: return@forEach
            val timeMinutes = checklist.reminderRecurrenceTimeMinutes ?: return@forEach
            val dayOfWeek = checklist.reminderRecurrenceDayOfWeek ?: return@forEach
            val dayOfMonth = checklist.reminderRecurrenceDayOfMonth ?: return@forEach
            val isComplete = entry.items.isNotEmpty() && entry.items.all { it.isCompleted }
            if (isComplete) return@forEach

            val hasValidWork = activeWork[checklist.syncId].orEmpty().any { info ->
                occurrenceAt(info) == checklist.reminderRecurrenceNextAt
            }
            if (hasValidWork) {
                checklist.reminderRecurrenceNextAt?.let { desired[checklist.syncId] = it }
                return@forEach
            }

            val storedNextAt = checklist.reminderRecurrenceNextAt
            val nextAt = when {
                storedNextAt == null -> RecurrenceSchedule.firstOccurrenceAfter(
                    now,
                    frequency,
                    timeMinutes,
                    dayOfWeek,
                    dayOfMonth
                )
                storedNextAt <= now -> RecurrenceSchedule.nextOccurrenceAfter(
                    storedNextAt,
                    now,
                    frequency,
                    timeMinutes,
                    dayOfMonth
                )
                else -> storedNextAt
            }
            if (nextAt != storedNextAt) {
                dao.updateReminderRecurrenceNextAt(checklist.id, nextAt)
            }
            desired[checklist.syncId] = nextAt
        }

        (workInfos.mapNotNull(::checklistSyncId) + checklistsBySyncId.keys)
            .distinct()
            .forEach { syncId ->
                val target = desired[syncId]
                val active = activeWork[syncId].orEmpty()
                if (target == null) {
                    workManager.cancelUniqueWork(workName(syncId))
                    return@forEach
                }
                val nextAt = checklistsBySyncId[syncId]?.checklist?.reminderRecurrenceNextAt
                if (active.any { occurrenceAt(it) == nextAt }) return@forEach
                if (active.any { occurrenceAt(it) != nextAt }) {
                    workManager.cancelUniqueWork(workName(syncId))
                }
                if (active.none { occurrenceAt(it) == target }) {
                    workManager.enqueueUniqueWork(
                        workName(syncId),
                        ExistingWorkPolicy.REPLACE,
                        buildWorkRequest(syncId, target)
                    )
                }
            }
    }

    fun scheduleNext(context: Context, syncId: String, occurrenceAt: Long) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(syncId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            buildWorkRequest(syncId, occurrenceAt)
        )
    }

    private fun buildWorkRequest(syncId: String, occurrenceAt: Long) =
        OneTimeWorkRequestBuilder<RecurringChecklistReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putString(RecurringChecklistReminderWorker.KEY_CHECKLIST_SYNC_ID, syncId)
                    .putLong(RecurringChecklistReminderWorker.KEY_OCCURRENCE_AT, occurrenceAt)
                    .build()
            )
            .setInitialDelay(
                (occurrenceAt - System.currentTimeMillis()).coerceAtLeast(0L),
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_TAG)
            .addTag(checklistTag(syncId))
            .addTag(occurrenceTag(occurrenceAt))
            .build()

    private fun checklistSyncId(workInfo: WorkInfo): String? =
        workInfo.tags.firstOrNull { it.startsWith(CHECKLIST_TAG_PREFIX) }
            ?.removePrefix(CHECKLIST_TAG_PREFIX)

    private fun occurrenceAt(workInfo: WorkInfo): Long? =
        workInfo.tags.firstOrNull { it.startsWith(OCCURRENCE_TAG_PREFIX) }
            ?.removePrefix(OCCURRENCE_TAG_PREFIX)
            ?.toLongOrNull()

    private fun checklistTag(syncId: String) = "$CHECKLIST_TAG_PREFIX$syncId"

    private fun occurrenceTag(occurrenceAt: Long) = "$OCCURRENCE_TAG_PREFIX$occurrenceAt"

    private fun workName(syncId: String) = "$WORK_PREFIX$syncId"
}
