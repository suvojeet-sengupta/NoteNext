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
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import com.suvojeet.notenext.ui.settings.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow

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
 * @param themeMode The current theme mode of the app, used to conditionally apply a border in dark mode.
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
    onAudioClick: () -> Unit,
    themeMode: ThemeMode
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface, // Background color matches the note's editing color.
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
                    var fabCoordinates by remember { mutableStateOf<IntOffset?>(null) }
                    var fabSize by remember { mutableStateOf<IntSize?>(null) }

                    FloatingActionButton(
                        onClick = { showAttachmentMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { coordinates ->
                                fabCoordinates = IntOffset(
                                    coordinates.positionInWindow().x.roundToInt(),
                                    coordinates.positionInWindow().y.roundToInt()
                                )
                                fabSize = coordinates.size
                            },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_attachment))
                    }

                    if (showAttachmentMenu && fabCoordinates != null && fabSize != null) {
                        val xOffset = fabCoordinates!!.x
                        val yOffset = fabCoordinates!!.y - fabSize!!.height

                        AttachmentMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false },
                            offset = IntOffset(x = xOffset, y = yOffset),
                            themeMode = themeMode,
                            onImageClick = onImageClick,
                            onTakePhotoClick = onTakePhotoClick,
                            onAudioClick = onAudioClick
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

/**
 * A custom dropdown menu for attachment options, displayed in a Popup with a fade-in/fade-out animation.
 *
 * @param expanded Whether the menu is currently visible.
 * @param onDismissRequest Lambda to be invoked when the menu should be dismissed.
 * @param offset The offset of the popup from the top-left corner of the screen.
 * @param themeMode The current theme mode, used to apply a border in dark mode.
 * @param onImageClick Lambda for when the "Add Image" option is clicked.
 * @param onTakePhotoClick Lambda for when the "Take Photo" option is clicked.
 * @param onAudioClick Lambda for when the "Audio Recording" option is clicked.
 */
@Composable
private fun AttachmentMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: IntOffset,
    themeMode: ThemeMode,
    onImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        offset = offset
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            val isDark = when (themeMode) {
                ThemeMode.DARK, ThemeMode.AMOLED -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> false
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .then(
                        if (isDark) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.add_image)) },
                        onClick = {
                            onImageClick()
                            onDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.take_photo)) },
                        onClick = {
                            onTakePhotoClick()
                            onDismissRequest()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.audio_recording)) },
                        onClick = {
                            onAudioClick()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

