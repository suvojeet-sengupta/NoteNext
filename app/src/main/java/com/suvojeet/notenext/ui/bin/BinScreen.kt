package com.suvojeet.notenext.ui.bin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeet.notenext.R
import com.suvojeet.notenext.ui.components.NoteItem

import androidx.compose.material.icons.filled.Menu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BinScreen(
    viewModel: BinViewModel,
    onMenuClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isSelectionModeActive = state.selectedNoteIds.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isSelectionModeActive) {
                    BinContextualTopAppBar(
                        selectedItemCount = state.selectedNoteIds.size,
                        onClearSelection = { viewModel.onEvent(BinEvent.ClearSelection) },
                        onRestoreClick = { viewModel.onEvent(BinEvent.RestoreSelectedNotes) },
                        onDeletePermanentlyClick = { viewModel.onEvent(BinEvent.DeleteSelectedNotesPermanently) }
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.bin_title)) },
                        navigationIcon = {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (state.notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.bin_empty_message),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(
                            items = state.notes,
                            key = { note -> note.id }
                        ) { note ->
                            NoteItem(
                                note = NoteWithAttachments(note, emptyList()),
                                onNoteClick = {
                                    if (isSelectionModeActive) {
                                        viewModel.onEvent(BinEvent.ToggleNoteSelection(note.id))
                                    } else {
                                        viewModel.onEvent(BinEvent.ExpandNote(note.id))
                                    }
                                },
                                onNoteLongClick = { viewModel.onEvent(BinEvent.ToggleNoteSelection(note.id)) },
                                isSelected = state.selectedNoteIds.contains(note.id)
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = state.expandedNoteId != null,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            BinnedNoteScreen(
                state = state,
                onDismiss = { viewModel.onEvent(BinEvent.CollapseNote) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BinContextualTopAppBar(
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onRestoreClick: () -> Unit,
    onDeletePermanentlyClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = "$selectedItemCount selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onRestoreClick) {
                Icon(Icons.Default.Restore, contentDescription = "Restore")
            }
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete permanently") },
                        onClick = {
                            onDeletePermanentlyClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    )
}
