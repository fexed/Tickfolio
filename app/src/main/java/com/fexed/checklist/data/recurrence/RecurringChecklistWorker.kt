package com.fexed.checklist.data.recurrence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.util.RecurrenceSchedule
import kotlinx.coroutines.CancellationException

class RecurringChecklistWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ChecklistApplication
        val dao = app.database.checklistDao()

        return try {
            val now = System.currentTimeMillis()
            dao.getDueRecurringChecklists(now).forEach { checklist ->
                val frequency = ChecklistRecurrence.values().firstOrNull {
                    it.name == checklist.recurrenceFrequency
                }
                val timeMinutes = checklist.recurrenceTimeMinutes
                val dayOfMonth = checklist.recurrenceDayOfMonth
                val previousOccurrence = checklist.recurrenceNextAt
                if (frequency == null || timeMinutes == null || dayOfMonth == null || previousOccurrence == null) {
                    Log.e(TAG, "Invalid recurrence configuration for checklist ${checklist.id}")
                    return Result.failure()
                }

                val nextOccurrence = RecurrenceSchedule.nextOccurrenceAfter(
                    previousOccurrence = previousOccurrence,
                    now = now,
                    frequency = frequency,
                    timeMinutes = timeMinutes,
                    dayOfMonth = dayOfMonth
                )
                dao.resetRecurringChecklist(
                    checklistId = checklist.id,
                    expectedNextAt = previousOccurrence,
                    nextAt = nextOccurrence,
                    updatedAt = now
                )
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to reset due recurring checklists", exception)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "RecurringChecklistWorker"
    }
}
