package com.fexed.checklist.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.fexed.checklist.data.model.Checklist
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceManager"

        fun hasFineLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Required on Android 10+ to receive geofence transitions while the app is closed.
        fun hasBackgroundLocationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * @param notifyIfAlreadyInside when true, a notification fires immediately if the device is
     * already inside the region. Use false when silently re-registering existing geofences.
     */
    @SuppressLint("MissingPermission")
    fun addOrUpdateGeofence(
        checklistId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 200f,
        notifyIfAlreadyInside: Boolean = true
    ) {
        if (!hasFineLocationPermission(context)) {
            Log.w(TAG, "Fine location permission missing, cannot register geofence for checklist $checklistId")
            return
        }
        if (!hasBackgroundLocationPermission(context)) {
            Log.w(TAG, "Background location permission missing, geofence for checklist $checklistId will only trigger while the app is in use")
        }

        val requestId = "checklist_$checklistId"

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(15000) // 15 seconds dwell
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(if (notifyIfAlreadyInside) GeofencingRequest.INITIAL_TRIGGER_ENTER else 0)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d(TAG, "Successfully registered geofence for $requestId")
                }
                addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofence for $requestId", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for geofence registration", e)
        }
    }

    /**
     * Geofences are cleared by the system on reboot, app update, app data clear and when location
     * services are toggled, so they must be re-registered from the persisted checklists.
     */
    fun restoreGeofences(checklists: List<Checklist>) {
        checklists.forEach { checklist ->
            val lat = checklist.geofenceLat ?: return@forEach
            val lng = checklist.geofenceLng ?: return@forEach
            addOrUpdateGeofence(
                checklistId = checklist.id,
                latitude = lat,
                longitude = lng,
                radiusMeters = checklist.geofenceRadius,
                notifyIfAlreadyInside = false
            )
        }
    }

    fun removeGeofence(checklistId: Long) {
        val requestId = "checklist_$checklistId"
        geofencingClient.removeGeofences(listOf(requestId)).run {
            addOnSuccessListener {
                Log.d(TAG, "Removed geofence for $requestId")
            }
            addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence for $requestId", e)
            }
        }
    }
}
