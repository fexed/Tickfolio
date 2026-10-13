package com.fexed.checklist.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fexed.checklist.ChecklistApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-registers geofences after events that make the system drop them (reboot, app update).
 */
class GeofenceRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }

        val app = context.applicationContext as ChecklistApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.geofenceManager.restoreGeofences(app.repository.getChecklistsWithGeofence())
            } catch (e: Exception) {
                Log.e("GeofenceRestore", "Failed to restore geofences", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
