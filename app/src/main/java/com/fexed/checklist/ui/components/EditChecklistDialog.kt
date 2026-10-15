package com.fexed.checklist.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.fexed.checklist.R
import com.fexed.checklist.data.model.Checklist
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditChecklistDialog(
    initialChecklist: Checklist? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, iconName: String) -> Unit
) {
    var title by remember { mutableStateOf(initialChecklist?.title ?: "") }
    var description by remember { mutableStateOf(initialChecklist?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(initialChecklist?.iconName ?: "list") }
    var titleError by remember { mutableStateOf(false) }

    val dialogTitle: @Composable () -> Unit = {
            Text(
                text = if (initialChecklist == null) stringResource(R.string.dialog_title_new_checklist) else stringResource(R.string.dialog_title_edit_checklist),
                style = MaterialTheme.typography.titleLarge
            )
    }
    val dialogContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text(stringResource(R.string.label_checklist_title)) },
                    placeholder = { Text(stringResource(R.string.placeholder_checklist_title)) },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text(stringResource(R.string.error_title_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.label_checklist_description)) },
                    placeholder = { Text(stringResource(R.string.placeholder_checklist_description)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.label_select_icon),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconHelper.availableIcons.forEach { iconItem ->
                        val isSelected = selectedIcon == iconItem.id
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = iconItem.id },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = iconItem.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
    }
    val confirmButton: @Composable () -> Unit = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onConfirm(title.trim(), description.trim(), selectedIcon)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (initialChecklist == null) stringResource(R.string.action_create) else stringResource(R.string.action_save))
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

@Preview(name = "Edit Checklist Dialog Preview", showBackground = true)
@Composable
private fun EditChecklistDialogPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        EditChecklistDialog(
            initialChecklist = Checklist(
                id = 1,
                title = "Work Trip to Zurich",
                description = "Packing & prep",
                iconName = "trip"
            ),
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
