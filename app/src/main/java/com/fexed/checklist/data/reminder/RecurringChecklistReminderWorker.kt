package com.fexed.checklist.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.EXTRA_OPEN_CHECKLIST_ID
import com.fexed.checklist.MainActivity
import com.fexed.checklist.R
import com.fexed.checklist.data.model.ChecklistRecurrence
import com.fexed.checklist.util.RecurrenceSchedule
import kotlinx.coroutines.CancellationException

class RecurringChecklistReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncId = inputData.getString(KEY_CHECKLIST_SYNC_ID) ?: return Result.failure()
        val occurrenceAt = inputData.getLong(KEY_OCCURRENCE_AT, Long.MIN_VALUE)
        if (occurrenceAt == Long.MIN_VALUE) return Result.failure()

        val app = applicationContext as ChecklistApplication
        val dao = app.database.checklistDao()
        val entry = dao.getChecklistBySyncId(syncId)?.let {
            dao.getChecklistWithItemsById(it.id)
        } ?: return Result.success()
        val checklist = entry.checklist
        val frequency = checklist.reminderRecurrenceFrequency
            ?.let { name -> ChecklistRecurrence.values().firstOrNull { it.name == name } }
            ?: return Result.success()
        val timeMinutes = checklist.reminderRecurrenceTimeMinutes ?: return Result.success()
        val dayOfWeek = checklist.reminderRecurrenceDayOfWeek ?: return Result.success()
        val dayOfMonth = checklist.reminderRecurrenceDayOfMonth ?: return Result.success()
        if (checklist.reminderRecurrenceNextAt != occurrenceAt) return Result.success()

        val now = System.currentTimeMillis()
        if (occurrenceAt > now) {
            RecurringChecklistReminderScheduler.scheduleNext(applicationContext, syncId, occurrenceAt)
            return Result.success()
        }
        if (entry.items.isNotEmpty() && entry.items.all { it.isCompleted }) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission is not granted; recurring checklist reminder will retry")
            return Result.retry()
        }

        return try {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        applicationContext.getString(R.string.channel_recurring_reminder_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = applicationContext.getString(R.string.channel_recurring_reminder_description)
                    }
                )
            }

            val notificationId = syncId.hashCode()
            val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_CHECKLIST_ID, checklist.id)
            }
            val contentIntent = PendingIntent.getActivity(
                applicationContext,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationManager.notify(
                notificationId,
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(
                        applicationContext.getString(
                            R.string.recurring_checklist_notification_title,
                            checklist.title
                        )
                    )
                    .setContentText(
                        applicationContext.getString(R.string.recurring_checklist_notification_text)
                    )
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            )

            val nextOccurrence = RecurrenceSchedule.nextOccurrenceAfter(
                previousOccurrence = occurrenceAt,
                now = System.currentTimeMillis(),
                frequency = frequency,
                timeMinutes = timeMinutes,
                dayOfMonth = dayOfMonth
            )
            if (dao.advanceReminderRecurrence(checklist.id, occurrenceAt, nextOccurrence) == 1) {
                RecurringChecklistReminderScheduler.scheduleNext(
                    applicationContext,
                    syncId,
                    nextOccurrence
                )
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to deliver recurring checklist reminder", exception)
            Result.retry()
        }
    }

    companion object {
        const val KEY_CHECKLIST_SYNC_ID = "checklist_sync_id"
        const val KEY_OCCURRENCE_AT = "occurrence_at"
        const val TAG = "ChecklistReminderWorker"
        const val CHANNEL_ID = "recurring_checklist_reminders"
    }
}
