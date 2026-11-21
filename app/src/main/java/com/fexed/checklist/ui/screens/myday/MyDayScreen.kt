package com.fexed.checklist.ui.screens.myday

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.fexed.checklist.R
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

@Composable
fun MyDayScreen(
    viewModel: MyDayViewModel,
    onNavigateBack: () -> Unit,
    onOpenChecklist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddItemsDialog by remember { mutableStateOf(false) }

    MyDayContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAddItems = { showAddItemsDialog = true },
        onToggleSelection = viewModel::toggleSelection,
        onSetItemCompleted = viewModel::setItemCompleted,
        onOpenChecklist = onOpenChecklist,
        modifier = modifier
    )

    if (showAddItemsDialog) {
        AddMyDayItemsDialog(
            items = uiState.availableItems,
            onToggleSelection = viewModel::toggleSelection,
            onDismiss = { showAddItemsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDayContent(
    uiState: MyDayUiState,
    onNavigateBack: () -> Unit,
    onAddItems: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onSetItemCompleted: (MyDayTask, Boolean) -> Unit,
    onOpenChecklist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val overdueTasks = uiState.tasks.filter { it.isOverdue }
    val dueTasks = uiState.tasks.filter { it.isDueAutomatically && !it.isOverdue }
    val focusTasks = uiState.tasks.filter { !it.isDueAutomatically }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.my_day_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = onAddItems) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.my_day_add_items_desc),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "my_day_summary") {
                    MyDaySummary(
                        today = uiState.today,
                        formatter = dateFormatter,
                        completed = uiState.tasks.count { it.item.isCompleted },
                        total = uiState.tasks.size
                    )
                }

                myDayTaskSection(
                    sectionKey = "my_day_overdue",
                    titleResId = R.string.my_day_overdue,
                    tasks = overdueTasks,
                    today = uiState.today,
                    onSetItemCompleted = onSetItemCompleted,
                    onToggleSelection = onToggleSelection,
                    onOpenChecklist = onOpenChecklist
                )
                myDayTaskSection(
                    sectionKey = "my_day_due",
                    titleResId = R.string.my_day_due_today,
                    tasks = dueTasks,
                    today = uiState.today,
                    onSetItemCompleted = onSetItemCompleted,
                    onToggleSelection = onToggleSelection,
                    onOpenChecklist = onOpenChecklist
                )
                myDayTaskSection(
                    sectionKey = "my_day_focus",
                    titleResId = R.string.my_day_focus_items,
                    tasks = focusTasks,
                    today = uiState.today,
                    onSetItemCompleted = onSetItemCompleted,
                    onToggleSelection = onToggleSelection,
                    onOpenChecklist = onOpenChecklist
                )

                if (uiState.tasks.isEmpty()) {
                    item(key = "my_day_empty") {
                        MyDayEmptyState(
                            onAddItems = onAddItems,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.myDayTaskSection(
    sectionKey: String,
    titleResId: Int,
    tasks: List<MyDayTask>,
    today: LocalDate,
    onSetItemCompleted: (MyDayTask, Boolean) -> Unit,
    onToggleSelection: (String) -> Unit,
    onOpenChecklist: (Long) -> Unit
) {
    if (tasks.isEmpty()) return

    item(key = "${sectionKey}_header") {
        MyDaySectionHeader(stringResource(titleResId))
    }
    items(tasks, key = { it.item.syncId }) { task ->
        MyDayTaskCard(
            task = task,
            today = today,
            onSetCompleted = { completed -> onSetItemCompleted(task, completed) },
            onRemove = { onToggleSelection(task.item.syncId) },
            onOpenChecklist = { onOpenChecklist(task.checklistId) }
        )
    }
}

@Composable
private fun MyDaySummary(
    today: LocalDate,
    formatter: DateTimeFormatter,
    completed: Int,
    total: Int
) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.my_day_date, today.format(formatter)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.my_day_progress, completed, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun MyDaySectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun MyDayTaskCard(
    task: MyDayTask,
    today: LocalDate,
    onSetCompleted: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onOpenChecklist: () -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = remember(context) { AndroidDateFormat.getTimeFormat(context) }
    val dateTimeFormatter = remember(context) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    val zone = ZoneId.systemDefault()
    val dueLabel = task.dueAt?.let { dueAt ->
        val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
        val time = if (date == today) {
            timeFormatter.format(Date(dueAt))
        } else {
            dateTimeFormatter.format(Date(dueAt))
        }
        stringResource(
            if (task.isOverdue) R.string.my_day_due_overdue
            else if (date == today) R.string.my_day_due_today
            else R.string.my_day_due_date,
            time
        )
    }

    Card(
        onClick = onOpenChecklist,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.item.isCompleted,
                onCheckedChange = onSetCompleted
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = task.item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.checklistTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dueLabel != null) {
                    Text(
                        text = dueLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.item.requiredChecks > 1) {
                    Text(
                        text = stringResource(
                            R.string.item_checks_progress,
                            task.item.completedChecks,
                            task.item.requiredChecks
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (task.isUserSelected && !task.isDueAutomatically) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.my_day_remove_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMyDayItemsDialog(
    items: List<MyDayTask>,
    onToggleSelection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.my_day_add_dialog_title)) },
        text = {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.my_day_no_available_items),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(items, key = { it.item.syncId }) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSelection(task.item.syncId) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isUserSelected,
                                onCheckedChange = { onToggleSelection(task.item.syncId) }
                            )
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = task.item.title,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = task.checklistTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun MyDayEmptyState(
    onAddItems: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Today,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.my_day_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.my_day_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddItems) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.my_day_add_items),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Preview(name = "My Day - Paper", showBackground = true, backgroundColor = 0xFFF8F4EC)
@Composable
private fun MyDayScreenPreview() {
    val today = LocalDate.now()
    val dueAt = today.atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        MyDayContent(
            uiState = MyDayUiState(
                today = today,
                tasks = listOf(
                    MyDayTask(
                        item = ChecklistItem(
                            id = 1L,
                            checklistId = 1L,
                            title = "Pack charger",
                            dueAt = dueAt,
                            syncId = "preview"
                        ),
                        checklistId = 1L,
                        checklistTitle = "Weekend trip",
                        dueAt = dueAt,
                        isDueAutomatically = true,
                        isOverdue = false,
                        isUserSelected = false
                    )
                ),
                isLoading = false
            ),
            onNavigateBack = {},
            onAddItems = {},
            onToggleSelection = {},
            onSetItemCompleted = { _, _ -> },
            onOpenChecklist = {}
        )
    }
}
