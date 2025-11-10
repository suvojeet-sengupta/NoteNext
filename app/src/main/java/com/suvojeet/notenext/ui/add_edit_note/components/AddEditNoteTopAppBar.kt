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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteTopAppBar(
    state: NotesState,
    onEvent: (NotesEvent) -> Unit,
    onDismiss: () -> Unit,
    showDeleteDialog: (Boolean) -> Unit,
    editingNoteType: String
) {
    TopAppBar(
        title = { Text(if (state.editingIsNewNote) {
            if (editingNoteType == "CHECKLIST") stringResource(id = R.string.add_checklist) else stringResource(id = R.string.add_note)
        } else "") },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(state.editingColor),
            titleContentColor = contentColorFor(backgroundColor = Color(state.editingColor)),
        ),
        actions = {
            if (!state.editingIsNewNote) {
                IconButton(onClick = { onEvent(NotesEvent.OnTogglePinClick) }) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = if (state.isPinned) stringResource(id = R.string.unpin_note) else stringResource(id = R.string.pin_note),
                        tint = if (state.isPinned) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))
                    )
                }
                IconButton(onClick = { onEvent(NotesEvent.OnToggleArchiveClick) }) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = if (state.isArchived) stringResource(id = R.string.unarchive_note) else stringResource(id = R.string.archive_note),
                        tint = if (state.isArchived) MaterialTheme.colorScheme.primary else contentColorFor(backgroundColor = Color(state.editingColor))
                    )
                }
                IconButton(onClick = { showDeleteDialog(true) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.delete_note),
                        tint = contentColorFor(backgroundColor = Color(state.editingColor))
                    )
                }
            }
        }
    )
}