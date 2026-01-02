package com.suvojeet.notenext.ui.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.NoteItem

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { padding ->
        if (state.notes.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Archive,
                message = stringResource(id = R.string.no_archived_notes),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(state.notes) { noteWithAttachments ->
                    NoteItem(
                        note = noteWithAttachments,
                        isSelected = false,
                        onNoteClick = {
                            noteToRestore = noteWithAttachments.note
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