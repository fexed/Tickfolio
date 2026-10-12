package com.fexed.checklist.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fexed.checklist.R
import com.fexed.checklist.data.model.Checklist
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.ui.tooling.preview.Preview
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme

@Composable
fun ReminderConfigDialog(
    checklist: Checklist,
    onDismiss: () -> Unit,
    onSaveReminders: (
        reminderTime: Long?,
        geofenceLat: Double?,
        geofenceLng: Double?,
        geofenceRadius: Float,
        geofenceName: String?
    ) -> Unit
) {
    val context = LocalContext.current

    // Alarm reminder state
    var enableTimeReminder by remember { mutableStateOf(checklist.reminderTime != null) }
    var selectedTimestamp by remember {
        mutableStateOf(checklist.reminderTime ?: (System.currentTimeMillis() + 3600000L))
    }

    // Geofence reminder state
    var enableGeofence by remember { mutableStateOf(checklist.geofenceLat != null && checklist.geofenceLng != null) }
    var locationName by remember { mutableStateOf(checklist.geofenceName ?: "") }
    var latitudeText by remember { mutableStateOf(checklist.geofenceLat?.toString() ?: "") }
    var longitudeText by remember { mutableStateOf(checklist.geofenceLng?.toString() ?: "") }
    var radiusText by remember { mutableStateOf(checklist.geofenceRadius.toInt().toString()) }
    var showMapPicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy - HH:mm", Locale.getDefault()) }

    if (!showMapPicker) {
        val dialogTitle: @Composable () -> Unit = {
            Text(
                text = stringResource(R.string.dialog_title_reminders),
                style = MaterialTheme.typography.titleLarge
            )
        }
        val dialogContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Scheduled Date & Time Alarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.reminder_time_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.reminder_time_desc),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = enableTimeReminder,
                        onCheckedChange = { enableTimeReminder = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (enableTimeReminder) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                cal.set(Calendar.MINUTE, minute)
                                                cal.set(Calendar.SECOND, 0)
                                                selectedTimestamp = cal.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.scheduled_alarm_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(Date(selectedTimestamp)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Location Geofence Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.reminder_geofence_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.reminder_geofence_desc),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = enableGeofence,
                        onCheckedChange = { enableGeofence = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (enableGeofence) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Map selection launcher button
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (latitudeText.isNotBlank() && longitudeText.isNotBlank())
                                stringResource(R.string.btn_change_location_map)
                            else
                                stringResource(R.string.btn_pick_location_map),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text(stringResource(R.string.label_location_name)) },
                        placeholder = { Text(stringResource(R.string.placeholder_location_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latitudeText,
                            onValueChange = { latitudeText = it },
                            label = { Text(stringResource(R.string.label_latitude)) },
                            placeholder = { Text(stringResource(R.string.placeholder_latitude)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        OutlinedTextField(
                            value = longitudeText,
                            onValueChange = { longitudeText = it },
                            label = { Text(stringResource(R.string.label_longitude)) },
                            placeholder = { Text(stringResource(R.string.placeholder_longitude)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = radiusText,
                        onValueChange = { radiusText = it },
                        label = { Text(stringResource(R.string.label_trigger_radius)) },
                        placeholder = { Text(stringResource(R.string.placeholder_trigger_radius)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
        val confirmButton: @Composable () -> Unit = {
            Button(
                onClick = {
                    val finalReminderTime = if (enableTimeReminder) selectedTimestamp else null
                    val lat = if (enableGeofence) latitudeText.toDoubleOrNull() else null
                    val lng = if (enableGeofence) longitudeText.toDoubleOrNull() else null
                    val radius = if (enableGeofence) radiusText.toFloatOrNull() ?: 200f else 200f
                    val name = if (enableGeofence) locationName.trim() else null

                    onSaveReminders(finalReminderTime, lat, lng, radius, name)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
        val dismissButton: @Composable () -> Unit = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (LocalInspectionMode.current) {
            DialogPreviewSurface(
                title = dialogTitle,
                content = dialogContent,
                confirmButton = confirmButton,
                dismissButton = dismissButton
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = dialogTitle,
                text = dialogContent,
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }

    // Interactive Map Picker Dialog
    if (showMapPicker) {
        LocationPickerMapDialog(
            initialLat = latitudeText.toDoubleOrNull() ?: checklist.geofenceLat,
            initialLng = longitudeText.toDoubleOrNull() ?: checklist.geofenceLng,
            initialRadius = radiusText.toFloatOrNull() ?: checklist.geofenceRadius,
            onDismiss = { showMapPicker = false },
            onLocationSelected = { lat, lng, suggestedName ->
                latitudeText = "%.5f".format(Locale.US, lat)
                longitudeText = "%.5f".format(Locale.US, lng)
                if (!suggestedName.isNullOrBlank()) {
                    locationName = suggestedName
                }
                showMapPicker = false
            }
        )
    }
}

@Preview(name = "Reminder Config Dialog Preview", showBackground = true)
@Composable
private fun ReminderConfigDialogPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        ReminderConfigDialog(
            checklist = Checklist(
                id = 1,
                title = "Work Trip to Zurich",
                description = "Packing & prep for client conference",
                reminderTime = System.currentTimeMillis() + 3600000L,
                geofenceLat = 47.3769,
                geofenceLng = 8.5417,
                geofenceName = "Zurich Airport"
            ),
            onDismiss = {},
            onSaveReminders = { _, _, _, _, _ -> }
        )
    }
}
