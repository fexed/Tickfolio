package com.fexed.checklist

import android.app.Application
import com.fexed.checklist.data.local.ChecklistDatabase
import com.fexed.checklist.data.preferences.AppPreferences
import com.fexed.checklist.data.reminder.GeofenceManager
import com.fexed.checklist.data.recurrence.RecurringChecklistScheduler
import com.fexed.checklist.data.sync.DriveSyncScheduler
import com.fexed.checklist.data.repository.ChecklistRepository
import com.fexed.checklist.util.SampleDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.fexed.checklist.widget.ChecklistWidgetUpdater

class ChecklistApplication : Application() {

    val database by lazy { ChecklistDatabase.getDatabase(this) }
    val appPreferences by lazy { AppPreferences(this) }
    val repository by lazy { ChecklistRepository(database.checklistDao(), appPreferences) }
    val geofenceManager by lazy { GeofenceManager(this) }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        RecurringChecklistScheduler.ensureScheduled(this)
        applicationScope.launch {
            database.invalidationTracker
                .createFlow("checklists", "checklist_items", "sync_tombstones")
                .debounce(250)
                .collect {
                    ChecklistWidgetUpdater.updateAll(this@ChecklistApplication)
                }
        }
        applicationScope.launch {
            database.invalidationTracker
                .createFlow("checklists", "checklist_items", "sync_tombstones")
                .debounce(2_000)
                .collect {
                    val backupEnabled = appPreferences.userPreferencesFlow
                        .map { it.driveBackupEnabled }
                        .first()
                    if (backupEnabled) DriveSyncScheduler.enqueueNow(this@ChecklistApplication)
                }
        }
        applicationScope.launch {
            appPreferences.userPreferencesFlow
                .map { it.driveBackupEnabled to it.driveFolderName }
                .distinctUntilChanged()
                .collect { (enabled, _) ->
                    if (enabled) {
                        DriveSyncScheduler.ensureScheduled(this@ChecklistApplication)
                        DriveSyncScheduler.enqueueNow(this@ChecklistApplication)
                    } else {
                        DriveSyncScheduler.cancel(this@ChecklistApplication)
                    }
                }
        }
        applicationScope.launch {
            appPreferences.userPreferencesFlow
                .map { it.themeMode }
                .distinctUntilChanged()
                .collect {
                    ChecklistWidgetUpdater.updateAll(this@ChecklistApplication)
                }
        }
    }
}
