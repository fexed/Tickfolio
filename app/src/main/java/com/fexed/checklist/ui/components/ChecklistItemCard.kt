package com.fexed.checklist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fexed.checklist.R
import coil.compose.AsyncImage
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecklistItemCard(
    item: ChecklistItem,
    onToggleCompleted: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardColor by animateColorAsState(
        targetValue = if (item.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "itemCardColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (item.isCompleted) 0.2f else 0.45f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggleCompleted(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Content: Title, Description, Tags
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleCompleted(!item.isCompleted) }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (item.isCompleted) 0.5f else 0.85f
                        )
                    )
                }

                if (item.tag.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TagChip(tag = item.tag)
                }
            }

            // Attached Picture Thumbnail (if present)
            if (!item.imageUri.isNullOrBlank()) {
                val imageFile = File(item.imageUri)
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 4.dp)
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onViewImage(item.imageUri) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = stringResource(R.string.item_photo_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_item_options_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (!item.imageUri.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_view_photo)) },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onViewImage(item.imageUri)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_edit_item)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete_item)) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Preview(name = "Item Card - Incomplete", showBackground = true, backgroundColor = 0xFFF8F4EC)
@Composable
private fun ChecklistItemCardPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        ChecklistItemCard(
            item = ChecklistItem(
                id = 1,
                checklistId = 1,
                title = "Check Engine Oil Level",
                description = "Warm up engine, wipe dipstick, ensure level is between min and max",
                tag = "Engine",
                isCompleted = false
            ),
            onToggleCompleted = {},
            onEdit = {},
            onDelete = {},
            onViewImage = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Item Card - Completed (Dark)", showBackground = true, backgroundColor = 0xFF171513)
@Composable
private fun ChecklistItemCardCompletedPreview() {
    ChecklistTheme(themeMode = ThemeMode.DARK) {
        ChecklistItemCard(
            item = ChecklistItem(
                id = 2,
                checklistId = 1,
                title = "Passport & International ID",
                description = "Kept in front pocket of backpack",
                tag = "Documents",
                isCompleted = true
            ),
            onToggleCompleted = {},
            onEdit = {},
            onDelete = {},
            onViewImage = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
