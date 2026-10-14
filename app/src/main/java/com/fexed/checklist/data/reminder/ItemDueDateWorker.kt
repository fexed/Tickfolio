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
import kotlinx.coroutines.CancellationException

class ItemDueDateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val syncId = inputData.getString(KEY_ITEM_SYNC_ID)
            ?: return Result.failure()
        val expectedDueAt = inputData.getLong(KEY_DUE_AT, Long.MIN_VALUE)
        if (expectedDueAt == Long.MIN_VALUE) return Result.failure()

        val app = applicationContext as ChecklistApplication
        val dao = app.database.checklistDao()
        val item = dao.getItemBySyncId(syncId) ?: return Result.success()
        if (item.dueAt != expectedDueAt || item.isCompleted ||
            item.dueNotificationSentAt == expectedDueAt
        ) {
            return Result.success()
        }
        if (expectedDueAt > System.currentTimeMillis()) {
            ItemDueDateScheduler.schedule(applicationContext, syncId, expectedDueAt)
            return Result.success()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission is not granted; item due reminder was not posted")
            return Result.failure()
        }

        val checklist = dao.getChecklistById(item.checklistId) ?: return Result.success()
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
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.channel_item_due_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = applicationContext.getString(R.string.channel_item_due_description)
                }
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(applicationContext.getString(R.string.item_due_notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.item_due_notification_text,
                    item.title,
                    checklist.title
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    applicationContext.getString(
                        R.string.item_due_notification_text,
                        item.title,
                        checklist.title
                    )
                )
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            manager.notify(notificationId, notification)
            dao.markDueNotificationSent(syncId, expectedDueAt)
            return Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to post item due-date notification", exception)
            return Result.retry()
        }
    }

    companion object {
        const val KEY_ITEM_SYNC_ID = "item_sync_id"
        const val KEY_DUE_AT = "due_at"
        private const val TAG = "ItemDueDateWorker"
        private const val CHANNEL_ID = "item_due_date_notifications"
    }
}
