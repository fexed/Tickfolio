package com.fexed.checklist

import android.app.Application
import com.fexed.checklist.data.local.ChecklistDatabase
import com.fexed.checklist.data.preferences.AppPreferences
import com.fexed.checklist.data.reminder.GeofenceManager
import com.fexed.checklist.data.repository.ChecklistRepository
import com.fexed.checklist.util.SampleDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChecklistApplication : Application() {

    val database by lazy { ChecklistDatabase.getDatabase(this) }
    val appPreferences by lazy { AppPreferences(this) }
    val repository by lazy { ChecklistRepository(database.checklistDao(), appPreferences) }
    val geofenceManager by lazy { GeofenceManager(this) }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Check if database has any checklists on first run; if completely empty, populate presets
        applicationScope.launch {
            val existing = database.checklistDao().getAllChecklistsWithItems().first()
            if (existing.isEmpty()) {
                SampleDataProvider.populateSampleData(database.checklistDao())
            }
        }
    }
}
