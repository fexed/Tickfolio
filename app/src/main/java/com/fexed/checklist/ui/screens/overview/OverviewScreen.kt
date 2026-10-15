package com.fexed.checklist.ui.screens.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fexed.checklist.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.preferences.ChecklistSortOption
import com.fexed.checklist.data.reminder.ChecklistAlarmReceiver
import com.fexed.checklist.ui.components.ChecklistCard
import com.fexed.checklist.ui.components.EditChecklistDialog
import com.fexed.checklist.ui.components.ReminderConfigDialog
import com.fexed.checklist.ui.screens.settings.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onToggleDriveBackup: (Boolean) -> Unit,
    onReconnectDrive: () -> Unit,
    onOpenChecklist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val app = context.applicationContext as ChecklistApplication

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingChecklist by remember { mutableStateOf<Checklist?>(null) }
    var reminderChecklist by remember { mutableStateOf<Checklist?>(null) }
    var deletingChecklistId by remember { mutableStateOf<Long?>(null) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var selectedChecklistIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val isSelectionMode = selectedChecklistIds.isNotEmpty()
    val visibleChecklistIds = uiState.checklists.map { it.checklist.id }
    val allVisibleChecklistsSelected = visibleChecklistIds.isNotEmpty() &&
        visibleChecklistIds.all { it in selectedChecklistIds }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedChecklistIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_checklist_selection),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = if (isSelectionMode) {
                            stringResource(R.string.selected_checklists_count, selectedChecklistIds.size)
                        } else {
                            stringResource(R.string.overview_title)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                selectedChecklistIds = if (allVisibleChecklistsSelected) {
                                    emptySet()
                                } else {
                                    selectedChecklistIds + visibleChecklistIds
                                }
                            },
                            enabled = visibleChecklistIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = if (allVisibleChecklistsSelected) {
                                    Icons.Default.Clear
                                } else {
                                    Icons.Default.Checklist
                                },
                                contentDescription = stringResource(
                                    if (allVisibleChecklistsSelected) {
                                        R.string.unselect_all_checklists
                                    } else {
                                        R.string.select_all_checklists
                                    }
                                ),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { showDeleteSelectedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_selected_checklists_desc),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) {
                                viewModel.onSearchQueryChanged("")
                            }
                        }) {
                            Icon(
                                imageVector = if (searchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = if (searchActive) stringResource(R.string.close_search_desc) else stringResource(R.string.search_desc),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.sort_checklists_desc),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                ChecklistSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    when (option) {
                                                        ChecklistSortOption.TITLE -> R.string.sort_by_title
                                                        ChecklistSortOption.DUE_DATE -> R.string.sort_by_due_date
                                                        ChecklistSortOption.LAST_EDITED -> R.string.sort_by_last_edit
                                                        ChecklistSortOption.CREATED -> R.string.sort_by_creation_date
                                                        ChecklistSortOption.LAST_OPENED -> R.string.sort_by_last_opened
                                                    }
                                                )
                                            )
                                        },
                                        onClick = {
                                            viewModel.setChecklistSortOption(option)
                                            sortMenuExpanded = false
                                        },
                                        trailingIcon = {
                                            if (uiState.userPreferences.checklistSortOption == option) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_ascending)) },
                                    onClick = {
                                        viewModel.setChecklistSortAscending(true)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (uiState.userPreferences.checklistSortAscending) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_descending)) },
                                    onClick = {
                                        viewModel.setChecklistSortAscending(false)
                                        sortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (!uiState.userPreferences.checklistSortAscending) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_desc),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_checklist_fab))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            AnimatedVisibility(visible = searchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(stringResource(R.string.search_hint_overview)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search_desc))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // Main Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.checklists.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) stringResource(R.string.empty_checklists_found_title) else stringResource(R.string.empty_checklists_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (uiState.searchQuery.isNotBlank())
                                stringResource(R.string.empty_checklists_found_subtitle)
                            else
                                stringResource(R.string.empty_checklists_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.searchQuery.isBlank()) {
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_new_checklist))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { viewModel.loadSampleData() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_load_sample_data))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.checklists,
                        key = { it.checklist.id }
                    ) { checklistWithItems ->
                        ChecklistCard(
                            checklistWithItems = checklistWithItems,
                            isSelected = checklistWithItems.checklist.id in selectedChecklistIds,
                            onClick = {
                                val id = checklistWithItems.checklist.id
                                if (selectedChecklistIds.isNotEmpty()) {
                                    selectedChecklistIds = if (id in selectedChecklistIds) {
                                        selectedChecklistIds - id
                                    } else {
                                        selectedChecklistIds + id
                                    }
                                } else {
                                    onOpenChecklist(id)
                                }
                            },
                            onLongClick = {
                                val id = checklistWithItems.checklist.id
                                selectedChecklistIds = if (id in selectedChecklistIds) {
                                    selectedChecklistIds - id
                                } else {
                                    selectedChecklistIds + id
                                }
                            },
                            onEdit = { editingChecklist = checklistWithItems.checklist },
                            onDuplicate = { viewModel.duplicateChecklist(checklistWithItems.checklist.id) },
                            onReset = { viewModel.resetChecklist(checklistWithItems.checklist.id) },
                            onDelete = { deletingChecklistId = checklistWithItems.checklist.id },
                            onConfigureReminders = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val notifGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!notifGranted) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.POST_NOTIFICATIONS,
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                reminderChecklist = checklistWithItems.checklist
                            },
                            tagColors = uiState.userPreferences.tagColors
                        )
                    }
                    item(key = "app_name_footer") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for creating a new checklist
    if (showCreateDialog) {
        EditChecklistDialog(
            initialChecklist = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, description, iconName ->
                showCreateDialog = false
                viewModel.createChecklist(title, description, iconName) { newId ->
                    onOpenChecklist(newId)
                }
            }
        )
    }

    // Dialog for editing an existing checklist
    editingChecklist?.let { checklist ->
        EditChecklistDialog(
            initialChecklist = checklist,
            onDismiss = { editingChecklist = null },
            onConfirm = { title, description, iconName ->
                viewModel.updateChecklist(checklist.id, title, description, iconName)
                editingChecklist = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingChecklistId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingChecklistId = null },
            title = { Text(stringResource(R.string.dialog_title_delete_checklist), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_msg_delete_checklist)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChecklist(id)
                        selectedChecklistIds = selectedChecklistIds - id
                        deletingChecklistId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChecklistId = null }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(stringResource(R.string.dialog_title_delete_selected_checklists)) },
            text = {
                Text(stringResource(R.string.dialog_msg_delete_selected_checklists, selectedChecklistIds.size))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChecklists(selectedChecklistIds)
                        selectedChecklistIds = emptySet()
                        showDeleteSelectedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            userPreferences = uiState.userPreferences,
            onSetStartPage = { viewModel.setStartPage(it) },
            onSetThemeMode = { viewModel.setThemeMode(it) },
            onSetDriveFolderName = { viewModel.setDriveFolderName(it) },
            onToggleDriveBackup = onToggleDriveBackup,
            onReconnectDrive = onReconnectDrive,
            onLoadSampleData = { viewModel.loadSampleData() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Reminder Configuration Dialog
    reminderChecklist?.let { checklist ->
        ReminderConfigDialog(
            checklist = checklist,
            onDismiss = { reminderChecklist = null },
            onSaveReminders = { reminderTime, geofenceLat, geofenceLng, geofenceRadius, geofenceName,
                                recurrence, recurrenceTimeMinutes, recurrenceDayOfWeek, recurrenceDayOfMonth ->
                // Update Alarm
                if (reminderTime != null) {
                    ChecklistAlarmReceiver.scheduleAlarm(
                        context = context,
                        checklistId = checklist.id,
                        title = checklist.title,
                        description = checklist.description,
                        triggerAtMillis = reminderTime
                    )
                } else {
                    ChecklistAlarmReceiver.cancelAlarm(context, checklist.id)
                }

                // Update Geofence
                if (geofenceLat != null && geofenceLng != null) {
                    app.geofenceManager.addOrUpdateGeofence(
                        checklistId = checklist.id,
                        latitude = geofenceLat,
                        longitude = geofenceLng,
                        radiusMeters = geofenceRadius
                    )
                } else {
                    app.geofenceManager.removeGeofence(checklist.id)
                }

                viewModel.updateReminders(
                    checklistId = checklist.id,
                    reminderTime = reminderTime,
                    geofenceLat = geofenceLat,
                    geofenceLng = geofenceLng,
                    geofenceRadius = geofenceRadius,
                    geofenceName = geofenceName,
                    recurrence = recurrence,
                    recurrenceTimeMinutes = recurrenceTimeMinutes,
                    recurrenceDayOfWeek = recurrenceDayOfWeek,
                    recurrenceDayOfMonth = recurrenceDayOfMonth
                )
                reminderChecklist = null
            }
        )
    }
}
}
