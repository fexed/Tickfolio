package com.fexed.checklist

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fexed.checklist.R
import com.fexed.checklist.data.sync.DRIVE_FILE_SCOPE
import com.fexed.checklist.data.sync.DriveSyncScheduler
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.navigation.ChecklistNavGraph
import com.fexed.checklist.ui.theme.ChecklistTheme
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val EXTRA_OPEN_CHECKLIST_ID = "open_checklist_id"

class MainActivity : ComponentActivity() {
    private lateinit var driveAuthorizationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val authorizationError = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        driveAuthorizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                authorizationError.value = true
            } else {
                try {
                    completeDriveAuthorization(
                        Identity.getAuthorizationClient(this)
                            .getAuthorizationResultFromIntent(result.data)
                    )
                } catch (exception: ApiException) {
                    Log.e(TAG, "Google Drive authorization was not completed", exception)
                    authorizationError.value = true
                }
            }
        }

        val app = application as ChecklistApplication
        val openChecklistId = intent?.getLongExtra(EXTRA_OPEN_CHECKLIST_ID, -1L)?.takeIf { it != -1L }

        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                app.geofenceManager.restoreGeofences(app.repository.getChecklistsWithGeofence())
            }
        }

        setContent {
            val userPrefs by app.repository.userPreferences.collectAsState(initial = null)
            val currentUserPrefs = userPrefs
            val themeMode = currentUserPrefs?.themeMode ?: ThemeMode.SYSTEM
            val coroutineScope = rememberCoroutineScope()

            ChecklistTheme(
                themeMode = themeMode,
                handwritingFontEnabled = currentUserPrefs?.handwritingFontEnabled ?: false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ChecklistNavGraph(
                        navController = navController,
                        repository = app.repository,
                        checklistDao = app.database.checklistDao(),
                        userPreferences = currentUserPrefs,
                        initialChecklistId = openChecklistId,
                        onToggleDriveBackup = { enabled ->
                            if (enabled) {
                                coroutineScope.launch {
                                    app.appPreferences.setDriveConsentAsked()
                                }
                                requestDriveAuthorization()
                            } else {
                                coroutineScope.launch {
                                    app.appPreferences.setDriveBackupEnabled(false)
                                }
                            }
                        },
                        onReconnectDrive = ::requestDriveAuthorization
                    )
                }

                if (currentUserPrefs != null && currentUserPrefs.driveBackupEnabled.not() &&
                    currentUserPrefs.driveConsentAsked.not()
                ) {
                    AlertDialog(
                        onDismissRequest = {
                            coroutineScope.launch { app.appPreferences.setDriveConsentAsked() }
                        },
                        title = { Text(stringResource(R.string.drive_consent_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.drive_consent_message,
                                    currentUserPrefs.driveFolderName
                                )
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                coroutineScope.launch {
                                    app.appPreferences.setDriveConsentAsked()
                                }
                                requestDriveAuthorization()
                            }) {
                                Text(stringResource(R.string.drive_consent_enable))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                coroutineScope.launch { app.appPreferences.setDriveConsentAsked() }
                            }) {
                                Text(stringResource(R.string.drive_consent_not_now))
                            }
                        }
                    )
                }

                if (authorizationError.value) {
                    AlertDialog(
                        onDismissRequest = { authorizationError.value = false },
                        title = { Text(stringResource(R.string.drive_auth_failed_title)) },
                        text = { Text(stringResource(R.string.drive_auth_failed_message)) },
                        confirmButton = {
                            TextButton(onClick = { authorizationError.value = false }) {
                                Text(stringResource(R.string.action_done))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestDriveAuthorization() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()
        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener(::completeDriveAuthorization)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Unable to authorize Google Drive access", exception)
                authorizationError.value = true
            }
    }

    private fun completeDriveAuthorization(result: AuthorizationResult) {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
            if (pendingIntent == null) {
                authorizationError.value = true
            } else {
                driveAuthorizationLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }
            return
        }
        if (result.accessToken.isNullOrBlank()) {
            authorizationError.value = true
            return
        }
        val app = application as ChecklistApplication
        lifecycleScope.launch {
            app.appPreferences.setDriveSyncError(null)
            app.appPreferences.setDriveBackupEnabled(true)
            DriveSyncScheduler.ensureScheduled(this@MainActivity)
            DriveSyncScheduler.enqueueNow(this@MainActivity)
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
