package com.fexed.checklist.ui.screens.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.core.content.ContextCompat
import com.fexed.checklist.ChecklistApplication
import com.fexed.checklist.data.reminder.ChecklistAlarmReceiver
import com.fexed.checklist.data.reminder.ItemDueDateScheduler
import com.fexed.checklist.ui.components.ReminderConfigDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fexed.checklist.R
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.ui.components.ChecklistItemCard
import com.fexed.checklist.ui.components.EditChecklistDialog
import com.fexed.checklist.ui.components.EditItemBottomSheet
import com.fexed.checklist.ui.components.IconHelper
import com.fexed.checklist.ui.components.ImageViewerDialog
import com.fexed.checklist.ui.components.TagChip
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    viewModel: ChecklistDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as ChecklistApplication
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ChecklistItem?>(null) }
    var viewingImageItem by remember { mutableStateOf<Pair<String, String>?>(null) } // path, title
    var showEditChecklistDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    val dueDatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                val dueItems = app.database.checklistDao().getItemsWithDueDates()
                withContext(Dispatchers.IO) {
                    dueItems
                        .filter {
                            !it.isCompleted && it.dueAt != null &&
                                it.dueNotificationSentAt != it.dueAt
                        }
                        .forEach { item ->
                            ItemDueDateScheduler.schedule(context, item.syncId, item.dueAt!!)
                        }
                }
            }
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val checklistWithItems = uiState.checklistWithItems
    val checklist = checklistWithItems?.checklist

    fun requestDueDateNotificationPermissionIfNeeded(dueAt: Long?) {
        if (dueAt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            dueDatePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (checklist != null) {
                            Icon(
                                imageVector = IconHelper.getIcon(checklist.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                            )
                            Text(
                                text = checklist.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        searchActive = !searchActive
                        if (!searchActive) viewModel.onSearchQueryChanged("")
                    }) {
                        Icon(
                            imageVector = if (searchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_items_desc),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Hide / Show completed toggle
                    IconButton(onClick = { viewModel.toggleHideCompleted() }) {
                        Icon(
                            imageVector = if (uiState.hideCompleted) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.hideCompleted) stringResource(R.string.action_show_all) else stringResource(R.string.action_hide_completed),
                            tint = if (uiState.hideCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu_desc),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_uncheck_all)) },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.resetAllItems()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_check_all)) },
                                leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.completeAllItems()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_reminders_alerts)) },
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
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
                                    showReminderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_edit_details)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showEditChecklistDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_share_checklist)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    checklistWithItems?.let { cwi ->
                                        val sb = StringBuilder()
                                        sb.append(context.getString(R.string.share_checklist_header, cwi.checklist.title))
                                        if (cwi.checklist.description.isNotBlank()) {
                                            sb.append("${cwi.checklist.description}\n")
                                        }
                                        sb.append("\n")
                                        cwi.items.forEach { item ->
                                            val box = when {
                                                item.isCompleted -> "[x]"
                                                item.completedChecks > 0 -> "[${item.completedChecks}/${item.requiredChecks}]"
                                                else -> "[ ]"
                                            }
                                            val tagPart = if (item.tag.isNotBlank()) " (#${item.tag})" else ""
                                            sb.append("$box ${item.title}$tagPart\n")
                                            if (item.description.isNotBlank()) {
                                                sb.append("    ${item.description}\n")
                                            }
                                            item.dueAt?.let { dueAt ->
                                                val formattedDueDate = DateUtils.formatDateTime(
                                                    context,
                                                    dueAt,
                                                    DateUtils.FORMAT_SHOW_DATE or
                                                        DateUtils.FORMAT_ABBREV_MONTH or
                                                        DateUtils.FORMAT_SHOW_TIME or
                                                        DateUtils.FORMAT_SHOW_YEAR
                                                )
                                                val dueDateLabel = context.getString(
                                                    R.string.item_due_date_display,
                                                    formattedDueDate
                                                )
                                                sb.append("    $dueDateLabel\n")
                                            }
                                        }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, cwi.checklist.title)
                                            putExtra(Intent.EXTRA_TEXT, sb.toString())
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_delete_checklist)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
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
            FloatingActionButton(
                onClick = { showAddItemSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_add_item))
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading || checklistWithItems == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search bar
                AnimatedVisibility(visible = searchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.search_items_hint)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
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

                // Summary Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (checklistWithItems.checklist.description.isNotBlank()) {
                            Text(
                                text = checklistWithItems.checklist.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Progress
                        LinearProgressIndicator(
                            progress = { checklistWithItems.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (checklistWithItems.progress >= 1f && checklistWithItems.totalCount > 0)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.progress_format,
                                    checklistWithItems.completedCount,
                                    checklistWithItems.totalCount
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (checklistWithItems.totalCount > 0) {
                                Text(
                                    text = "${(checklistWithItems.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Reminder chips
                        val currentChecklist = checklistWithItems.checklist
                        if (currentChecklist.reminderTime != null || (currentChecklist.geofenceLat != null && currentChecklist.geofenceLng != null)) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentChecklist.reminderTime != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = DateUtils.formatDateTime(
                                                context,
                                                currentChecklist.reminderTime,
                                                DateUtils.FORMAT_SHOW_DATE or
                                                    DateUtils.FORMAT_ABBREV_MONTH or
                                                    DateUtils.FORMAT_SHOW_TIME or
                                                    DateUtils.FORMAT_NO_YEAR
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                if (currentChecklist.geofenceLat != null && currentChecklist.geofenceLng != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (!currentChecklist.geofenceName.isNullOrBlank()) currentChecklist.geofenceName else stringResource(R.string.geofence_active_badge),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tag Filter Chips (All + tags)
                val allTags = checklistWithItems.tags
                if (allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TagChip(
                            tag = stringResource(R.string.tag_filter_all),
                            isSelected = uiState.selectedTag == null,
                            onClick = { viewModel.onTagSelected(null) }
                        )
                        allTags.forEach { tag ->
                            TagChip(
                                tag = tag,
                                isSelected = uiState.selectedTag.equals(tag, ignoreCase = true),
                                onClick = {
                                    viewModel.onTagSelected(
                                        if (uiState.selectedTag.equals(tag, ignoreCase = true)) null else tag
                                    )
                                }
                            )
                        }
                    }
                }

                // Items list
                val itemsToShow = uiState.filteredItems
                if (itemsToShow.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (checklistWithItems.totalCount == 0) stringResource(R.string.empty_detail_no_items)
                                else if (uiState.hideCompleted && checklistWithItems.completedCount > 0) stringResource(R.string.empty_detail_all_completed)
                                else stringResource(R.string.empty_detail_no_matching),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (checklistWithItems.totalCount == 0) stringResource(R.string.empty_detail_tap_to_add)
                                else stringResource(R.string.empty_detail_clear_filters),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = itemsToShow,
                            key = { it.id }
                        ) { item ->
                            ChecklistItemCard(
                                item = item,
                                onProgressChange = { completedChecks ->
                                    viewModel.setItemCheckCount(item.id, completedChecks)
                                },
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.deleteItem(item.id) },
                                onViewImage = { imagePath ->
                                    viewingImageItem = Pair(imagePath, item.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add item bottom sheet
    if (showAddItemSheet) {
        EditItemBottomSheet(
            sheetState = bottomSheetState,
            initialItem = null,
            availableTags = checklistWithItems?.tags ?: emptyList(),
            onDismiss = { showAddItemSheet = false },
            onConfirm = { title, description, tag, imageUri, requiredChecks, dueAt ->
                viewModel.addItem(title, description, tag, imageUri, requiredChecks, dueAt)
                requestDueDateNotificationPermissionIfNeeded(dueAt)
                coroutineScope.launch {
                    bottomSheetState.hide()
                    showAddItemSheet = false
                }
            }
        )
    }

    // Edit item bottom sheet
    editingItem?.let { item ->
        EditItemBottomSheet(
            sheetState = bottomSheetState,
            initialItem = item,
            availableTags = checklistWithItems?.tags ?: emptyList(),
            onDismiss = { editingItem = null },
            onConfirm = { title, description, tag, imageUri, requiredChecks, dueAt ->
                viewModel.updateItem(item, title, description, tag, imageUri, requiredChecks, dueAt)
                requestDueDateNotificationPermissionIfNeeded(dueAt)
                coroutineScope.launch {
                    bottomSheetState.hide()
                    editingItem = null
                }
            }
        )
    }

    // Full screen image viewer dialog
    viewingImageItem?.let { (imagePath, title) ->
        ImageViewerDialog(
            imagePath = imagePath,
            title = title,
            onDismiss = { viewingImageItem = null }
        )
    }

    // Edit checklist dialog
    if (showEditChecklistDialog && checklist != null) {
        EditChecklistDialog(
            initialChecklist = checklist,
            onDismiss = { showEditChecklistDialog = false },
            onConfirm = { title, description, iconName ->
                viewModel.updateChecklist(title, description, iconName)
                showEditChecklistDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_title_delete_checklist), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_msg_delete_named_checklist, checklist?.title ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteChecklist {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reminder Configuration Dialog
    if (showReminderDialog && checklist != null) {
        ReminderConfigDialog(
            checklist = checklist,
            onDismiss = { showReminderDialog = false },
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
            }
        )
    }

}
}
