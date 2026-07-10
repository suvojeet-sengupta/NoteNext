@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.suvojeet.notenext.ui.archive

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.suvojeet.notenext.R
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.ui.components.EmptyState
import com.suvojeet.notenext.ui.components.NoteItem
import com.suvojeet.notenext.ui.components.ExpressiveSection
import com.suvojeet.notenext.ui.components.springPress

@Composable
fun ArchiveScreen(
    onMenuClick: () -> Unit
) {
    val viewModel: ArchiveViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var noteToRestore by remember { mutableStateOf<com.suvojeet.notenext.data.NoteSummary?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.archive),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.springPress()) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.notes.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Archive,
                    message = stringResource(id = R.string.no_archived_notes)
                )
            } else {
                ExpressiveSection(
                    title = "Archived Notes",
                    description = "Notes you've put away for safekeeping"
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(
                            items = state.notes,
                            key = { it.note.id },
                            contentType = { it.note.noteType }
                        ) { noteWithAttachments ->
                            NoteItem(
                                modifier = Modifier.animateItem(),
                                note = noteWithAttachments,
                                isSelected = false,
                                onNoteClick = {
                                    noteToRestore = noteWithAttachments.note
                                    showRestoreDialog = true
                                },
                                onNoteLongClick = { /* Handle long click if needed */ }
                            )
                        }
                    }
                }
            }
        }

        if (showRestoreDialog && noteToRestore != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreDialog = false
                    noteToRestore = null
                },
                shape = MaterialTheme.shapes.extraLarge,
                title = { Text(stringResource(id = R.string.restore_note_title)) },
                text = { Text(stringResource(id = R.string.restore_note_confirmation)) },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.springPress(),
                        onClick = {
                            noteToRestore?.let {
                                viewModel.onEvent(ArchiveEvent.UnarchiveNote(it))
                            }
                            showRestoreDialog = false
                            noteToRestore = null
                        }
                    ) {
                        Text(stringResource(id = R.string.restore), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        modifier = Modifier.springPress(),
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
