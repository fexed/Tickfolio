package com.fexed.checklist.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fexed.checklist.R
import com.fexed.checklist.data.model.ChecklistItem
import com.fexed.checklist.data.preferences.ThemeMode
import com.fexed.checklist.ui.theme.ChecklistTheme
import com.fexed.checklist.util.ImageStorageHelper
import java.io.File
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditItemBottomSheet(
    sheetState: SheetState,
    initialItem: ChecklistItem? = null,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        tag: String,
        imageUri: String?,
        requiredChecks: Int,
        dueAt: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var tag by remember { mutableStateOf(initialItem?.tag ?: "") }
    var imageUri by remember { mutableStateOf(initialItem?.imageUri) }
    var titleError by remember { mutableStateOf(false) }
    var requiredChecks by remember { mutableStateOf(initialItem?.requiredChecks?.toString() ?: "1") }
    var requiredChecksError by remember { mutableStateOf(false) }
    var dueAt by remember(initialItem?.syncId) { mutableStateOf(initialItem?.dueAt) }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    fun showDueDatePicker() {
        val current = Calendar.getInstance().apply {
            dueAt?.let(::setTimeInMillis)
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    dueAt?.let { setTimeInMillis(it) }
                    set(year, month, dayOfMonth)
                }
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selected.set(Calendar.MINUTE, minute)
                        selected.set(Calendar.SECOND, 0)
                        selected.set(Calendar.MILLISECOND, 0)
                        dueAt = selected.timeInMillis
                    },
                    current.get(Calendar.HOUR_OF_DAY),
                    current.get(Calendar.MINUTE),
                    AndroidDateFormat.is24HourFormat(context)
                ).show()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                // If there was an old image, delete it
                if (imageUri != null && imageUri != initialItem?.imageUri) {
                    ImageStorageHelper.deleteImage(imageUri)
                }
                imageUri = savedPath
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingCameraFile != null) {
            val file = pendingCameraFile!!
            val savedPath = ImageStorageHelper.saveImageToInternalStorage(context, Uri.fromFile(file))
            if (file.exists()) file.delete()
            if (savedPath != null) {
                if (imageUri != null && imageUri != initialItem?.imageUri) {
                    ImageStorageHelper.deleteImage(imageUri)
                }
                imageUri = savedPath
            }
        }
    }

    val sheetContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                text = if (initialItem == null) stringResource(R.string.sheet_title_new_item) else stringResource(R.string.sheet_title_edit_item),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text(stringResource(R.string.label_item_title)) },
                placeholder = { Text(stringResource(R.string.placeholder_item_title)) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.error_title_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = requiredChecks,
                onValueChange = {
                    requiredChecks = it.filter(Char::isDigit)
                    requiredChecksError = false
                },
                label = { Text(stringResource(R.string.label_required_checks)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = requiredChecksError,
                supportingText = if (requiredChecksError) {
                    { Text(stringResource(R.string.error_required_checks)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.label_item_description)) },
                placeholder = { Text(stringResource(R.string.placeholder_item_description)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tag input
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text(stringResource(R.string.label_item_tag)) },
                placeholder = { Text(stringResource(R.string.placeholder_item_tag)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Tag suggestions chips
            if (availableTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_existing_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTags.forEach { existingTag ->
                        TagChip(
                            tag = existingTag,
                            isSelected = tag.equals(existingTag, ignoreCase = true),
                            onClick = { tag = existingTag }
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.item_due_date_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = ::showDueDatePicker,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dueAt?.let {
                            java.text.DateFormat.getDateTimeInstance().format(Date(it))
                        } ?: stringResource(R.string.item_due_date_set),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (dueAt != null) {
                    TextButton(onClick = { dueAt = null }) {
                        Text(stringResource(R.string.item_due_date_clear))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Picture section
            Text(
                text = stringResource(R.string.label_attached_photo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (imageUri.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                            pendingCameraFile = file
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_camera))
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_gallery))
                    }
                }
            } else {
                val imageFile = File(imageUri!!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = stringResource(R.string.item_photo_desc),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.photo_attached),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    val (uri, file) = ImageStorageHelper.createTempCameraUri(context)
                                    pendingCameraFile = file
                                    cameraLauncher.launch(uri)
                                }
                            ) {
                                Text(stringResource(R.string.btn_retake))
                            }
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.btn_gallery))
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (imageUri != initialItem?.imageUri) {
                                ImageStorageHelper.deleteImage(imageUri)
                            }
                            imageUri = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remove_photo_desc),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val checkCount = requiredChecks.toIntOrNull()
                        titleError = title.isBlank()
                        requiredChecksError = checkCount == null || checkCount < 1
                        if (!titleError && checkCount != null && checkCount > 0) {
                            onConfirm(
                                title.trim(),
                                description.trim(),
                                tag.trim(),
                                imageUri,
                                checkCount,
                                dueAt
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (initialItem == null) stringResource(R.string.btn_add_item) else stringResource(R.string.action_save))
                }
            }
        }
    }

    if (LocalInspectionMode.current) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 6.dp
        ) {
            sheetContent()
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            sheetContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "New checklist item", showBackground = true)
@Composable
private fun EditItemBottomSheetPreview() {
    ChecklistTheme(themeMode = ThemeMode.LIGHT) {
        EditItemBottomSheet(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            ),
            availableTags = listOf("Essentials", "Documents", "Clothing"),
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _ -> }
        )
    }
}
