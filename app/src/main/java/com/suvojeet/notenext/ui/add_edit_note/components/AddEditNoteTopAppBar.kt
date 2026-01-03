package com.suvojeet.notenext.ui.add_edit_note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.ui.notes.NotesEvent
import com.suvojeet.notenext.ui.notes.NotesState
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CodeOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

/**
 * Top app bar for the Add/Edit Note screen. Provides navigation back,
 * and actions like pinning, archiving, and deleting the note.
 * The app bar's color adapts to the note's editing color.
 *
 * @param state The current [NotesState] containing information about the note being edited.
 * @param onEvent Lambda to dispatch [NotesEvent]s for various actions.
 * @param onDismiss Lambda to be invoked when the back button is clicked, typically to dismiss the screen.
 * @param showDeleteDialog Lambda to show/hide the delete confirmation dialog.
 * @param editingNoteType The type of the note being edited (e.g., "TEXT", "CHECKLIST").
 * @param onToggleFocusMode Lambda to toggle focus mode.
 * @param isFocusMode Boolean to indicate if focus mode is active.
 * @param onToggleMarkdownPreview Lambda to toggle markdown preview.
 * @param isMarkdownPreviewVisible Boolean to indicate if markdown preview is active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteTopAppBar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    editingNoteType: String,
    onToggleFocusMode: () -> Unit,
    isFocusMode: Boolean,
    onToggleMarkdownPreview: () -> Unit,
    isMarkdownPreviewVisible: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    TopAppBar(
        title = {
            // Dynamic title based on whether it's a new note and its type.
            Text(if (state.editingIsNewNote) {
                if (editingNoteType == "CHECKLIST") stringResource(id = R.string.add_checklist) else stringResource(id = R.string.add_note)
            } else "")
        },
        navigationIcon = {
            // Back button to dismiss the screen.
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back), tint = contentColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor, // Background color matches the note's color
            titleContentColor = contentColor, // Content color adapts to background
            actionIconContentColor = contentColor
        ),
        actions = {
            // Markdown Preview Toggle
            if (editingNoteType == "TEXT") {
                 IconButton(onClick = onToggleMarkdownPreview) {
                    Icon(
                        imageVector = if (isMarkdownPreviewVisible) Icons.Default.CodeOff else Icons.Default.Code,
                        contentDescription = "Toggle Markdown Preview",
                        tint = if (isMarkdownPreviewVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

            // Summarize Note
            IconButton(onClick = { onEvent(NotesEvent.SummarizeNote) }) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Summarize Note",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            }
            
            // Focus Mode Toggle
            IconButton(onClick = onToggleFocusMode) {
                Icon(
                    imageVector = if (isFocusMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Focus Mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions are only visible for existing notes, not for newly created ones.
            if (!state.editingIsNewNote) {
                // Pin/Unpin action.
                IconButton(onClick = { onEvent(NotesEvent.OnTogglePinClick) }) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = if (state.isPinned) stringResource(id = R.string.unpin_note) else stringResource(id = R.string.pin_note),
                        tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface // Tint changes based on pinned state.
                    )
                }
                // Archive/Unarchive action.
                IconButton(onClick = { onEvent(NotesEvent.OnToggleArchiveClick) }) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = if (state.isArchived) stringResource(id = R.string.unarchive_note) else stringResource(id = R.string.archive_note),
                        tint = if (state.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface // Tint changes based on archived state.
                    )
                }
                // Delete action.
                IconButton(onClick = { showDeleteDialog(true) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.delete_note),
                        tint = MaterialTheme.colorScheme.onSurface // Tint adapts to background color.
                    )
                }
            }
        }
    )
}