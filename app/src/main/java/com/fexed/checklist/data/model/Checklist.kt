package com.fexed.checklist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklists")
data class Checklist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val iconName: String = "list",
    val colorTag: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    // Time-based reminder: timestamp in ms or null
    val reminderTime: Long? = null,
    // Geofence reminder: latitude, longitude, radius in meters, location name
    val geofenceLat: Double? = null,
    val geofenceLng: Double? = null,
    val geofenceRadius: Float = 200f,
    val geofenceName: String? = null,
    val recurrenceFrequency: String? = null,
    val recurrenceTimeMinutes: Int? = null,
    val recurrenceDayOfWeek: Int? = null,
    val recurrenceDayOfMonth: Int? = null,
    val recurrenceNextAt: Long? = null
)
