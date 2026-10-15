package com.fexed.checklist.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.fexed.checklist.R
import com.fexed.checklist.data.preferences.StartPageOption
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.data.preferences.UserPreferences
import com.fexed.checklist.ui.components.DialogPreviewSurface
import com.fexed.checklist.ui.theme.ChecklistTheme
import java.text.DateFormat
import java.util.Date

private const val MAX_DRIVE_FOLDER_NAME_LENGTH = 80
private const val AUTHORIZATION_REQUIRED = "authorization_required"

@Composable
fun SettingsDialog(
    userPreferences: UserPreferences,
    onSetStartPage: (StartPageOption) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDriveFolderName: (String) -> Unit,
    onToggleDriveBackup: (Boolean) -> Unit,
    onReconnectDrive: () -> Unit,
    onLoadSampleData: () -> Unit,
    onDismiss: () -> Unit
) {
    var driveFolderName by remember(userPreferences.driveFolderName) {
        mutableStateOf(userPreferences.driveFolderName)
    }
    val dialogTitle: @Composable () -> Unit = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge
            )
    }
    val dialogContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Default Startup Page
                Text(
                    text = stringResource(R.string.section_launch_screen),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.section_launch_screen_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                StartPageOption.values().forEach { option ->
                    val (labelRes, descRes) = when (option) {
                        StartPageOption.ALL_CHECKLISTS -> Pair(R.string.start_page_all_checklists, R.string.start_page_all_checklists_desc)
                        StartPageOption.LATEST_OPENED -> Pair(R.string.start_page_latest_opened, R.string.start_page_latest_opened_desc)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetStartPage(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = userPreferences.startPage == option,
                            onClick = { onSetStartPage(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Theme
                Text(
                    text = stringResource(R.string.section_appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThemeMode.values().forEach { mode ->
                    val modeLabelRes = when (mode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetThemeMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = userPreferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(modeLabelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.drive_backup_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.drive_backup_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = userPreferences.driveBackupEnabled,
                        onCheckedChange = onToggleDriveBackup,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = driveFolderName,
                    onValueChange = {
                        driveFolderName = it
                            .filterNot { character -> character == '/' || character == '\\' }
                            .take(MAX_DRIVE_FOLDER_NAME_LENGTH)
                    },
                    label = { Text(stringResource(R.string.drive_folder_name_label)) },
                    supportingText = { Text(stringResource(R.string.drive_folder_name_description)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                val validFolderName = driveFolderName.trim().isNotEmpty() &&
                    '/' !in driveFolderName && '\\' !in driveFolderName
                if (driveFolderName.trim() != userPreferences.driveFolderName && validFolderName) {
                    OutlinedButton(
                        onClick = { onSetDriveFolderName(driveFolderName.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.drive_folder_save))
                    }
                }
                userPreferences.lastDriveSyncAt?.let { lastSyncAt ->
                    Text(
                        text = stringResource(
                            R.string.drive_last_sync,
                            DateFormat.getDateTimeInstance().format(Date(lastSyncAt))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                userPreferences.driveSyncError?.let { error ->
                    val message = if (error == AUTHORIZATION_REQUIRED) {
                        R.string.drive_sync_authorization_required
                    } else {
                        R.string.drive_sync_failed
                    }
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (error == AUTHORIZATION_REQUIRED && userPreferences.driveBackupEnabled) {
                        TextButton(onClick = onReconnectDrive) {
                            Text(stringResource(R.string.drive_reconnect))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Sample data
                Text(
                    text = stringResource(R.string.section_sample_checklists),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.section_sample_checklists_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onLoadSampleData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_load_sample_data))
                }
            }
    }
    val confirmButton: @Composable () -> Unit = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.action_done))
            }
    }

    if (LocalInspectionMode.current) {
        DialogPreviewSurface(
            title = dialogTitle,
            content = dialogContent,
            confirmButton = confirmButton
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = dialogTitle,
            text = dialogContent,
            confirmButton = confirmButton,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(name = "Settings Dialog Preview", showBackground = true)
@Composable
private fun SettingsDialogPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        SettingsDialog(
            userPreferences = UserPreferences(
                startPage = StartPageOption.ALL_CHECKLISTS,
                themeMode = ThemeMode.SYSTEM,
                lastOpenedChecklistId = null
            ),
            onSetStartPage = {},
            onSetThemeMode = {},
            onSetDriveFolderName = {},
            onToggleDriveBackup = {},
            onReconnectDrive = {},
            onLoadSampleData = {},
            onDismiss = {}
        )
    }
}
