package com.fexed.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.navigation.ChecklistNavGraph
import com.fexed.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val EXTRA_OPEN_CHECKLIST_ID = "open_checklist_id"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ChecklistApplication
        val openChecklistId = intent?.getLongExtra(EXTRA_OPEN_CHECKLIST_ID, -1L)?.takeIf { it != -1L }

        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                app.geofenceManager.restoreGeofences(app.repository.getChecklistsWithGeofence())
            }
        }

        setContent {
            val userPrefs by app.repository.userPreferences.collectAsState(initial = null)
            val themeMode = userPrefs?.themeMode ?: ThemeMode.SYSTEM

            ChecklistTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ChecklistNavGraph(
                        navController = navController,
                        repository = app.repository,
                        checklistDao = app.database.checklistDao(),
                        userPreferences = userPrefs,
                        initialChecklistId = openChecklistId
                    )
                }
            }
        }
    }
}
