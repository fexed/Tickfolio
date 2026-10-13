package com.fexed.checklist.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fexed.checklist.R
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.model.ChecklistWithItems
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecklistCard(
    checklistWithItems: ChecklistWithItems,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onConfigureReminders: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val checklist = checklistWithItems.checklist
    val totalCount = checklistWithItems.totalCount
    val completedCount = checklistWithItems.completedCount
    val progress = checklistWithItems.progress
    val tags = checklistWithItems.tags

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon + Title + Dropdown Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category / Context Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconHelper.getIcon(checklist.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checklist.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (checklist.description.isNotBlank()) {
                        Text(
                            text = checklist.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.checklist_options_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (onConfigureReminders != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_reminders_alerts)) },
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onConfigureReminders()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_reset_uncheck_all)) },
                            leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReset()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_duplicate)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 1f && totalCount > 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalCount == 0) stringResource(R.string.progress_no_items) else stringResource(R.string.progress_format, completedCount, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalCount > 0) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reminder indicators (if scheduled or geofenced)
            if (checklist.reminderTime != null || (checklist.geofenceLat != null && checklist.geofenceLng != null)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (checklist.reminderTime != null) {
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
                                    LocalContext.current,
                                    checklist.reminderTime,
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

                    if (checklist.geofenceLat != null && checklist.geofenceLng != null) {
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
                                text = if (!checklist.geofenceName.isNullOrBlank()) checklist.geofenceName else stringResource(R.string.geofence_active_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Tags preview
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.take(4).forEach { tag ->
                        TagChip(tag = tag)
                    }
                    if (tags.size > 4) {
                        Text(
                            text = stringResource(R.string.more_tags_format, tags.size - 4),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Checklist Card - Paper Light", showBackground = true, backgroundColor = 0xFFF8F4EC)
@Composable
private fun ChecklistCardLightPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        val checklist = Checklist(
            id = 1,
            title = "Weekend Apartment Deep Clean",
            description = "Room-by-room tidy up and disinfection",
            iconName = "home",
            reminderTime = System.currentTimeMillis() + 3600000L,
            geofenceLat = 45.0,
            geofenceLng = 9.0,
            geofenceName = "Home"
        )
        val items = listOf(
            ChecklistItem(id = 1, checklistId = 1, title = "Kitchen stove", tag = "Kitchen", isCompleted = true),
            ChecklistItem(id = 2, checklistId = 1, title = "Dust shelves", tag = "Living", isCompleted = true),
            ChecklistItem(id = 3, checklistId = 1, title = "Mop floors", tag = "General", isCompleted = false)
        )
        ChecklistCard(
            checklistWithItems = ChecklistWithItems(checklist, items),
            onClick = {},
            onEdit = {},
            onDuplicate = {},
            onReset = {},
            onDelete = {},
            onConfigureReminders = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Checklist Card - Parchment Dark", showBackground = true, backgroundColor = 0xFF171513)
@Composable
private fun ChecklistCardDarkPreview() {
    ChecklistTheme(themeMode = ThemeMode.DARK) {
        val checklist = Checklist(
            id = 2,
            title = "Gaming PC Build & Setup",
            description = "Components unboxing and cable management",
            iconName = "gaming",
            reminderTime = null,
            geofenceLat = null,
            geofenceLng = null
        )
        val items = listOf(
            ChecklistItem(id = 4, checklistId = 2, title = "Install CPU & Cooler", tag = "Hardware", isCompleted = true),
            ChecklistItem(id = 5, checklistId = 2, title = "Route PSU cables", tag = "Hardware", isCompleted = false)
        )
        ChecklistCard(
            checklistWithItems = ChecklistWithItems(checklist, items),
            onClick = {},
            onEdit = {},
            onDuplicate = {},
            onReset = {},
            onDelete = {},
            onConfigureReminders = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
