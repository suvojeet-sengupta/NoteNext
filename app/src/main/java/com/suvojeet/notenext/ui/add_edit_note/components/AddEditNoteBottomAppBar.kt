package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

/**
 * Bottom app bar for the Add/Edit Note screen. Provides quick access to actions
 * like adding attachments, changing note color, formatting text, undo/redo, and more options.
 *
 * @param state The current [NotesState] containing information about the note being edited.
 * @param onEvent Lambda to dispatch [NotesEvent]s for various actions.
 * @param showColorPicker Lambda to show/hide the color picker.
 * @param showFormatBar Lambda to show/hide the format bar.
 * @param showMoreOptions Lambda to show/hide the more options sheet.
 * @param onImageClick Lambda to be invoked when "Add Image" is selected from the attachment menu.
 * @param onTakePhotoClick Lambda to be invoked when "Take Photo" is selected from the attachment menu.
 * @param onAudioClick Lambda to be invoked when "Audio Recording" is selected from the attachment menu.
 */
@Composable
fun AddEditNoteBottomAppBar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    showColorPicker: (Boolean) -> Unit,
    showFormatBar: (Boolean) -> Unit,
    showMoreOptions: (Boolean) -> Unit,
    onImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }

    BottomAppBar(
        containerColor = Color(state.editingColor), // Background color matches the note's editing color.
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp) // Remove default window insets for full-bleed.
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left-aligned action buttons: Add Attachment, Color Picker, Format Bar.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Attachment menu FAB.
                Box {
                    FloatingActionButton(
                        onClick = { showAttachmentMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_attachment))
                    }
                    // Dropdown menu for attachment options.
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.add_image)) },
                            onClick = {
                                onImageClick()
                                showAttachmentMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.take_photo)) },
                            onClick = {
                                onTakePhotoClick()
                                showAttachmentMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.audio_recording)) },
                            onClick = {
                                onAudioClick()
                                showAttachmentMenu = false
                            }
                        )
                    }
                }
                // Color picker FAB.
                FloatingActionButton(
                    onClick = { showColorPicker(true) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Palette, contentDescription = stringResource(id = R.string.toggle_color_picker))
                }
                // Format bar FAB.
                FloatingActionButton(
                    onClick = { showFormatBar(true) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = stringResource(id = R.string.toggle_format_bar))
                }
            }
            // Right-aligned action buttons: Undo, Redo, More Options.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo/Redo buttons are only visible if there's editing history.
                if (state.editingHistory.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Undo Button: enabled if there are previous states to undo to.
                        FloatingActionButton(
                            onClick = { onEvent(NotesEvent.OnUndoClick) },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = if (state.editingHistoryIndex > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ) // Dimmed if no undo available.
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = stringResource(id = R.string.undo)
                            )
                        }

                        // Redo Button: enabled if there are future states to redo to.
                        FloatingActionButton(
                            onClick = { onEvent(NotesEvent.OnRedoClick) },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = if (state.editingHistoryIndex < state.editingHistory.size - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            ) // Dimmed if no redo available.
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = stringResource(id = R.string.redo)
                            )
                        }
                    }
                }

                // More options FAB.
                Box {
                    FloatingActionButton(
                        onClick = { showMoreOptions(true) },
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.more_options)
                        )
                    }
                }
            }
        }
    }
}
