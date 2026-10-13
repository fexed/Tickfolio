package com.fexed.checklist.data.recurrence

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecurringChecklistScheduler {

    private const val WORK_NAME = "recurring_checklist_resets"

    fun ensureScheduled(context: Context) {
        val work = PeriodicWorkRequestBuilder<RecurringChecklistWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
}
