package com.fexed.checklist.data.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fexed.checklist.MainActivity
import com.fexed.checklist.R

class ChecklistAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "checklist_time_reminders"
        const val CHANNEL_NAME = "Checklist Scheduled Reminders"

        const val EXTRA_CHECKLIST_ID = "extra_checklist_id"
        const val EXTRA_CHECKLIST_TITLE = "extra_checklist_title"
        const val EXTRA_CHECKLIST_DESC = "extra_checklist_desc"

        fun scheduleAlarm(
            context: Context,
            checklistId: Long,
            title: String,
            description: String,
            triggerAtMillis: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ChecklistAlarmReceiver::class.java).apply {
                putExtra(EXTRA_CHECKLIST_ID, checklistId)
                putExtra(EXTRA_CHECKLIST_TITLE, title)
                putExtra(EXTRA_CHECKLIST_DESC, description)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                checklistId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // If exact alarms are not permitted on Android 12+, fall back to standard alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context, checklistId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ChecklistAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                checklistId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val checklistId = intent.getLongExtra(EXTRA_CHECKLIST_ID, -1L)
        val title = intent.getStringExtra(EXTRA_CHECKLIST_TITLE) ?: context.getString(R.string.default_alarm_title)
        val desc = intent.getStringExtra(EXTRA_CHECKLIST_DESC) ?: context.getString(R.string.default_alarm_desc)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_checklist_id", checklistId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            checklistId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notification_title_format, title))
            .setContentText(if (desc.isNotBlank()) desc else context.getString(R.string.notification_default_text))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(checklistId.toInt(), notification)
    }
}
