@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlin.math.roundToInt

@Composable
fun AddEditNoteBottomAppBar(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    showColorPicker: (Boolean) -> Unit,
    showFormatBar: (Boolean) -> Unit,
    showReminderDialog: (Boolean) -> Unit,
    showMoreOptions: (Boolean) -> Unit,
    onImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAudioClick: () -> Unit,
    themeMode: ThemeMode,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    var showAttachmentMenu by remember { mutableStateOf(false) }

    BottomAppBar(
        containerColor = backgroundColor,
        windowInsets = BottomAppBarDefaults.windowInsets,
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonGroup(
                modifier = Modifier, 
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    var fabCoordinates by remember { mutableStateOf<IntOffset?>(null) }
                    var fabSize by remember { mutableStateOf<IntSize?>(null) }

                    FloatingActionButton(
                        onClick = { showAttachmentMenu = true },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .size(36.dp)
                            .springPress()
                            .onGloballyPositioned { coordinates ->
                                fabCoordinates = IntOffset(
                                    coordinates.positionInWindow().x.roundToInt(),
                                    coordinates.positionInWindow().y.roundToInt()
                                )
                                fabSize = coordinates.size
                            },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_attachment), modifier = Modifier.size(18.dp))
                    }

                    if (showAttachmentMenu) {
                        val coords = fabCoordinates
                        val size = fabSize
                        if (coords != null && size != null) {
                            AttachmentMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false },
                                offset = IntOffset(x = coords.x, y = coords.y - size.height),
                                themeMode = themeMode,
                                onImageClick = onImageClick,
                                onTakePhotoClick = onTakePhotoClick,
                                onAudioClick = onAudioClick
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showColorPicker(true) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(36.dp).springPress(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Palette, contentDescription = stringResource(id = R.string.toggle_color_picker), modifier = Modifier.size(18.dp))
                }
                FloatingActionButton(
                    onClick = { showFormatBar(true) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(36.dp).springPress(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = stringResource(id = R.string.toggle_format_bar), modifier = Modifier.size(18.dp))
                }
                FloatingActionButton(
                    onClick = { showReminderDialog(true) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(36.dp).springPress(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Set Reminder", modifier = Modifier.size(18.dp))
                }
            }

            ButtonGroup(
                modifier = Modifier, 
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (state.canUndo || state.canRedo) {
                    FloatingActionButton(
                        onClick = { onEvent(NotesEvent.OnUndoClick) },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.size(36.dp).springPress(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (state.canUndo) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(id = R.string.undo), modifier = Modifier.size(18.dp))
                    }

                    FloatingActionButton(
                        onClick = { onEvent(NotesEvent.OnRedoClick) },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.size(36.dp).springPress(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (state.canRedo) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = stringResource(id = R.string.redo), modifier = Modifier.size(18.dp))
                    }
                }

                FloatingActionButton(
                    onClick = { showMoreOptions(true) },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(36.dp).springPress(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

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
            enter = fadeIn(animationSpec = spring()),
            exit = fadeOut(animationSpec = spring())
        ) {
            val isDark = when (themeMode) {
                ThemeMode.DARK, ThemeMode.AMOLED -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                else -> false
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(8.dp)
                    .width(IntrinsicSize.Max)
                    .then(
                        if (isDark) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.extraLarge
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
