package com.suvojeet.notenext.ui.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.ui.archive.ArchiveEvent
import androidx.compose.ui.res.stringResource
import com.suvojeet.notenext.R

import androidx.compose.material.icons.filled.Menu

import com.suvojeet.notenext.data.NoteWithAttachments

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onMenuClick: () -> Unit
) {
    val viewModel: ArchiveViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var noteToRestore by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                }
            )
        }
    ) { padding ->
        if (state.notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.no_archived_notes),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.notes) { note ->
                    NoteItem(
                        note = NoteWithAttachments(note, emptyList(), emptyList()),
                        isSelected = false, // Not selectable in archive
                        onNoteClick = {
                            noteToRestore = note
                            showRestoreDialog = true
                        },
                        onNoteLongClick = { /* TODO: Handle long click if needed */ }
                    )
                }
            }
        }

        if (showRestoreDialog && noteToRestore != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreDialog = false
                    noteToRestore = null
                },
                title = { Text(stringResource(id = R.string.restore_note_title)) },
                text = { Text(stringResource(id = R.string.restore_note_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            noteToRestore?.let {
                                viewModel.onEvent(ArchiveEvent.UnarchiveNote(it))
                            }
                            showRestoreDialog = false
                            noteToRestore = null
                        }
                    ) {
                        Text(stringResource(id = R.string.restore))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRestoreDialog = false
                            noteToRestore = null
                        }
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }
    }
}