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
        containerColor = Color(state.editingColor),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    FloatingActionButton(
                        onClick = { showAttachmentMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add attachment")
                    }
                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add image") },
                            onClick = {
                                onImageClick()
                                showAttachmentMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Take photo") },
                            onClick = {
                                onTakePhotoClick()
                                showAttachmentMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio Recording") },
                            onClick = {
                                onAudioClick()
                                showAttachmentMenu = false
                            }
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { showColorPicker(true) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Toggle color picker")
                }
                FloatingActionButton(
                    onClick = { showFormatBar(true) },
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Toggle format bar")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.editingHistory.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Undo Button
                        FloatingActionButton(
                            onClick = { onEvent(NotesEvent.OnUndoClick) },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = if (state.editingHistoryIndex > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                contentDescription = "Undo"
                            )
                        }

                        // Redo Button
                        FloatingActionButton(
                            onClick = { onEvent(NotesEvent.OnRedoClick) },
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = if (state.editingHistoryIndex < state.editingHistory.size - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                contentDescription = "Redo"
                            )
                        }
                    }
                }

                // 3-dot button
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
                            contentDescription = "More options"
                        )
                    }
                }
            }
        }
    }
}
