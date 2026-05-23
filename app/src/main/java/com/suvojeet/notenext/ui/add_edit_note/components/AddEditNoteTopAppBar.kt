@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.springPress
import com.suvojeet.notenext.core.model.NoteType
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesEditState
import com.suvojeet.notenext.ui.notes.SaveStatus

private val OliveSaved = Color(0xFF6B7F4E)

@Composable
fun AddEditNoteTopAppBar(
    state: NotesEditState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    onToneRewriteClick: () -> Unit,
    editingNoteType: NoteType,
    onToggleFocusMode: () -> Unit,
    isFocusMode: Boolean,
    onShowColorPicker: () -> Unit = {},
    onShowReminder: () -> Unit = {},
    onShowMoreOptions: () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var showOverflow by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (state.editingIsNewNote) {
                Text(
                    text = if (editingNoteType == NoteType.CHECKLIST) stringResource(id = R.string.add_checklist) else stringResource(id = R.string.add_note),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // "Saved" vital sign, mockup-style, with an olive dot.
                val saveText = when (state.saveStatus) {
                    SaveStatus.SAVING -> "Saving…"
                    SaveStatus.SAVED -> "Saved"
                    SaveStatus.UNSAVED -> "Unsaved"
                    SaveStatus.ERROR -> "Not saved"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                when (state.saveStatus) {
                                    SaveStatus.SAVED -> OliveSaved
                                    SaveStatus.ERROR -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                CircleShape
                            )
                    )
                    Text(
                        text = saveText,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        },
        navigationIcon = {
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .springPress(),
                shape = MaterialTheme.shapes.medium,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = contentColor
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        scrollBehavior = scrollBehavior,
        actions = {
            // Focus mode stays a direct toggle (frequent, mockup keeps writing distraction-free).
            IconButton(onClick = onToggleFocusMode, modifier = Modifier.springPress()) {
                Icon(
                    imageVector = if (isFocusMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Focus Mode",
                    tint = contentColor
                )
            }

            Box {
                FilledTonalIconButton(
                    onClick = { showOverflow = true },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .springPress(),
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = contentColor
                    )
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                }

                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false },
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.toggle_color_picker)) },
                        onClick = { showOverflow = false; onShowColorPicker() },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Reminder") },
                        onClick = { showOverflow = false; onShowReminder() },
                        leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                    )
                    if (!state.editingIsNewNote) {
                        DropdownMenuItem(
                            text = { Text(if (state.isPinned) stringResource(id = R.string.unpin_note) else stringResource(id = R.string.pin_note)) },
                            onClick = { showOverflow = false; onEvent(NotesEvent.OnTogglePinClick) },
                            leadingIcon = { Icon(if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (state.isArchived) stringResource(id = R.string.unarchive_note) else stringResource(id = R.string.archive_note)) },
                            onClick = { showOverflow = false; onEvent(NotesEvent.OnToggleArchiveClick) },
                            leadingIcon = { Icon(if (state.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive, contentDescription = null) }
                        )
                    }
                    if (editingNoteType == NoteType.TEXT && !state.editingIsNewNote) {
                        DropdownMenuItem(
                            text = { Text("Summarize") },
                            onClick = { showOverflow = false; onEvent(NotesEvent.SummarizeNote) },
                            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rewrite tone") },
                            onClick = { showOverflow = false; onToneRewriteClick() },
                            leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.more_options)) },
                        onClick = { showOverflow = false; onShowMoreOptions() },
                        leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    )
                }
            }
        }
    )
}
