package com.fexed.checklist.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fexed.checklist.ChecklistApplication
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

class DriveSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ChecklistApplication
        val preferences = app.appPreferences.userPreferencesFlow
        val userPreferences = preferences.first()
        if (!userPreferences.driveBackupEnabled) return Result.success()

        return syncMutex.withLock {
            try {
                withContext(Dispatchers.IO) {
                    val authorization = Tasks.await(
                        Identity.getAuthorizationClient(applicationContext).authorize(
                            AuthorizationRequest.builder()
                                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                                .build()
                        )
                    )
                    if (authorization.hasResolution()) {
                        throw DriveAuthorizationRequiredException()
                    }
                    val token = authorization.accessToken
                        ?: throw DriveAuthorizationRequiredException()
                    DriveSyncEngine(applicationContext, app.database).sync(
                        accessToken = token,
                        folderName = userPreferences.driveFolderName
                    )
                    app.geofenceManager.restoreGeofences(
                        app.repository.getChecklistsWithGeofence()
                    )
                }
                app.appPreferences.setLastDriveSyncAt(System.currentTimeMillis())
                Result.success()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DriveAuthorizationRequiredException) {
                app.appPreferences.setDriveSyncError(AUTHORIZATION_REQUIRED)
                Log.w(TAG, "Drive authorization must be renewed in the app")
                Result.failure()
            } catch (exception: DriveApiException) {
                if (exception.statusCode == 401 || exception.statusCode == 403) {
                    app.appPreferences.setDriveSyncError(AUTHORIZATION_REQUIRED)
                    Log.w(TAG, "Google Drive rejected the current authorization")
                    Result.failure()
                } else {
                    retry(app, exception)
                }
            } catch (exception: Exception) {
                retry(app, exception)
            }
        }
    }

    private suspend fun retry(app: ChecklistApplication, exception: Exception): Result {
        app.appPreferences.setDriveSyncError(SYNC_FAILED)
        Log.e(TAG, "Google Drive sync failed", exception)
        return Result.retry()
    }

    private companion object {
        const val TAG = "DriveSyncWorker"
        const val AUTHORIZATION_REQUIRED = "authorization_required"
        const val SYNC_FAILED = "sync_failed"
        val syncMutex = Mutex()
    }
}

private class DriveAuthorizationRequiredException : IOException()
