package com.fexed.checklist.data.reminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

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

    @SuppressLint("MissingPermission")
    fun addOrUpdateGeofence(
        checklistId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 200f
    ) {
        val requestId = "checklist_$checklistId"

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(15000) // 15 seconds dwell
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d("GeofenceManager", "Successfully registered geofence for $requestId")
                }
                addOnFailureListener { e ->
                    Log.e("GeofenceManager", "Failed to register geofence for $requestId", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e("GeofenceManager", "Location permission missing for geofence registration", e)
        }
    }

    fun removeGeofence(checklistId: Long) {
        val requestId = "checklist_$checklistId"
        geofencingClient.removeGeofences(listOf(requestId)).run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Removed geofence for $requestId")
            }
            addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove geofence for $requestId", e)
            }
        }
    }
}
